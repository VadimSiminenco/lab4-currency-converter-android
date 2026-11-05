package md.bnm.currencyconverter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import md.bnm.currencyconverter.data.*
import md.bnm.currencyconverter.domain.*
import java.math.BigDecimal
import java.time.*
import java.time.temporal.ChronoUnit

/** This source and its exported control receiver are compiled only into the opt-in e2e flavor. */
private val fixtureClock: Clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC)
private object FixtureControl {
    @Synchronized fun record(context: Context): String {
        val prefs=context.getSharedPreferences("e2e-control",Context.MODE_PRIVATE)
        prefs.edit().putInt("requests",prefs.getInt("requests",0)+1).commit()
        return prefs.getString("mode","ready")!!
    }
}
class FakeRemoteSource(private val context: Context) : RateRemoteSource {
    override suspend fun fetch(date: LocalDate): RemoteOutcome {
        val mode=FixtureControl.record(context)
        val connectivity=context.getSystemService(ConnectivityManager::class.java)
        val network=connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        if(mode=="failure" || network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)!=true)
            return RemoteOutcome.Failure("Synthetic E2E network failure")
        val index=ChronoUnit.DAYS.between(LocalDate.of(2026,10,6),date)
        if(index !in 0L..29L || mode=="empty" || (mode=="gaps" && index%5L==0L)) return RemoteOutcome.Unpublished
        val usd=BigDecimal("17.71").add(BigDecimal(index).movePointLeft(2))
        val rates=mapOf(Currency.MDL to BigDecimal.ONE, Currency.USD to usd, Currency.EUR to BigDecimal("20"),
            Currency.RON to BigDecimal("4"),Currency.GBP to BigDecimal("24"),Currency.UAH to BigDecimal("0.45"))
        return RemoteOutcome.Published(RateSnapshot(date,"Deterministic E2E fixture",rates,fixtureClock.instant()))
    }
}
class E2eControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs=context.getSharedPreferences("e2e-control",Context.MODE_PRIVATE)
        intent.getStringExtra("mode")?.let { require(it in setOf("ready","failure","empty","gaps"));prefs.edit().putString("mode",it).commit() }
        resultData=prefs.getInt("requests",0).toString()
    }
}
internal fun createBackend(context: Context): AppBackend {
    val database=Room.databaseBuilder(context.applicationContext,RateDatabase::class.java,"currency-rates.db").build()
    return AppBackend(fixtureClock,CachedRateRepository(FakeRemoteSource(context),RoomRateStore(database),fixtureClock))
}
