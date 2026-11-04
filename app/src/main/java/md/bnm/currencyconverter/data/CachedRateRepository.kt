package md.bnm.currencyconverter.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import md.bnm.currencyconverter.domain.*
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class CachedRateRepository(
    private val remote: RateRemoteSource,
    private val local: RateLocalSource,
    private val clock: Clock,
) : RateRepository {
    private val networkSlots = Semaphore(3)
    private suspend fun fetchAndStore(date: LocalDate): RemoteOutcome = networkSlots.withPermit {
        delay(200)
        val outcome = remote.fetch(date)
        when (outcome) {
            is RemoteOutcome.Published -> {
                if (outcome.snapshot.date > date) return@withPermit RemoteOutcome.Failure("BNM returned a future rate date.")
                if (Currency.entries.all { it in outcome.snapshot.ratesMdlPerUnit }) {
                    local.savePublished(date, outcome.snapshot)
                } else {
                    // A partial response can report a missing selected currency, but must not replace valid saved data.
                    local.recordFetch(DateFetch(date, outcome.snapshot.date, FetchKind.FAILED, clock.instant()))
                }
            }
            RemoteOutcome.Unpublished -> local.recordFetch(DateFetch(date, null, FetchKind.UNPUBLISHED, clock.instant()))
            is RemoteOutcome.Failure -> local.recordFetch(DateFetch(date, null, FetchKind.FAILED, clock.instant()))
        }
        outcome
    }
    override suspend fun resolve(date: LocalDate): ResolvedRate {
        for (offset in 0L..14L) {
            val requested = date.minusDays(offset)
            when (val outcome = fetchAndStore(requested)) {
                is RemoteOutcome.Published -> return ResolvedRate(outcome.snapshot, false, outcome.snapshot.date != date)
                RemoteOutcome.Unpublished -> continue
                is RemoteOutcome.Failure -> break
            }
        }
        val saved = local.latestOnOrBefore(date)
            ?: throw RatesUnavailable("No rates available from BNM or saved data. Connect to the internet and retry.")
        return ResolvedRate(saved, true, saved.date != date)
    }
    override suspend fun cached(date: LocalDate) = local.latestOnOrBefore(date)
    override suspend fun history(start: LocalDate, end: LocalDate) = local.inPeriod(start, end)
    override suspend fun refreshHistory(today: LocalDate, force: Boolean, progress: (HistoryProgress) -> Unit): HistoryRefresh = coroutineScope {
        val completed = AtomicInteger(0)
        val failures = AtomicInteger(0)
        progress(HistoryProgress(0, 30, 0))
        // Submit only one small batch at a time, so an interactive conversion never queues behind the entire month.
        for (batch in (29L downTo 0L).toList().chunked(3)) {
            batch.map { offset -> async {
                val date = today.minusDays(offset)
                val recorded = local.fetchRecord(date)
                val recent = offset < 7
                val mustFetch = force || recent || recorded == null || recorded.kind == FetchKind.FAILED
                if (mustFetch) {
                    val outcome = fetchAndStore(date)
                    if (outcome is RemoteOutcome.Failure ||
                        (outcome is RemoteOutcome.Published && !Currency.entries.all { it in outcome.snapshot.ratesMdlPerUnit })) failures.incrementAndGet()
                }
                progress(HistoryProgress(completed.incrementAndGet(), 30, failures.get()))
            } }.awaitAll()
        }
        HistoryRefresh(completed.get(), failures.get())
    }
}
