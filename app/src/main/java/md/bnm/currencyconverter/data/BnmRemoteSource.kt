package md.bnm.currencyconverter.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import md.bnm.currencyconverter.domain.*
import okhttp3.*
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

class BnmRemoteSource(
    private val client: OkHttpClient,
    private val parser: BnmXmlParser,
    private val clock: Clock,
    private val endpoint: String = "https://www.bnm.md/en/official_exchange_rates",
) : RateRemoteSource {
    override suspend fun fetch(date: LocalDate): RemoteOutcome {
        var outcome: RemoteOutcome = RemoteOutcome.Failure("BNM is unavailable", true)
        repeat(2) { attempt ->
            val request = Request.Builder().url("$endpoint?get_xml=1&date=${date.format(DateTimeFormatter.ofPattern("dd.MM.uuuu"))}")
                .header("Accept", "application/xml").header("User-Agent", "MoldovaCurrency-Lab4/0.1").build()
            outcome = execute(request)
            if (outcome !is RemoteOutcome.Failure || !(outcome as RemoteOutcome.Failure).retryable) return outcome
            if (attempt == 0) delay(350)
        }
        return outcome
    }
    private suspend fun execute(request: Request): RemoteOutcome = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume(RemoteOutcome.Failure("Could not reach BNM. Check your connection.", true))
            }
            override fun onResponse(call: Call, response: Response) {
                val outcome = response.use {
                    when {
                        it.code == 404 -> RemoteOutcome.Unpublished
                        !it.isSuccessful -> RemoteOutcome.Failure("BNM returned HTTP ${it.code}.", it.code >= 500 || it.code == 429)
                        else -> try {
                            val body = it.body
                            if (body == null || body.contentLength() > 2_000_000) RemoteOutcome.Failure("Invalid BNM response.")
                            else parser.parse(body.string(), clock.instant())?.let { snapshot -> RemoteOutcome.Published(snapshot) }
                                ?: RemoteOutcome.Unpublished
                        } catch (_: Exception) { RemoteOutcome.Failure("BNM returned invalid exchange-rate data.") }
                    }
                }
                if (continuation.isActive) continuation.resume(outcome)
            }
        })
    }
}
