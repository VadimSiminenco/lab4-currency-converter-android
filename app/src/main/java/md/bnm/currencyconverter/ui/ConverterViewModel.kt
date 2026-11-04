package md.bnm.currencyconverter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import md.bnm.currencyconverter.domain.*
import java.time.Clock
import java.time.LocalDate

data class ConverterUiState(
    val amount: String = "100", val source: Currency = Currency.USD, val target: Currency = Currency.MDL,
    val amountError: String? = null, val result: ConversionResult? = null, val error: String? = null,
    val converting: Boolean = false, val historyRefreshing: Boolean = false, val historyProgress: Int = 0,
    val historyFailures: Int = 0, val historyWarning: String? = null, val savedDate: LocalDate? = null,
) {
    val canConvert: Boolean get() = AmountValidator.validate(amount).value != null && !converting
}
class ConverterViewModel(private val repository: RateRepository, private val clock: Clock) : ViewModel() {
    private val mutableState = MutableStateFlow(ConverterUiState())
    val state = mutableState.asStateFlow()
    private val converter = ConvertCurrency(repository)
    private var conversionJob: Job? = null
    private var historyJob: Job? = null
    init { refreshHistory() }
    private fun edit(change: (ConverterUiState) -> ConverterUiState) {
        conversionJob?.cancel()
        mutableState.update { change(it).copy(result = null, error = null, converting = false) }
    }
    fun amount(value: String) = edit { it.copy(amount = value, amountError = AmountValidator.validate(value).error) }
    fun source(value: Currency) = edit { it.copy(source = value) }
    fun target(value: Currency) = edit { it.copy(target = value) }
    fun swap() = edit { it.copy(source = it.target, target = it.source) }
    fun convert() {
        val input = state.value
        val validated = AmountValidator.validate(input.amount)
        if (validated.value == null || input.converting) return
        conversionJob = viewModelScope.launch {
            mutableState.update { it.copy(converting = true, result = null, error = null) }
            try {
                val result = converter(validated.value, input.source, input.target, LocalDate.now(clock))
                val savedDate = if (result.resolved == null) state.value.savedDate else repository.cached(LocalDate.now(clock))?.date
                mutableState.update { it.copy(result = result, converting = false, savedDate = savedDate ?: it.savedDate) }
            } catch (exc: CancellationException) { throw exc }
            catch (exc: RatesUnavailable) { mutableState.update { it.copy(error = exc.message, converting = false) } }
            catch (exc: CurrencyUnavailable) { mutableState.update { it.copy(error = exc.message, converting = false) } }
            catch (_: Exception) { mutableState.update { it.copy(error = "Conversion could not be completed. Please retry.", converting = false) } }
        }
    }
    fun refreshHistory(force: Boolean = false) {
        if (historyJob?.isActive == true) return
        historyJob = viewModelScope.launch {
            mutableState.update { it.copy(historyRefreshing = true, historyProgress = 0, historyFailures = 0, historyWarning = null) }
            try {
                val today = LocalDate.now(clock)
                mutableState.update { it.copy(savedDate = repository.cached(today)?.date) }
                val refreshed = repository.refreshHistory(today, force) { progress ->
                    mutableState.update { it.copy(historyProgress = maxOf(it.historyProgress, progress.completed), historyFailures = progress.failures) }
                }
                val latest = repository.cached(today)
                mutableState.update { it.copy(savedDate = latest?.date, historyRefreshing = false,
                    historyWarning = if (refreshed.failures > 0) "${refreshed.failures} dates could not be updated. Saved data is preserved." else null) }
            } catch (exc: CancellationException) { throw exc }
            catch (_: Exception) { mutableState.update { it.copy(historyRefreshing = false, historyWarning = "Could not refresh history. Existing saved data is preserved.") } }
        }
    }
}
