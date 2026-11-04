package md.bnm.currencyconverter

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import md.bnm.currencyconverter.domain.*
import md.bnm.currencyconverter.ui.AnalyticsScreen
import md.bnm.currencyconverter.ui.AnalyticsUiState
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class AnalyticsScreenTest {
    @get:Rule val compose=createComposeRule()
    private val today=LocalDate.of(2026,11,4)
    private fun state(days: Int, count: Int=days): AnalyticsUiState {
        val period=AnalyticsPeriod(days,today)
        val snapshots=(0 until count).map { index -> RateSnapshot(period.start.plusDays(index.toLong()),"Synthetic Android fixture",
            Currency.entries.associateWith { when(it) { Currency.MDL -> BigDecimal.ONE; Currency.USD -> BigDecimal(10+index); else -> BigDecimal("20") } },Instant.parse("2026-11-04T10:00:00Z")) }
        val records=snapshots.map { DateFetch(it.date,it.date,FetchKind.PUBLISHED,it.fetchedAt) }
        return AnalyticsUiState(period,data=GetAnalytics()(snapshots,records,Currency.USD,Currency.MDL,period),reading=false)
    }
    private fun show(state: AnalyticsUiState,onRefresh: ()->Unit={}) {
        compose.setContent { MaterialTheme { AnalyticsScreen(state,{},{},{},onRefresh) } }
    }
    private fun scrollTo(tag: String,days: Int) { compose.onNodeWithTag("analytics_${days}_screen").performScrollToNode(hasTestTag(tag)) }
    private fun metric(tag: String,expected: String,days: Int) {
        scrollTo(tag,days)
        compose.onNode(hasText(expected) and hasAnyAncestor(hasTestTag(tag))).assertIsDisplayed()
    }
    private fun readyChart(days: Int) {
        scrollTo("rate_chart",days)
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag("rate_chart") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Ready")).fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test fun weeklyChartAndAllSixMetricsUseKnownFixtureValues() {
        show(state(7));readyChart(7)
        metric("stat_min","10.000000",7);metric("stat_max","16.000000",7);metric("stat_mean","13.000000",7)
        metric("stat_delta","+6.000000",7);metric("stat_percent","+60.0000%",7);metric("stat_trend","↑ Rising",7)
    }
    @Test fun monthlyScreenUsesThirtyDatesAndKnownAverage() {
        show(state(30));readyChart(30)
        compose.onNodeWithTag("analytics_coverage").assertTextContains("30 observations",substring=true)
        metric("stat_min","10.000000",30);metric("stat_max","39.000000",30);metric("stat_mean","24.500000",30)
        metric("stat_delta","+29.000000",30);metric("stat_percent","+290.0000%",30);metric("stat_trend","↑ Rising",30)
    }
    @Test fun onePointShowsUnknownComparisonsInsteadOfZero() {
        show(state(7,1));readyChart(7)
        scrollTo("analytics_single_point",7);compose.onNodeWithTag("analytics_single_point").assertExists()
        metric("stat_min","10.000000",7);metric("stat_mean","10.000000",7)
        metric("stat_delta","Unavailable",7);metric("stat_percent","Unavailable",7);metric("stat_trend","Unavailable",7)
    }
    @Test fun emptyWindowHasNoInventedChartOrStatistics() {
        show(state(7,0));compose.onNodeWithTag("analytics_empty").assertTextContains("No observations",substring=true)
        compose.onNodeWithTag("rate_chart").assertDoesNotExist();compose.onNodeWithTag("stat_mean").assertDoesNotExist()
        compose.onNodeWithTag("analytics_period").assertTextContains("04 Nov 2026",substring=true)
    }
    @Test fun failedReadOffersRetryWithoutFabricatingData() {
        var clicked=false
        show(state(7,0).copy(error="Could not read saved history.")) { clicked=true }
        compose.onNodeWithTag("analytics_7_screen").performScrollToNode(hasTestTag("analytics_retry"))
        compose.onNodeWithTag("analytics_retry").performClick()
        compose.runOnIdle { check(clicked) }
        compose.onNodeWithTag("rate_chart").assertDoesNotExist()
    }
}
