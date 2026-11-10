package net.thunderbird.feature.antispam.parser

import net.thunderbird.feature.antispam.data.RuleType

data class ParsedRule(val type: RuleType, val value: String, val isException: Boolean)

data class ParseResult(val rules: List<ParsedRule>, val skippedLines: Int)

/**
 * Sintassi supportata (una regola per riga, stile EasyList/AdBlock):
 *
 *   ! commento            # commento           [Header]
 *   ||esempio.com^        dominio mittente (include i sottodomini)
 *   from:promo@esempio.it mittente esatto (o from:dominio.it)
 *   subject:offerta       parola chiave nell'oggetto
 *   body:sconto           parola chiave nel corpo
 *   @@||fidato.com^       eccezione (allowlist), vale anche con from:/subject:/body:
 *   promo@esempio.it      indirizzo nudo -> mittente
 *   esempio.com           dominio nudo   -> dominio mittente
 *   offerta imperdibile   altro testo    -> keyword nell'oggetto
 */
object AntispamRuleParser {

    private const val MIN_KEYWORD_LENGTH = 3
    private val DOMAIN_REGEX =
        Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$")

    fun parse(lines: Sequence<String>, maxRules: Int = 200_000): ParseResult {
        val rules = LinkedHashSet<ParsedRule>()
        var skipped = 0

        for (raw in lines) {
            val line = raw.removePrefix("\uFEFF").trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("#") || line.startsWith("[")) continue

            val isException = line.startsWith("@@")
            val body = if (isException) line.substring(2).trim() else line

            val rule = parseBody(body, isException)
            if (rule == null) skipped++ else rules.add(rule)
            if (rules.size >= maxRules) break
        }
        return ParseResult(rules.toList(), skipped)
    }

    private fun parseBody(body: String, isException: Boolean): ParsedRule? {
        val lower = body.lowercase()
        return when {
            lower.startsWith("||") -> {
                val domain = lower.substring(2).substringBefore('^').substringBefore('/')
                domain.takeIf { DOMAIN_REGEX.matches(it) }
                    ?.let { ParsedRule(RuleType.SENDER_DOMAIN, it, isException) }
            }
            lower.startsWith("from:") -> sender(lower.removePrefix("from:").trim(), isException)
            lower.startsWith("subject:") -> keyword(RuleType.SUBJECT_KEYWORD, lower.removePrefix("subject:"), isException)
            lower.startsWith("body:") -> keyword(RuleType.BODY_KEYWORD, lower.removePrefix("body:"), isException)
            else -> sender(lower, isException) ?: keyword(RuleType.SUBJECT_KEYWORD, lower, isException)
        }
    }

    private fun sender(value: String, isException: Boolean): ParsedRule? = when {
        ' ' in value -> null
        '@' in value && value.indexOf('@') > 0 && value.indexOf('@') == value.lastIndexOf('@') ->
            ParsedRule(RuleType.SENDER_ADDRESS, value, isException)
        DOMAIN_REGEX.matches(value) -> ParsedRule(RuleType.SENDER_DOMAIN, value, isException)
        else -> null
    }

    private fun keyword(type: RuleType, value: String, isException: Boolean): ParsedRule? =
        value.trim().takeIf { it.length >= MIN_KEYWORD_LENGTH }?.let { ParsedRule(type, it, isException) }
}
