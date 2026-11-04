package md.bnm.currencyconverter

import md.bnm.currencyconverter.domain.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

val testDate: LocalDate = LocalDate.of(2026, 11, 4)
val testInstant: Instant = Instant.parse("2026-11-04T10:00:00Z")
fun snapshot(date: LocalDate = testDate) = RateSnapshot(date, "Synthetic fixture", mapOf(
    Currency.MDL to BigDecimal.ONE, Currency.USD to BigDecimal("18"), Currency.EUR to BigDecimal("20"),
    Currency.RON to BigDecimal("4"), Currency.GBP to BigDecimal("24"), Currency.UAH to BigDecimal("0.4")), testInstant)
class MemoryStore : RateLocalSource {
    val snapshots = mutableMapOf<LocalDate, RateSnapshot>()
    val records = mutableMapOf<LocalDate, DateFetch>()
    override suspend fun latestOnOrBefore(date: LocalDate) = snapshots.values.filter { it.date <= date }.maxByOrNull { it.date }
    override suspend fun inPeriod(start: LocalDate, end: LocalDate) = snapshots.values.filter { it.date >= start && it.date <= end }.sortedBy { it.date }
    override suspend fun fetchRecord(date: LocalDate) = records[date]
    override suspend fun savePublished(requestDate: LocalDate, snapshot: RateSnapshot) {
        snapshots[snapshot.date] = snapshot
        records[requestDate] = DateFetch(requestDate, snapshot.date, FetchKind.PUBLISHED, snapshot.fetchedAt)
    }
    override suspend fun recordFetch(record: DateFetch) { records[record.date] = record }
}
