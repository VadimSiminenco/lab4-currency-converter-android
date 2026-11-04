package md.bnm.currencyconverter.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

val CALCULATION_CONTEXT: MathContext = MathContext.DECIMAL128
const val BNM_SOURCE = "National Bank of Moldova"
enum class Currency(val displayName: String) {
    MDL("Moldovan Leu"), EUR("Euro"), USD("US Dollar"), RON("Romanian Leu"), GBP("British Pound"), UAH("Ukrainian Hryvnia")
}
data class RateSnapshot(
    val date: LocalDate,
    val sourceName: String,
    val ratesMdlPerUnit: Map<Currency, BigDecimal>,
    val fetchedAt: Instant,
) {
    init {
        require(ratesMdlPerUnit.isNotEmpty()) { "Snapshot must contain rates" }
        require(ratesMdlPerUnit.values.all { it.signum() > 0 }) { "Rates must be positive" }
        require(ratesMdlPerUnit[Currency.MDL]?.compareTo(BigDecimal.ONE) == 0) { "MDL base rate must be 1" }
    }
}
sealed interface RemoteOutcome {
    data class Published(val snapshot: RateSnapshot) : RemoteOutcome
    data object Unpublished : RemoteOutcome
    data class Failure(val message: String, val retryable: Boolean = false) : RemoteOutcome
}
enum class FetchKind { PUBLISHED, UNPUBLISHED, FAILED }
data class DateFetch(val date: LocalDate, val effectiveDate: LocalDate?, val kind: FetchKind, val checkedAt: Instant)
data class ResolvedRate(val snapshot: RateSnapshot, val fromCache: Boolean, val usedPreviousDate: Boolean)
data class HistoryProgress(val completed: Int, val total: Int, val failures: Int)
data class HistoryRefresh(val completed: Int, val failures: Int)
class RatesUnavailable(message: String) : Exception(message)
class CurrencyUnavailable(message: String) : Exception(message)

data class AmountValidation(val value: BigDecimal? = null, val error: String? = null)
object AmountValidator {
    private val pattern = Regex("[0-9]+([.,][0-9]+)?")
    fun validate(raw: String): AmountValidation {
        val text = raw.trim()
        if (text.isEmpty()) return AmountValidation(error = "Enter an amount.")
        if (text.length > 64) return AmountValidation(error = "Use at most 64 characters.")
        if (text.startsWith("-")) return AmountValidation(error = "Amount must be greater than zero.")
        if (!pattern.matches(text)) return AmountValidation(error = "Enter a decimal number, such as 100 or 25.50.")
        val number = text.replace(',', '.').toBigDecimalOrNull()
            ?: return AmountValidation(error = "Enter a valid amount.")
        if (number.signum() <= 0) return AmountValidation(error = "Amount must be greater than zero.")
        if (number > BigDecimal("1000000000")) return AmountValidation(error = "Maximum amount is 1,000,000,000.")
        return AmountValidation(value = number)
    }
}
data class ConversionResult(
    val amount: BigDecimal,
    val source: Currency,
    val target: Currency,
    val converted: BigDecimal,
    val rate: BigDecimal,
    val resolved: ResolvedRate?,
)
class ConvertCurrency(private val repository: RateRepository) {
    suspend operator fun invoke(amount: BigDecimal, source: Currency, target: Currency, date: LocalDate): ConversionResult {
        require(amount.signum() > 0 && amount <= BigDecimal("1000000000")) { "Amount out of range" }
        if (source == target) return ConversionResult(amount, source, target, amount, BigDecimal.ONE, null)
        val resolved = repository.resolve(date)
        val from = resolved.snapshot.ratesMdlPerUnit[source]
            ?: throw CurrencyUnavailable("$source is unavailable on ${resolved.snapshot.date}.")
        val to = resolved.snapshot.ratesMdlPerUnit[target]
            ?: throw CurrencyUnavailable("$target is unavailable on ${resolved.snapshot.date}.")
        val converted = amount.multiply(from).divide(to, CALCULATION_CONTEXT).setScale(2, RoundingMode.HALF_UP)
        return ConversionResult(amount, source, target, converted, from.divide(to, CALCULATION_CONTEXT), resolved)
    }
}
