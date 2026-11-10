package net.thunderbird.feature.antispam

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking
import net.thunderbird.feature.antispam.data.AntispamDao
import net.thunderbird.feature.antispam.data.RuleType

class AntispamFilterManager(private val dao: AntispamDao) {

    private class KeywordCache(
        val subject: List<String>,
        val body: List<String>,
        val subjectExceptions: List<String>,
        val bodyExceptions: List<String>,
    )

    // Le keyword (poche migliaia) stanno in memoria; mittenti e domini si cercano via indice Room.
    private val keywordCache = ConcurrentHashMap<String, KeywordCache>()

    /** Da chiamare quando cambiano liste, regole o collegamenti account <-> lista. */
    fun invalidate() = keywordCache.clear()

    /**
     * Versione sincrona per la callback di sync (già eseguita fuori dal thread principale).
     * In caso di errore NON blocca la posta: restituisce false.
     */
    fun isSpam(accountUuid: String, subject: String, fromAddress: String, bodySnippet: String): Boolean =
        try {
            runBlocking { isSpamSuspend(accountUuid, subject, fromAddress, bodySnippet) }
        } catch (e: Exception) {
            false
        }

    suspend fun isSpamSuspend(accountUuid: String, subject: String, fromAddress: String, bodySnippet: String): Boolean {
        val sender = normalizeAddress(fromAddress)
        val domains = domainChain(sender)
        val subjectLower = subject.lowercase()
        val bodyLower = bodySnippet.lowercase()
        val keywords = keywords(accountUuid)

        val addressHits = if (sender.isNotEmpty()) {
            dao.findExact(accountUuid, RuleType.SENDER_ADDRESS, listOf(sender))
        } else {
            emptyList()
        }
        val domainHits = if (domains.isNotEmpty()) {
            dao.findExact(accountUuid, RuleType.SENDER_DOMAIN, domains)
        } else {
            emptyList()
        }

        // Le eccezioni (@@) hanno sempre la precedenza.
        val isAllowed = addressHits.any { it.isException } ||
            domainHits.any { it.isException } ||
            keywords.subjectExceptions.any { subjectLower.contains(it) } ||
            keywords.bodyExceptions.any { bodyLower.contains(it) }
        if (isAllowed) return false

        return addressHits.isNotEmpty() ||
            domainHits.isNotEmpty() ||
            keywords.subject.any { subjectLower.contains(it) } ||
            keywords.body.any { bodyLower.contains(it) }
    }

    private suspend fun keywords(accountUuid: String): KeywordCache {
        keywordCache[accountUuid]?.let { return it }
        val subject = dao.getKeywordRules(accountUuid, RuleType.SUBJECT_KEYWORD)
        val body = dao.getKeywordRules(accountUuid, RuleType.BODY_KEYWORD)
        return KeywordCache(
            subject = subject.filterNot { it.isException }.map { it.value },
            body = body.filterNot { it.isException }.map { it.value },
            subjectExceptions = subject.filter { it.isException }.map { it.value },
            bodyExceptions = body.filter { it.isException }.map { it.value },
        ).also { keywordCache[accountUuid] = it }
    }

    /** "Nome <a@b.it>" -> "a@b.it" */
    internal fun normalizeAddress(from: String): String =
        from.substringAfterLast('<').substringBefore('>').trim().lowercase()

    /** "a@x.y.example.com" -> [x.y.example.com, y.example.com, example.com] (mai il solo TLD) */
    internal fun domainChain(address: String): List<String> {
        val domain = address.substringAfter('@', "")
        if (domain.isEmpty()) return emptyList()
        val labels = domain.split('.')
        return (0 until labels.size - 1).map { labels.drop(it).joinToString(".") }
    }
}
