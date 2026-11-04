package md.bnm.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.LifecycleResumeEffect
import md.bnm.currencyconverter.ui.ConverterViewModel
import md.bnm.currencyconverter.ui.CurrencyApp
import md.bnm.currencyconverter.ui.AnalyticsViewModel
import md.bnm.currencyconverter.ui.AnalyticsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CurrencyApplication).container
        setContent {
            val vm: ConverterViewModel = viewModel(factory = viewModelFactory {
                initializer { ConverterViewModel(container.repository, container.clock) }
            })
            val state by vm.state.collectAsStateWithLifecycle()
            LifecycleResumeEffect(Unit) {
                vm.syncDay()
                onPauseOrDispose { }
            }
            CurrencyApp(state, vm::amount, vm::source, vm::target, vm::swap, vm::convert,
                onRefresh = { vm.refreshHistory(true) },
                analyticsContent = { days ->
                    val analytics: AnalyticsViewModel = viewModel(key = "analytics-$days", factory = viewModelFactory {
                        initializer { AnalyticsViewModel(days, container.repository, container.clock) }
                    })
                    val analysis by analytics.state.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(days) {
                        analytics.syncWindow()
                        onPauseOrDispose { }
                    }
                    AnalyticsScreen(analysis, analytics::source, analytics::target, analytics::swap,
                        onRefresh = { analytics.retryRead(); vm.refreshHistory(true) },
                        refreshing = state.historyRefreshing, progress = state.historyProgress, refreshWarning = state.historyWarning)
                })
        }
    }
}
