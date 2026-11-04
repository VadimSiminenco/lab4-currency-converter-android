package md.bnm.currencyconverter.domain

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface RateRemoteSource { suspend fun fetch(date: LocalDate): RemoteOutcome }
interface RateLocalSource {
    suspend fun latestOnOrBefore(date: LocalDate): RateSnapshot?
    suspend fun inPeriod(start: LocalDate, end: LocalDate): List<RateSnapshot>
    suspend fun fetchRecord(date: LocalDate): DateFetch?
    suspend fun savePublished(requestDate: LocalDate, snapshot: RateSnapshot)
    suspend fun recordFetch(record: DateFetch)
    fun observePeriod(start: LocalDate, end: LocalDate): Flow<List<RateSnapshot>> = flow { emit(inPeriod(start, end)) }
    fun observeFetchRecords(start: LocalDate, end: LocalDate): Flow<List<DateFetch>> = flow {
        val records = mutableListOf<DateFetch>()
        var day = start
        while (day <= end) { fetchRecord(day)?.let(records::add); day = day.plusDays(1) }
        emit(records)
    }
}
interface RateRepository {
    suspend fun resolve(date: LocalDate): ResolvedRate
    suspend fun cached(date: LocalDate): RateSnapshot?
    suspend fun history(start: LocalDate, end: LocalDate): List<RateSnapshot>
    suspend fun refreshHistory(today: LocalDate, force: Boolean = false, progress: (HistoryProgress) -> Unit = {}): HistoryRefresh
    fun observeHistory(start: LocalDate, end: LocalDate): Flow<List<RateSnapshot>> = flow { emit(history(start, end)) }
    fun observeFetchRecords(start: LocalDate, end: LocalDate): Flow<List<DateFetch>> = flow { emit(emptyList()) }
}
