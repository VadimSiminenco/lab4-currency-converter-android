package md.bnm.currencyconverter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import md.bnm.currencyconverter.domain.*
import java.time.Clock
import java.time.LocalDate

data class AnalyticsUiState(
    val period: AnalyticsPeriod,
    val source: Currency = Currency.USD,
    val target: Currency = Currency.MDL,
    val data: AnalyticsData = AnalyticsData(period, AnalyticsResult.InsufficientData, null, 0, period.days),
    val reading: Boolean = true,
    val error: String? = null,
)
class AnalyticsViewModel(days: Int, private val repository: RateRepository, private val clock: Clock) : ViewModel() {
    private val mutable = MutableStateFlow(AnalyticsUiState(AnalyticsPeriod(days, LocalDate.now(clock))))
    val state = mutable.asStateFlow()
    private val analyze = GetAnalytics()
    private var observation: Job? = null
    init { observe() }
    fun source(value: Currency) { mutable.update { it.copy(source = value) }; observe() }
    fun target(value: Currency) { mutable.update { it.copy(target = value) }; observe() }
    fun swap() { mutable.update { it.copy(source = it.target, target = it.source) }; observe() }
    fun retryRead() {
        mutable.update { it.copy(period = AnalyticsPeriod(it.period.days, LocalDate.now(clock))) }
        observe()
    }
    fun syncWindow() {
        val today = LocalDate.now(clock)
        if (state.value.period.end != today) {
            mutable.update { it.copy(period = AnalyticsPeriod(it.period.days, today)) }; observe()
        }
    }
    private fun observe() {
        observation?.cancel()
        val input = state.value
        mutable.update { it.copy(reading = true, error = null, data = AnalyticsData(input.period, AnalyticsResult.InsufficientData, null, 0, input.period.days)) }
        observation = viewModelScope.launch {
            try {
                combine(repository.observeHistory(input.period.start, input.period.end), repository.observeFetchRecords(input.period.start, input.period.end)) { snapshots, records ->
                    analyze(snapshots, records, input.source, input.target, input.period)
                }.collect { data -> mutable.update { it.copy(data = data, reading = false, error = null) } }
            } catch (exc: CancellationException) { throw exc }
            catch (_: Exception) { mutable.update { it.copy(reading = false, error = "Could not read saved history. Refresh and try again.") } }
        }
    }
}
