package md.bnm.currencyconverter

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Optional live-service check, excluded by default from the deterministic suite. */
class LiveBnmSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun liveConversionShowsAmountSourceAndEffectiveDate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Opt in with the liveBnm instrumentation argument", InstrumentationRegistry.getArguments().getString("liveBnm") == "true")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("convert_button").performScrollTo().performClick()
            compose.waitUntil(45_000) { compose.onAllNodesWithTag("conversion_result").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("conversion_result").performScrollTo()
            compose.onNodeWithTag("result_amount").assertTextContains("MDL", substring = true)
            compose.onNodeWithTag("source_name").assertExists()
            compose.onNodeWithTag("rate_date").assertExists()
            compose.waitForIdle()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            val destination = File(instrumentation.targetContext.getExternalFilesDir(null), "live-conversion.png")
            destination.outputStream().use { out -> screenshot.compress(Bitmap.CompressFormat.PNG, 100, out) }
            screenshot.recycle()
        }
    }

    @Test fun offlineConversionUsesPersistentRoomData() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Opt in after a live load with the offlineBnm argument", InstrumentationRegistry.getArguments().getString("offlineBnm") == "true")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("convert_button").performScrollTo().performClick()
            compose.waitUntil(45_000) { compose.onAllNodesWithTag("conversion_result").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("conversion_result").performScrollTo()
            compose.onNodeWithTag("offline_banner").assertExists()
            compose.onNodeWithTag("rate_date").assertExists()
            val destination = File(instrumentation.targetContext.getExternalFilesDir(null), "offline-conversion.png")
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            destination.outputStream().use { out -> screenshot.compress(Bitmap.CompressFormat.PNG, 100, out) }
            screenshot.recycle()
        }
    }
}
