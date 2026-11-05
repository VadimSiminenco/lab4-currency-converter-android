package md.bnm.currencyconverter
import android.app.Application
import android.content.Context
import md.bnm.currencyconverter.domain.RateRepository
import java.time.Clock

internal data class AppBackend(val clock: Clock, val repository: RateRepository)
class AppContainer(context: Context) {
    private val backend = createBackend(context)
    val clock: Clock = backend.clock
    val repository: RateRepository = backend.repository
}
class CurrencyApplication : Application() { val container by lazy { AppContainer(this) } }
