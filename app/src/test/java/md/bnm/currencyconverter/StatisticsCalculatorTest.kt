package md.bnm.currencyconverter

import md.bnm.currencyconverter.domain.*
import md.bnm.currencyconverter.ui.changeText
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class StatisticsCalculatorTest {
    private val calculator = StatisticsCalculator()
    private fun points(vararg values: String) = values.mapIndexed { index, value -> PairObservation(testDate.minusDays(values.size-1L-index), BigDecimal(value)) }
    private fun number(expected: String, actual: BigDecimal?) { assertNotNull(actual); assertEquals(0, BigDecimal(expected).compareTo(actual)) }
    @Test fun allMetricsUseDateOrderAndKnownValues() {
        val result = calculator.calculate(points("10", "30", "20").reversed()) as AnalyticsResult.Full
        val s=result.statistics
        number("10",s.min); number("30",s.max); number("20",s.mean)
        number("10",s.delta); number("100",s.percent); assertEquals(Trend.RISING,s.trend)
        assertEquals(testDate,result.observations.last().date)
    }
    @Test fun decreasingSeriesHasNegativeChangeAndPercent() {
        val s=calculator.calculate(points("20","10")).statistics!!
        number("-10",s.delta); number("-50",s.percent); assertEquals(Trend.FALLING,s.trend)
    }
    @Test fun constantSeriesIsGenuineNoChange() {
        val s=calculator.calculate(points("18","18","18")).statistics!!
        number("18",s.mean); number("0",s.delta); number("0",s.percent); assertEquals(Trend.FLAT,s.trend)
    }
    @Test fun emptyDoesNotFabricateStatistics() {
        assertEquals(AnalyticsResult.InsufficientData,calculator.calculate(emptyList()))
        assertNull(calculator.calculate(emptyList()).statistics)
    }
    @Test fun singlePointOnlyHasDescriptiveMetrics() {
        val result=calculator.calculate(points("18"))
        assertTrue(result is AnalyticsResult.SinglePoint)
        val s=result.statistics!!; number("18",s.min); number("18",s.max); number("18",s.mean)
        assertNull(s.delta); assertNull(s.percent); assertNull(s.trend)
    }
    @Test fun zeroDenominatorReturnsUnavailablePercent() {
        val s=calculator.calculate(points("0","2")).statistics!!
        number("2",s.delta); assertNull(s.percent); assertEquals(Trend.RISING,s.trend)
    }
    @Test fun nonTerminatingMeanUsesDecimal128() {
        number("1.333333333333333333333333333333333",calculator.calculate(points("1","1","2")).statistics!!.mean)
    }
    @Test fun tinyUnroundedChangeStillHasDirection() {
        assertEquals(Trend.RISING,calculator.calculate(points("1","1.00000000000000000001")).statistics!!.trend)
        assertEquals(Trend.FALLING,calculator.calculate(points("1.00000000000000000001","1")).statistics!!.trend)
    }
    @Test fun duplicateDatesMustBeResolvedBeforeStatistics() {
        assertThrows(IllegalArgumentException::class.java) { calculator.calculate(listOf(PairObservation(testDate,BigDecimal.ONE),PairObservation(testDate,BigDecimal.TEN))) }
    }
    @Test fun negativeDataIsRejected() { assertThrows(IllegalArgumentException::class.java) { calculator.calculate(points("-1","2")) } }
    @Test fun displayDoesNotDescribeTinyChangeAsZero() {
        assertEquals("+<0.000001",changeText(BigDecimal("0.00000001")))
        assertEquals("−<0.0001",changeText(BigDecimal("-0.00000001"),4))
        assertEquals("Unavailable",changeText(null))
        assertEquals("0.000000",changeText(BigDecimal.ZERO))
    }
}
