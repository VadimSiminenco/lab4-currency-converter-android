package md.bnm.currencyconverter

import android.app.Application
import android.content.Context
import androidx.room.Room
import md.bnm.currencyconverter.data.*
import md.bnm.currencyconverter.domain.RateRepository
import okhttp3.OkHttpClient
import java.time.Clock
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    val database = Room.databaseBuilder(context.applicationContext, RateDatabase::class.java, "currency-rates.db").build()
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build()
    val repository: RateRepository = CachedRateRepository(BnmRemoteSource(client, BnmXmlParser(), clock), RoomRateStore(database), clock)
}
class CurrencyApplication : Application() { val container by lazy { AppContainer(this) } }
