package md.bnm.currencyconverter.e2e
import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.net.URI
import java.time.Duration

/** Black-box native tests using Appium Java Client and Selenium WebDriverWait/By/WebElement. */
class CurrencyE2ETest {
    @get:Rule val failureEvidence = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            try { capture("failed-${description.methodName}") } catch (_: Exception) { }
        }
    }
    companion object {
        private const val PKG="md.bnm.currencyconverter.e2e"
        private lateinit var driver: AndroidDriver
        private val serial=System.getProperty("e2e.serial")
        private val adb=System.getProperty("e2e.adb")
        private fun shell(vararg arguments: String): String {
            val process=ProcessBuilder(listOf(adb,"-s",serial,"shell")+arguments).redirectErrorStream(true).start()
            val output=process.inputStream.bufferedReader().readText()
            check(process.waitFor()==0) { output }; return output
        }
        private fun control(mode: String?=null): Int {
            val args=mutableListOf("am","broadcast","-f","0x20","-n","$PKG/md.bnm.currencyconverter.E2eControlReceiver")
            if(mode!=null) args.addAll(listOf("--es","mode",mode))
            val result=shell(*args.toTypedArray())
            return Regex("data=.([0-9]+).").find(result)?.groupValues?.get(1)?.toInt() ?: error(result)
        }
        @BeforeClass @JvmStatic fun connect() {
            require(serial.startsWith("emulator-")) { "E2E network tests require an emulator" }
            val options=UiAutomator2Options().setUdid(serial).setDeviceName("Pixel_5").setAppPackage(PKG)
                .setAppActivity("md.bnm.currencyconverter.MainActivity").setNoReset(true)
            options.setCapability("appium:autoLaunch",false)
            options.setCapability("appium:disableIdLocatorAutocompletion",true)
            options.setCapability("appium:newCommandTimeout",180)
            driver=AndroidDriver(URI(System.getProperty("e2e.server")).toURL(),options)
        }
        @AfterClass @JvmStatic fun disconnect() { if(::driver.isInitialized) driver.quit() }
    }
    private val wait get()=WebDriverWait(driver,Duration.ofSeconds(60)).ignoring(StaleElementReferenceException::class.java)
    private fun visibleNodes(id: String): List<WebElement> = try {
        driver.findElements(AppiumBy.id(id)).filter { it.isDisplayed }
    } catch(error: WebDriverException) {
        // During a cold-start splash UiAutomator2 can report this instead of returning an empty list.
        // Other driver/protocol failures must still fail the test immediately.
        if(error.message?.contains("root AccessibilityNodeInfo")==true) emptyList() else throw error
    }
    private fun node(id: String): WebElement = wait.until { visibleNodes(id).firstOrNull() }!!
    private fun visible(id: String)=visibleNodes(id).isNotEmpty()
    private fun text(id: String): String {
        val element=node(id)
        return (listOf(element.text,element.getAttribute("content-desc") ?: "")+element.findElements(By.xpath(".//*[@text!='']")).map { it.text }).joinToString(" ")
    }
    private fun scroll(id: String): WebElement {
        repeat(9) {
            if(visible(id)) return node(id)
            driver.executeScript("mobile: scrollGesture",mapOf("left" to 35,"top" to 220,"width" to 990,"height" to 1650,"direction" to "down","percent" to 0.65))
        }
        return node(id)
    }
    private fun expect(id: String,value: String) { scroll(id);wait.until { text(id).contains(value) } }
    private fun click(id: String) { scroll(id).click() }
    private fun pair(from: String,to: String) {
        click("source_currency");node("source_currency_$from").click()
        click("target_currency");node("target_currency_$to").click()
    }
    private fun amount(value: String) {
        val input=node("amount_input");input.clear();input.sendKeys(value)
        if(driver.isKeyboardShown) driver.hideKeyboard()
    }
    private fun launch(mode: String="ready") {
        driver.terminateApp(PKG)
        assertTrue(shell("pm","clear",PKG).contains("Success")) // test APK only; production data is never cleared.
        control(mode);driver.activateApp(PKG);node("amount_input")
    }
    private fun settled() { expect("saved_date","04 Nov 2026");assertEquals(30,control()) }
    private fun capture(name: String) {
        val folder=File("build/evidence").apply { mkdirs() }
        File(folder,"$name.png").writeBytes(driver.getScreenshotAs(OutputType.BYTES))
        File(folder,"$name.xml").writeText(driver.pageSource ?: error("No page source returned"))
    }
    @Test fun conversionCrossRateAndReverseHaveIndependentExpectedAmounts() {
        launch();pair("USD","EUR");amount("100");click("convert_button")
        expect("result_amount","90.00 EUR");expect("rate_date","04 Nov 2026");capture("conversion")
        node("nav_converter").click(); // Reopen from the top by relaunching the activity without clearing Room.
        driver.terminateApp(PKG);driver.activateApp(PKG);node("amount_input")
        pair("EUR","USD");amount("90");click("convert_button");expect("result_amount","100.00 USD")
    }
    private fun analytics(days: Int) {
        launch();settled();node(if(days==7) "nav_week" else "nav_month").click()
        expect("analytics_period",if(days==7) "29 Oct 2026" else "06 Oct 2026")
        expect("analytics_latest","18.000000");scroll("rate_chart");assertTrue(node("rate_chart").isDisplayed);capture("chart-$days")
        expect("analytics_coverage","$days observations across $days calendar days")
        val values=if(days==7) listOf("17.940000","18.000000","17.970000","+0.060000","+0.3344%","↑ Rising")
            else listOf("17.710000","18.000000","17.855000","+0.290000","+1.6375%","↑ Rising")
        listOf("stat_min","stat_max","stat_mean","stat_delta","stat_percent","stat_trend").zip(values).forEach { (tag,value)->expect(tag,value) }
        capture("statistics-$days")
    }
    @Test fun weeklyAnalyticsAssertDatesCoverageAndAllSixValues()=analytics(7)
    @Test fun monthlyAnalyticsAssertDatesCoverageAndAllSixValues()=analytics(30)
    @Test fun failureEmptyStateRetryAndRecovery() {
        launch("failure");click("convert_button");expect("service_error","No rates available")
        node("nav_week").click();expect("analytics_empty","No observations")
        assertTrue(scroll("analytics_retry").isDisplayed);capture("error")
        control("ready");click("analytics_retry");expect("analytics_latest","18.000000")
    }
    @Test fun sameCurrencyHasNoAdditionalRemoteLookup() {
        launch();settled()
        driver.executeScript("mobile: scrollGesture",mapOf("left" to 35,"top" to 220,"width" to 990,"height" to 1650,"direction" to "up","percent" to 0.9))
        pair("EUR","EUR");amount("123.45")
        val before=control()
        click("convert_button");expect("result_amount","123.45 EUR");expect("conversion_result","Same currency")
        assertEquals(before,control());capture("same-currency")
    }
    @Test fun offlineCacheSurvivesProcessRelaunchWithOriginalDates() {
        launch();settled()
        val airplane=shell("settings","get","global","airplane_mode_on").trim()
        val wifi=shell("settings","get","global","wifi_on").trim()
        require(airplane in listOf("0","1") && wifi in listOf("0","1"))
        try {
            shell("cmd","connectivity","airplane-mode","enable");shell("svc","wifi","disable")
            driver.terminateApp(PKG);driver.activateApp(PKG);node("amount_input");click("convert_button")
            expect("result_amount","1,800.00 MDL");expect("rate_date","04 Nov 2026");expect("offline_banner","saved snapshot")
            capture("offline-cache");node("nav_week").click();expect("analytics_cache_warning","Saved data is preserved")
            expect("analytics_coverage","7 observations");expect("stat_mean","17.970000")
        } finally {
            if(airplane=="0") shell("cmd","connectivity","airplane-mode","disable")
            if(wifi=="1") shell("svc","wifi","enable")
        }
    }
}
