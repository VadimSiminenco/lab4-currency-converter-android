package md.bnm.currencyconverter.data

import androidx.room.*
import md.bnm.currencyconverter.domain.*
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "snapshots")
data class SnapshotEntity(@PrimaryKey val date: String, val source: String, val fetchedAt: String)
@Entity(tableName = "rates", primaryKeys = ["date", "code"],
    foreignKeys = [ForeignKey(entity = SnapshotEntity::class, parentColumns = ["date"], childColumns = ["date"], onDelete = ForeignKey.CASCADE)])
data class RateEntity(val date: String, val code: String, val rate: String)
@Entity(tableName = "requests")
data class RequestEntity(@PrimaryKey val date: String, val effectiveDate: String?, val kind: String, val checkedAt: String)
data class SnapshotRows(
    @Embedded val header: SnapshotEntity,
    @Relation(parentColumn = "date", entityColumn = "date") val rates: List<RateEntity>,
) {
    fun toDomain(): RateSnapshot = RateSnapshot(LocalDate.parse(header.date), header.source,
        rates.associate { Currency.valueOf(it.code) to it.rate.toBigDecimal() }, Instant.parse(header.fetchedAt))
}
@Dao
interface RateDao {
    @Transaction @Query("SELECT * FROM snapshots WHERE date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun latest(date: String): SnapshotRows?
    @Transaction @Query("SELECT * FROM snapshots WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun period(start: String, end: String): List<SnapshotRows>
    @Query("SELECT * FROM requests WHERE date = :date") suspend fun request(date: String): RequestEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSnapshot(header: SnapshotEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRates(rates: List<RateEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRequest(request: RequestEntity)
}
@Database(entities = [SnapshotEntity::class, RateEntity::class, RequestEntity::class], version = 1, exportSchema = true)
abstract class RateDatabase : RoomDatabase() { abstract fun rates(): RateDao }
class RoomRateStore(private val database: RateDatabase) : RateLocalSource {
    private val dao get() = database.rates()
    override suspend fun latestOnOrBefore(date: LocalDate) = dao.latest(date.toString())?.toDomain()
    override suspend fun inPeriod(start: LocalDate, end: LocalDate) = dao.period(start.toString(), end.toString()).map { it.toDomain() }
    override suspend fun fetchRecord(date: LocalDate): DateFetch? = dao.request(date.toString())?.let {
        DateFetch(LocalDate.parse(it.date), it.effectiveDate?.let(LocalDate::parse), FetchKind.valueOf(it.kind), Instant.parse(it.checkedAt))
    }
    override suspend fun savePublished(requestDate: LocalDate, snapshot: RateSnapshot) {
        // Validate before entering the transaction; replacing a header atomically replaces its complete rate set.
        require(snapshot.ratesMdlPerUnit.values.all { it.signum() > 0 })
        require(Currency.entries.all { it in snapshot.ratesMdlPerUnit }) { "Incomplete snapshot must not replace the cache" }
        database.withTransaction {
            dao.insertSnapshot(SnapshotEntity(snapshot.date.toString(), snapshot.sourceName, snapshot.fetchedAt.toString()))
            dao.insertRates(snapshot.ratesMdlPerUnit.map { (code, rate) -> RateEntity(snapshot.date.toString(), code.name, rate.toPlainString()) })
            dao.insertRequest(RequestEntity(requestDate.toString(), snapshot.date.toString(), FetchKind.PUBLISHED.name, snapshot.fetchedAt.toString()))
        }
    }
    override suspend fun recordFetch(record: DateFetch) {
        dao.insertRequest(RequestEntity(record.date.toString(), record.effectiveDate?.toString(), record.kind.name, record.checkedAt.toString()))
    }
}
