package md.bnm.currencyconverter

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import md.bnm.currencyconverter.domain.*
import md.bnm.currencyconverter.ui.AnalyticsViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.*

private class MutableTestClock(var instantValue: Instant = testInstant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = instantValue
}
private class ObservableHistory : RateRepository {
    val snapshots=MutableStateFlow(listOf(snapshot()))
    var refreshCount=0
    var failRead=false
    override suspend fun resolve(date: LocalDate): ResolvedRate = error("No conversion lookup expected")
    override suspend fun cached(date: LocalDate)=snapshots.value.lastOrNull()
    override suspend fun history(start: LocalDate,end: LocalDate)=snapshots.value.filter { it.date in start..end }
    override fun observeHistory(start: LocalDate,end: LocalDate): Flow<List<RateSnapshot>> = flow {
        if (failRead) error("Synthetic storage read failure")
        emitAll(snapshots.map { all -> all.filter { it.date in start..end } })
    }
    override fun observeFetchRecords(start: LocalDate,end: LocalDate)=snapshots.map { all -> all.filter { it.date in start..end }.map { DateFetch(it.date,it.date,FetchKind.PUBLISHED,it.fetchedAt) } }
    override suspend fun refreshHistory(today: LocalDate,force: Boolean,progress: (HistoryProgress)->Unit): HistoryRefresh { refreshCount++;return HistoryRefresh(30,0) }
}
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    @Test fun retryResubscribesAfterStorageReadFailure()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo=ObservableHistory().apply { failRead=true };val vm=AnalyticsViewModel(7,repo,MutableTestClock())
        try {
            runCurrent();assertNotNull(vm.state.value.error)
            assertEquals(AnalyticsResult.InsufficientData,vm.state.value.data.result)
            repo.failRead=false;vm.retryRead();runCurrent()
            assertNull(vm.state.value.error);assertFalse(vm.state.value.reading)
            assertEquals(1,vm.state.value.data.result.observations.size)
            assertEquals(0,repo.refreshCount)
        } finally { vm.viewModelScope.cancel();Dispatchers.resetMain() }
    }
    @Test fun cachedHistoryLoadsWithoutAnotherNetworkRefresh()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo=ObservableHistory();val vm=AnalyticsViewModel(7,repo,MutableTestClock())
        try {
            runCurrent();assertFalse(vm.state.value.reading);assertEquals(1,vm.state.value.data.result.observations.size)
            assertEquals(0,repo.refreshCount)
        } finally { vm.viewModelScope.cancel();Dispatchers.resetMain() }
    }
    @Test fun pairChangeAndSwapRecomputeFromSameSnapshots()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo=ObservableHistory();val vm=AnalyticsViewModel(7,repo,MutableTestClock())
        try {
            runCurrent();vm.source(Currency.EUR);runCurrent()
            assertEquals(0,java.math.BigDecimal("20").compareTo(vm.state.value.data.result.statistics!!.mean))
            vm.swap();runCurrent()
            assertEquals(Currency.MDL,vm.state.value.source)
            assertEquals(0,java.math.BigDecimal("0.05").compareTo(vm.state.value.data.result.statistics!!.mean))
        } finally { vm.viewModelScope.cancel();Dispatchers.resetMain() }
    }
    @Test fun roomStyleFlowUpdatesStatisticsWithoutPolling()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo=ObservableHistory();val vm=AnalyticsViewModel(7,repo,MutableTestClock())
        try {
            runCurrent();repo.snapshots.value=listOf(snapshot(testDate.minusDays(1)),snapshot());runCurrent()
            assertTrue(vm.state.value.data.result is AnalyticsResult.Full)
            assertEquals(Trend.FLAT,vm.state.value.data.result.statistics!!.trend)
        } finally { vm.viewModelScope.cancel();Dispatchers.resetMain() }
    }
    @Test fun resumeAfterDateChangeDoesNotShiftWindowToStaleCache()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val clock=MutableTestClock();val vm=AnalyticsViewModel(7,ObservableHistory(),clock)
        try {
            runCurrent();clock.instantValue=testInstant.plusSeconds(30L*86400);vm.syncWindow();runCurrent()
            assertEquals(testDate.plusDays(30),vm.state.value.period.end)
            assertEquals(AnalyticsResult.InsufficientData,vm.state.value.data.result)
        } finally { vm.viewModelScope.cancel();Dispatchers.resetMain() }
    }
}
