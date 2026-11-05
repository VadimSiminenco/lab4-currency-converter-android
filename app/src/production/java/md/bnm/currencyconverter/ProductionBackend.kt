package md.bnm.currencyconverter
import android.content.Context
import androidx.room.Room
import md.bnm.currencyconverter.data.*
import okhttp3.OkHttpClient
import java.time.Clock
import java.util.concurrent.TimeUnit

internal fun createBackend(context: Context): AppBackend {
    val clock = Clock.systemDefaultZone()
    val database = Room.databaseBuilder(context.applicationContext, RateDatabase::class.java, "currency-rates.db").build()
    val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build()
    return AppBackend(clock, CachedRateRepository(BnmRemoteSource(client, BnmXmlParser(), clock), RoomRateStore(database), clock))
}
