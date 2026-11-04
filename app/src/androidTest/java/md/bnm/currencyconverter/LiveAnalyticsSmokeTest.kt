package md.bnm.currencyconverter

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Opt-in verification against previously received or live official data, not deterministic assertions of market values. */
class LiveAnalyticsSmokeTest {
    @get:Rule val compose=createEmptyComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val image=instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        image.recycle()
    }
    private fun openPeriod(days: Int) {
        compose.onNodeWithTag(if(days==7) "nav_week" else "nav_month").performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("analytics_latest").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("analytics_${days}_screen").performScrollToNode(hasTestTag("rate_chart"))
        compose.waitUntil(20_000) { compose.onAllNodes(hasTestTag("rate_chart") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Ready")).fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun liveHistoryRendersWeeklyAndMonthlyCharts() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveAnalytics")=="true")
        ActivityScenario.launch(MainActivity::class.java).use {
            for (days in listOf(7,30)) {
                openPeriod(days);capture("analytics-$days.png")
                for(tag in listOf("stat_min","stat_max","stat_mean","stat_delta","stat_percent","stat_trend")) {
                    compose.onNodeWithTag("analytics_${days}_screen").performScrollToNode(hasTestTag(tag))
                    compose.onNodeWithTag(tag).assertExists()
                }
                capture("analytics-$days-stats.png")
            }
        }
    }
    @Test fun savedAnalyticsSurviveProcessRelaunchWithoutNetwork() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("offlineAnalytics")=="true")
        ActivityScenario.launch(MainActivity::class.java).use {
            openPeriod(7)
            compose.waitUntil(60_000) { compose.onAllNodesWithTag("analytics_cache_warning").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("analytics_7_screen").performScrollToNode(hasTestTag("analytics_cache_warning"))
            capture("analytics-offline.png")
            openPeriod(30)
            compose.onNodeWithTag("analytics_30_screen").performScrollToNode(hasTestTag("stat_mean"))
            compose.onNodeWithTag("stat_mean").assertExists()
        }
    }
}
