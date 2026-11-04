package md.bnm.currencyconverter

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import md.bnm.currencyconverter.data.RateDatabase
import md.bnm.currencyconverter.data.RoomRateStore
import md.bnm.currencyconverter.domain.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "test-rates-${UUID.randomUUID()}.db"
    private var database: RateDatabase? = null
    private val day = LocalDate.of(2026, 11, 4)
    private fun open(): RoomRateStore {
        database = Room.databaseBuilder(context, RateDatabase::class.java, name).build()
        return RoomRateStore(database!!)
    }
    private fun sample(usd: String = "18.1234567890123456789") = RateSnapshot(day, "Synthetic fixture",
        Currency.entries.associateWith { if (it == Currency.MDL) BigDecimal.ONE else BigDecimal(usd) }, Instant.parse("2026-11-04T10:00:00Z"))

    @After fun cleanup() { database?.close(); context.deleteDatabase(name) }

    @Test fun dataAndDecimalPrecisionSurviveReopening() = runBlocking {
        val first = open()
        val original = sample()
        first.savePublished(day, original)
        database!!.close()
        val restarted = open()
        assertEquals(original, restarted.latestOnOrBefore(day))
        assertEquals(1, restarted.inPeriod(day, day).size)
        assertEquals(FetchKind.PUBLISHED, restarted.fetchRecord(day)!!.kind)
    }

    @Test fun incompleteUpdatePreservesPriorSnapshot() = runBlocking {
        val store = open(); val original = sample(); store.savePublished(day, original)
        val partial = original.copy(ratesMdlPerUnit = mapOf(Currency.MDL to BigDecimal.ONE, Currency.USD to BigDecimal("17")))
        try { store.savePublished(day, partial); fail("Expected incomplete snapshot rejection") }
        catch (_: IllegalArgumentException) { }
        assertEquals(original, store.latestOnOrBefore(day))
    }

    @Test fun correctionReplacesSameDateWithoutDuplicates() = runBlocking {
        val store = open(); store.savePublished(day, sample("18")); store.savePublished(day, sample("19"))
        assertEquals(1, store.inPeriod(day, day).size)
        assertEquals(BigDecimal("19"), store.latestOnOrBefore(day)!!.ratesMdlPerUnit[Currency.USD])
    }
}
