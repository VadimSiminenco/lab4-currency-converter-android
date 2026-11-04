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
import md.bnm.currencyconverter.ui.ConverterViewModel
import md.bnm.currencyconverter.ui.CurrencyApp

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
            CurrencyApp(state, vm::amount, vm::source, vm::target, vm::swap, vm::convert) { vm.refreshHistory(true) }
        }
    }
}
