package md.bnm.currencyconverter

import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import md.bnm.currencyconverter.data.CachedRateRepository
import md.bnm.currencyconverter.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class RepositoryTest {
    private val clock = Clock.fixed(testInstant, ZoneOffset.UTC)
    @Test fun previousDateIsExplicitAndSnapshotPersisted() = runTest {
        val store = MemoryStore(); val queried = mutableListOf<LocalDate>()
        val repo = CachedRateRepository(object : RateRemoteSource {
            override suspend fun fetch(date: LocalDate): RemoteOutcome {
                queried += date
                return if (date == testDate) RemoteOutcome.Unpublished else RemoteOutcome.Published(snapshot(date))
            }
        }, store, clock)
        val result = repo.resolve(testDate)
        assertFalse(result.fromCache); assertTrue(result.usedPreviousDate)
        assertEquals(testDate.minusDays(1), result.snapshot.date)
        assertEquals(listOf(testDate, testDate.minusDays(1)), queried)
        assertEquals(result.snapshot, store.latestOnOrBefore(testDate))
    }
    @Test fun providerFailureUsesRealSavedDateAndPreservesData() = runTest {
        val store = MemoryStore(); val saved = snapshot(testDate.minusDays(3)); store.savePublished(saved.date, saved)
        val repo = CachedRateRepository(object : RateRemoteSource { override suspend fun fetch(date: LocalDate) = RemoteOutcome.Failure("offline") }, store, clock)
        val result = repo.resolve(testDate)
        assertTrue(result.fromCache); assertEquals(saved, result.snapshot); assertEquals(saved, store.latestOnOrBefore(testDate))
    }
    @Test fun noCacheNoDataRaisesActionableFailure() = runTest {
        val queried = mutableListOf<LocalDate>()
        val repo = CachedRateRepository(object : RateRemoteSource { override suspend fun fetch(date: LocalDate): RemoteOutcome { queried += date; return RemoteOutcome.Unpublished } }, MemoryStore(), clock)
        try { repo.resolve(testDate); fail("Expected unavailable") } catch (e: RatesUnavailable) { assertTrue(e.message!!.contains("Connect")) }
        assertEquals(15, queried.size); assertEquals(testDate.minusDays(14), queried.last())
    }
    @Test fun historyIsThirtyCalendarDatesBoundedAndDeduplicated() = runTest {
        val store = MemoryStore(); val dates = mutableListOf<LocalDate>(); var active = 0; var peak = 0
        val repo = CachedRateRepository(object : RateRemoteSource {
            override suspend fun fetch(date: LocalDate): RemoteOutcome {
                dates += date; active++; peak = maxOf(peak, active); delay(20); active--
                return RemoteOutcome.Published(snapshot(date))
            }
        }, store, clock)
        val progress = mutableListOf<HistoryProgress>()
        repo.refreshHistory(testDate, progress = { progress += it })
        assertEquals(30, dates.size); assertEquals(testDate.minusDays(29), dates.min()); assertEquals(testDate, dates.max())
        assertTrue(peak <= 3); assertEquals(30, progress.last().completed); assertEquals(30, store.snapshots.size)
        dates.clear(); repo.refreshHistory(testDate)
        assertEquals(7, dates.size) // Revalidate recent dates; reuse older resolved dates.
        dates.clear(); repo.refreshHistory(testDate, force = true)
        assertEquals(30, dates.size)
    }
    @Test fun oldHistoryIsNotShiftedIntoCurrentWindow() = runTest {
        val store=MemoryStore(); val old=snapshot(testDate.minusDays(60)); store.savePublished(old.date,old)
        val repo=CachedRateRepository(object : RateRemoteSource { override suspend fun fetch(date: LocalDate)=RemoteOutcome.Failure("offline") },store,clock)
        assertTrue(repo.history(testDate.minusDays(29),testDate).isEmpty()); assertEquals(old,repo.cached(testDate))
    }
    @Test fun incompleteResponseDoesNotOverwriteValidSnapshot() = runTest {
        val store = MemoryStore(); val original = snapshot(); store.savePublished(testDate, original)
        val partial = original.copy(ratesMdlPerUnit = original.ratesMdlPerUnit - Currency.EUR)
        val repo = CachedRateRepository(object : RateRemoteSource {
            override suspend fun fetch(date: LocalDate) = RemoteOutcome.Published(partial.copy(date = date))
        }, store, clock)
        val resolved = repo.resolve(testDate)
        assertFalse(resolved.snapshot.ratesMdlPerUnit.containsKey(Currency.EUR))
        assertEquals(original, store.latestOnOrBefore(testDate))
        assertEquals(FetchKind.FAILED, store.fetchRecord(testDate)!!.kind)
    }
    @Test fun futureEffectiveDateIsRejectedAndCacheRemainsUsable() = runTest {
        val store = MemoryStore(); val original = snapshot(); store.savePublished(testDate, original)
        val repo = CachedRateRepository(object : RateRemoteSource {
            override suspend fun fetch(date: LocalDate) = RemoteOutcome.Published(snapshot(date.plusDays(1)))
        }, store, clock)
        val resolved = repo.resolve(testDate)
        assertTrue(resolved.fromCache); assertEquals(original, resolved.snapshot)
        assertEquals(1, store.snapshots.size)
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun interactiveConversionDoesNotQueueBehindEntireHistory() = runTest {
        val calls = mutableListOf<LocalDate>()
        val repo = CachedRateRepository(object : RateRemoteSource {
            override suspend fun fetch(date: LocalDate): RemoteOutcome {
                calls += date; delay(1_000); return RemoteOutcome.Published(snapshot(date))
            }
        }, MemoryStore(), clock)
        val history = launch { repo.refreshHistory(testDate) }
        runCurrent(); advanceTimeBy(201)
        val interactive = async { repo.resolve(testDate) }
        interactive.await()
        assertTrue("Conversion must run before the backlog of historical dates", calls.indexOf(testDate) in 0..6)
        history.join()
    }
}
