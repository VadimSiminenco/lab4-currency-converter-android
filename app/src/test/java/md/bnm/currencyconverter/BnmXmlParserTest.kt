package md.bnm.currencyconverter

import md.bnm.currencyconverter.data.BnmXmlParser
import md.bnm.currencyconverter.data.InvalidFeed
import md.bnm.currencyconverter.domain.Currency
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BnmXmlParserTest {
    private val parser = BnmXmlParser()
    private fun feed(entries: String, date: String = "04.11.2026") = "<ValCurs Date=\"$date\" name=\"Synthetic fixture\">$entries</ValCurs>"
    private fun entry(code: String = "USD", nominal: String = "1", value: String = "18") = "<Valute><CharCode>$code</CharCode><Nominal>$nominal</Nominal><Value>$value</Value></Valute>"
    @Test fun parsesRealShapeAndGenericNominal() {
        val data = parser.parse(feed(entry() + entry("UAH", "10", "4.5")), testInstant)!!
        assertEquals(testDate, data.date); assertEquals(testInstant, data.fetchedAt)
        assertEquals(BigDecimal.ONE, data.ratesMdlPerUnit[Currency.MDL])
        assertEquals(BigDecimal("18"), data.ratesMdlPerUnit[Currency.USD])
        assertEquals(BigDecimal("0.45"), data.ratesMdlPerUnit[Currency.UAH])
    }
    @Test fun emptyPublishedDayHasNoSnapshot() { assertNull(parser.parse(feed(""), testInstant)) }
    @Test fun acceptsDecimalComma() { assertEquals(BigDecimal("18.5"), parser.parse(feed(entry(value = "18,5")), testInstant)!!.ratesMdlPerUnit[Currency.USD]) }
    @Test fun rejectsMalformedErrorAndAttributeBasedFixtures() {
        listOf("", "<ValCurs>", "<Errors><Error>none</Error></Errors>", feed(entry(), "31.02.2026"),
            feed("<Valute CharCode=\"USD\" Nominal=\"1\"><Value>18</Value></Valute>"),
            feed(entry()+entry()), "<!DOCTYPE foo><ValCurs Date=\"04.11.2026\"/>").forEach {
            assertThrows(it, InvalidFeed::class.java) { parser.parse(it, testInstant) }
        }
    }
    @Test fun rejectsInvalidNumbersBeforeCache() {
        listOf("0", "-1", "NaN", "Infinity", "abc").forEach { bad ->
            assertThrows(InvalidFeed::class.java) { parser.parse(feed(entry(nominal = bad)), testInstant) }
            assertThrows(InvalidFeed::class.java) { parser.parse(feed(entry(value = bad)), testInstant) }
        }
    }
    @Test fun doesNotHardcodeWeekendAsMissing() { assertNotNull(parser.parse(feed(entry(), "01.11.2026"), testInstant)) }
}
