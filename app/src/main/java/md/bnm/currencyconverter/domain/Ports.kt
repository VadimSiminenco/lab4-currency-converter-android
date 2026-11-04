package md.bnm.currencyconverter.domain

import java.time.LocalDate

interface RateRemoteSource { suspend fun fetch(date: LocalDate): RemoteOutcome }
interface RateLocalSource {
    suspend fun latestOnOrBefore(date: LocalDate): RateSnapshot?
    suspend fun inPeriod(start: LocalDate, end: LocalDate): List<RateSnapshot>
    suspend fun fetchRecord(date: LocalDate): DateFetch?
    suspend fun savePublished(requestDate: LocalDate, snapshot: RateSnapshot)
    suspend fun recordFetch(record: DateFetch)
}
interface RateRepository {
    suspend fun resolve(date: LocalDate): ResolvedRate
    suspend fun cached(date: LocalDate): RateSnapshot?
    suspend fun history(start: LocalDate, end: LocalDate): List<RateSnapshot>
    suspend fun refreshHistory(today: LocalDate, force: Boolean = false, progress: (HistoryProgress) -> Unit = {}): HistoryRefresh
}
