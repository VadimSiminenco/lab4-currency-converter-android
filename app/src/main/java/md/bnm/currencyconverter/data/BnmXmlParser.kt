package md.bnm.currencyconverter.data

import md.bnm.currencyconverter.domain.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

class InvalidFeed(message: String) : Exception(message)
class BnmXmlParser {
    fun parse(xml: String, fetchedAt: Instant): RateSnapshot? {
        if (xml.isBlank() || xml.length > 2_000_000 || xml.contains("<!DOCTYPE", ignoreCase = true))
            throw InvalidFeed("Empty or unsupported XML response")
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))
            if (parser.nextTag() != XmlPullParser.START_TAG || parser.name != "ValCurs")
                throw InvalidFeed("Expected ValCurs XML response")
            val rateDate = LocalDate.parse(parser.getAttributeValue(null, "Date"), DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT))
            val source = parser.getAttributeValue(null, "name")?.takeIf { it.isNotBlank() } ?: BNM_SOURCE
            val rates = mutableMapOf(Currency.MDL to BigDecimal.ONE)
            val seenCodes = mutableSetOf<String>()
            var entries = 0
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Valute") {
                    entries++
                    val fields = mutableMapOf<String, String>()
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.END_TAG && parser.name == "Valute") break
                        if (parser.eventType == XmlPullParser.START_TAG) {
                            val field = parser.name
                            if (fields.put(field, parser.nextText().trim()) != null) throw InvalidFeed("Duplicate XML field")
                        }
                    }
                    val code = fields["CharCode"]?.uppercase()?.takeIf { it.matches(Regex("[A-Z]{3}")) }
                        ?: throw InvalidFeed("Missing currency code")
                    if (!seenCodes.add(code)) throw InvalidFeed("Duplicate currency code")
                    fun positive(field: String): BigDecimal {
                        val raw = fields[field]?.replace(',', '.') ?: throw InvalidFeed("Missing $field")
                        if (raw.length > 64) throw InvalidFeed("Oversized $field")
                        if (!raw.matches(Regex("[0-9]+(\\.[0-9]+)?"))) throw InvalidFeed("Invalid $field")
                        val value = raw.toBigDecimalOrNull() ?: throw InvalidFeed("Invalid $field")
                        if (value.signum() <= 0) throw InvalidFeed("$field must be positive")
                        return value
                    }
                    val nominal = positive("Nominal")
                    val value = positive("Value")
                    val currency = Currency.entries.firstOrNull { it.name == code }
                    if (currency != null) {
                        if (currency == Currency.MDL && value.compareTo(nominal) != 0) throw InvalidFeed("Invalid MDL base rate")
                        rates[currency] = value.divide(nominal, CALCULATION_CONTEXT)
                    }
                }
            }
            return if (entries == 0) null else RateSnapshot(rateDate, source, rates.toMap(), fetchedAt)
        } catch (exc: InvalidFeed) { throw exc }
        catch (exc: Exception) { throw InvalidFeed("Invalid exchange-rate XML: ${exc.javaClass.simpleName}") }
    }
}
