package md.bnm.currencyconverter

import kotlinx.coroutines.test.runTest
import md.bnm.currencyconverter.data.CachedRateRepository
import md.bnm.currencyconverter.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.ZoneOffset

class ConversionTest {
    private val clock = Clock.fixed(testInstant, ZoneOffset.UTC)
    @Test fun directionUnitsAndRoundingAreCorrect() = runTest {
        val repository = CachedRateRepository(object : RateRemoteSource { override suspend fun fetch(date: java.time.LocalDate) = RemoteOutcome.Published(snapshot(date)) }, MemoryStore(), clock)
        val convert = ConvertCurrency(repository)
        assertEquals(BigDecimal("90.00"), convert(BigDecimal("100"), Currency.USD, Currency.EUR, testDate).converted)
        assertEquals(BigDecimal("100.00"), convert(BigDecimal("90"), Currency.EUR, Currency.USD, testDate).converted)
        assertEquals(BigDecimal("1800.00"), convert(BigDecimal("100"), Currency.USD, Currency.MDL, testDate).converted)
        assertEquals(BigDecimal("100.00"), convert(BigDecimal("1800"), Currency.MDL, Currency.USD, testDate).converted)
        assertEquals(BigDecimal("0.01"), convert(BigDecimal("0.1"), Currency.MDL, Currency.EUR, testDate).converted)
    }
    @Test fun sameCurrencyPreservesPrecisionWithoutRemoteOrCache() = runTest {
        val repository = object : RateRepository {
            override suspend fun resolve(date: java.time.LocalDate): ResolvedRate = error("No lookup allowed")
            override suspend fun cached(date: java.time.LocalDate): RateSnapshot? = error("No cache allowed")
            override suspend fun history(start: java.time.LocalDate, end: java.time.LocalDate): List<RateSnapshot> = error("No lookup")
            override suspend fun refreshHistory(today: java.time.LocalDate, force: Boolean, progress: (HistoryProgress) -> Unit): HistoryRefresh = error("No refresh")
        }
        val result = ConvertCurrency(repository)(BigDecimal("0.00123"), Currency.EUR, Currency.EUR, testDate)
        assertEquals(BigDecimal("0.00123"), result.converted); assertNull(result.resolved)
    }
    @Test fun missingCurrencyIsNotSubstituted() = runTest {
        val partial = RateSnapshot(testDate, "fixture", mapOf(Currency.MDL to BigDecimal.ONE, Currency.USD to BigDecimal("18")), testInstant)
        val repository = CachedRateRepository(object : RateRemoteSource { override suspend fun fetch(date: java.time.LocalDate) = RemoteOutcome.Published(partial) }, MemoryStore(), clock)
        try { ConvertCurrency(repository)(BigDecimal.ONE, Currency.USD, Currency.EUR, testDate); fail("Expected missing currency") }
        catch (_: CurrencyUnavailable) { }
    }
}
