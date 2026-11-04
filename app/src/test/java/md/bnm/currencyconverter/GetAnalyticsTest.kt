package md.bnm.currencyconverter

import md.bnm.currencyconverter.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class GetAnalyticsTest {
    private val analyze=GetAnalytics()
    private fun records(days: Int)= (0 until days).map { DateFetch(testDate.minusDays(it.toLong()),testDate.minusDays(it.toLong()),FetchKind.PUBLISHED,testInstant) }
    @Test fun calendarWindowsAreInclusiveAndAnchoredToToday() {
        assertEquals(testDate.minusDays(6),AnalyticsPeriod(7,testDate).start)
        assertEquals(testDate.minusDays(29),AnalyticsPeriod(30,testDate).start)
        assertThrows(IllegalArgumentException::class.java) { AnalyticsPeriod(14,testDate) }
    }
    @Test fun outsideDatesAreExcludedRatherThanMovingTheWindow() {
        val result=analyze(listOf(snapshot(testDate.minusDays(30)),snapshot(testDate.plusDays(1))),emptyList(),Currency.USD,Currency.MDL,AnalyticsPeriod(7,testDate))
        assertEquals(AnalyticsResult.InsufficientData,result.result); assertEquals(testDate,result.period.end)
        assertNull(result.fetchedAt)
    }
    @Test fun sourceDividedByTargetHasCorrectUnits() {
        val result=analyze(listOf(snapshot()),records(7),Currency.USD,Currency.EUR,AnalyticsPeriod(7,testDate))
        assertEquals(0,BigDecimal("0.9").compareTo(result.result.observations.single().rate))
        val reverse=analyze(listOf(snapshot()),records(7),Currency.EUR,Currency.USD,AnalyticsPeriod(7,testDate))
        assertEquals(0,BigDecimal("1.111111111111111111111111111111111").compareTo(reverse.result.observations.single().rate))
    }
    @Test fun newestCorrectionWinsWithoutDoubleWeighting() {
        val old=snapshot(); val corrected=old.copy(ratesMdlPerUnit=old.ratesMdlPerUnit+(Currency.USD to BigDecimal("19")), fetchedAt=testInstant.plusSeconds(60))
        val result=analyze(listOf(corrected,old),records(7),Currency.USD,Currency.EUR,AnalyticsPeriod(7,testDate))
        assertEquals(1,result.result.observations.size)
        assertEquals(0,BigDecimal("0.95").compareTo(result.result.statistics!!.mean))
        assertEquals(corrected.fetchedAt,result.fetchedAt)
    }
    @Test fun pairsAreNotJoinedAcrossDifferentDates() {
        val usdOnly=snapshot().copy(ratesMdlPerUnit=mapOf(Currency.MDL to BigDecimal.ONE,Currency.USD to BigDecimal("18")))
        val eurOnly=snapshot(testDate.minusDays(1)).copy(ratesMdlPerUnit=mapOf(Currency.MDL to BigDecimal.ONE,Currency.EUR to BigDecimal("20")))
        val result=analyze(listOf(usdOnly,eurOnly),records(7),Currency.USD,Currency.EUR,AnalyticsPeriod(7,testDate))
        assertEquals(AnalyticsResult.InsufficientData,result.result); assertEquals(2,result.missingPairDates)
    }
    @Test fun gapsStayMissingButEqualRatesOnDifferentDatesStayPresent() {
        val result=analyze(listOf(snapshot(testDate.minusDays(6)),snapshot()),records(7),Currency.USD,Currency.MDL,AnalyticsPeriod(7,testDate))
        assertEquals(2,result.result.observations.size)
        assertEquals(Trend.FLAT,result.result.statistics!!.trend)
        assertEquals(testDate.minusDays(6),result.result.observations.first().date)
    }
    @Test fun completedNoPublicationIsNotReportedAsFailedRequest() {
        val rs=listOf(DateFetch(testDate,testDate,FetchKind.PUBLISHED,testInstant),DateFetch(testDate.minusDays(1),null,FetchKind.UNPUBLISHED,testInstant),DateFetch(testDate.minusDays(2),null,FetchKind.FAILED,testInstant))
        val result=analyze(listOf(snapshot()),rs,Currency.USD,Currency.MDL,AnalyticsPeriod(7,testDate))
        assertEquals(5,result.unresolvedRequests); assertEquals(1,result.result.observations.size)
    }
    @Test fun sameCurrencyHistoryUsesObservedDatesOnly() {
        val result=analyze(listOf(snapshot()),records(7),Currency.EUR,Currency.EUR,AnalyticsPeriod(7,testDate))
        assertEquals(1,result.result.observations.size)
        assertEquals(0,BigDecimal.ONE.compareTo(result.result.observations.single().rate))
        assertNull(result.result.statistics!!.trend)
    }
}
