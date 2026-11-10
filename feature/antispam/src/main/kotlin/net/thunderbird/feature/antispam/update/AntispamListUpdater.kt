package net.thunderbird.feature.antispam.update

import net.thunderbird.feature.antispam.data.AntispamDao
import net.thunderbird.feature.antispam.data.ListSourceEntity
import net.thunderbird.feature.antispam.data.RuleEntity
import net.thunderbird.feature.antispam.parser.AntispamRuleParser

sealed interface UpdateOutcome {
    data object Unchanged : UpdateOutcome
    data class Updated(val ruleCount: Int) : UpdateOutcome
    data class Error(val reason: String) : UpdateOutcome
}

class AntispamListUpdater(
    private val dao: AntispamDao,
    private val downloader: AntispamListDownloader,
    private val onRulesChanged: () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun updateList(list: ListSourceEntity, force: Boolean = false): UpdateOutcome {
        val result = downloader.fetch(
            url = list.url,
            etag = if (force) null else list.etag,
            lastModified = if (force) null else list.lastModified,
        )
        return when (result) {
            FetchResult.NotModified -> {
                dao.updateList(list.copy(lastUpdatedAt = now(), lastError = null))
                UpdateOutcome.Unchanged
            }
            is FetchResult.Failed -> {
                // Le regole esistenti restano al loro posto.
                dao.updateList(list.copy(lastError = result.reason))
                UpdateOutcome.Error(result.reason)
            }
            is FetchResult.Downloaded -> applyDownload(list, result)
        }
    }

    private suspend fun applyDownload(list: ListSourceEntity, download: FetchResult.Downloaded): UpdateOutcome {
        val parsed = AntispamRuleParser.parse(download.lines.asSequence())
        if (parsed.rules.isEmpty()) {
            // Protezione: una pagina d'errore o un file vuoto non deve cancellare le regole buone.
            val reason = "Nessuna regola valida trovata"
            dao.updateList(list.copy(lastError = reason))
            return UpdateOutcome.Error(reason)
        }
        val entities = parsed.rules.map { RuleEntity(listId = list.id, type = it.type, value = it.value, isException = it.isException) }
        dao.replaceRules(list.id, entities)
        dao.updateList(
            list.copy(
                etag = download.etag,
                lastModified = download.lastModified,
                lastUpdatedAt = now(),
                lastError = null,
                ruleCount = entities.size,
            ),
        )
        onRulesChanged()
        return UpdateOutcome.Updated(entities.size)
    }

    /** Usata dal worker ogni 7 giorni: aggiorna solo le liste attive su almeno un account. */
    suspend fun updateAllInUse(force: Boolean = false): Map<Long, UpdateOutcome> =
        dao.getListsInUse().associate { it.id to updateList(it, force) }
}
