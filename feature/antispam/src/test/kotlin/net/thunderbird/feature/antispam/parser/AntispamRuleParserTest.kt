package net.thunderbird.feature.antispam.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.feature.antispam.data.RuleType

class AntispamRuleParserTest {
    @Test
    fun `parses all rule kinds and ignores comments`() {
        val input = """
            [Antispam List 1.0]
            ! commento
            ||Promo.Example.com^
            from:news@shop.it
            subject:Offerta Imperdibile
            body:sconto esclusivo
            @@||fidato.com^
            ab
            ||nondominio^
        """.trimIndent().lineSequence()

        val result = AntispamRuleParser.parse(input)

        assertEquals(
            listOf(
                ParsedRule(RuleType.SENDER_DOMAIN, "promo.example.com", false),
                ParsedRule(RuleType.SENDER_ADDRESS, "news@shop.it", false),
                ParsedRule(RuleType.SUBJECT_KEYWORD, "offerta imperdibile", false),
                ParsedRule(RuleType.BODY_KEYWORD, "sconto esclusivo", false),
                ParsedRule(RuleType.SENDER_DOMAIN, "fidato.com", true),
            ),
            result.rules,
        )
        assertEquals(2, result.skippedLines)
    }
}
