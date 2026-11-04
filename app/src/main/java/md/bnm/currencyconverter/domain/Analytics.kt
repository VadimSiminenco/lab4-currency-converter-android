package md.bnm.currencyconverter.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class AnalyticsPeriod(val days: Int, val end: LocalDate) {
    init { require(days == 7 || days == 30) }
    val start: LocalDate get() = end.minusDays(days - 1L)
}
data class PairObservation(val date: LocalDate, val rate: BigDecimal)
enum class Trend(val label: String) { RISING("↑ Rising"), FALLING("↓ Falling"), FLAT("→ No change") }
data class RateStatistics(
    val min: BigDecimal, val max: BigDecimal, val mean: BigDecimal,
    val delta: BigDecimal? = null, val percent: BigDecimal? = null, val trend: Trend? = null,
)
sealed interface AnalyticsResult {
    val observations: List<PairObservation>
    val statistics: RateStatistics?
    data object InsufficientData : AnalyticsResult {
        override val observations = emptyList<PairObservation>()
        override val statistics: RateStatistics? = null
    }
    data class SinglePoint(val point: PairObservation) : AnalyticsResult {
        override val observations = listOf(point)
        override val statistics = RateStatistics(point.rate, point.rate, point.rate)
    }
    data class Full(override val observations: List<PairObservation>, override val statistics: RateStatistics) : AnalyticsResult
}
class StatisticsCalculator {
    fun calculate(observations: List<PairObservation>): AnalyticsResult {
        if (observations.isEmpty()) return AnalyticsResult.InsufficientData
        require(observations.all { it.rate.signum() >= 0 }) { "Negative rate is invalid" }
        require(observations.map { it.date }.distinct().size == observations.size) { "Duplicate effective dates must be resolved first" }
        val sorted = observations.sortedBy { it.date }
        if (sorted.size == 1) return AnalyticsResult.SinglePoint(sorted.single())
        val values = sorted.map { it.rate }
        val delta = values.last().subtract(values.first())
        val percent = if (values.first().signum() == 0) null else delta.divide(values.first(), CALCULATION_CONTEXT).multiply(BigDecimal("100"))
        val trend = when (delta.signum()) { 1 -> Trend.RISING; -1 -> Trend.FALLING; else -> Trend.FLAT }
        return AnalyticsResult.Full(sorted, RateStatistics(values.min(), values.max(),
            values.fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(values.size), CALCULATION_CONTEXT), delta, percent, trend))
    }
}
data class AnalyticsData(
    val period: AnalyticsPeriod,
    val result: AnalyticsResult,
    val fetchedAt: Instant?,
    val missingPairDates: Int,
    val unresolvedRequests: Int,
)
class GetAnalytics(private val calculator: StatisticsCalculator = StatisticsCalculator()) {
    operator fun invoke(snapshots: List<RateSnapshot>, records: List<DateFetch>, source: Currency, target: Currency, period: AnalyticsPeriod): AnalyticsData {
        val current = snapshots.filter { it.date >= period.start && it.date <= period.end }
            .groupBy { it.date }.values.map { sameDate -> sameDate.maxBy { it.fetchedAt } }.sortedBy { it.date }
        val observations = current.mapNotNull { snapshot ->
            val from = snapshot.ratesMdlPerUnit[source]
            val to = snapshot.ratesMdlPerUnit[target]
            if (from == null || to == null || from.signum() <= 0 || to.signum() <= 0) null
            else PairObservation(snapshot.date, from.divide(to, CALCULATION_CONTEXT))
        }
        val resolvedDates = records.filter { it.date >= period.start && it.date <= period.end && it.kind != FetchKind.FAILED }.map { it.date }.distinct()
        return AnalyticsData(period, calculator.calculate(observations), current.maxOfOrNull { it.fetchedAt },
            current.size - observations.size, period.days - resolvedDates.size)
    }
}
