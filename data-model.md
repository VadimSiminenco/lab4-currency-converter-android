# Data Model — Lab 4: Mobile Currency Converter

**Version:** 1.1.0 (2026-11-04)

---

## 1. Domain Model (Kotlin)

### 1.1 Currency Enum

```kotlin
enum class Currency(val code: String, val displayName: String) {
    MDL("MDL", "Moldovan Leu"),
    EUR("EUR", "Euro"),
    USD("USD", "US Dollar"),
    RON("RON", "Romanian Leu"),
    GBP("GBP", "British Pound Sterling"),
    UAH("UAH", "Ukrainian Hryvnia");

    companion object {
        fun fromCode(code: String): Currency =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown currency code: $code")
    }
}
```

### 1.2 ExchangeRate (Domain)

Represents the normalised rate for one currency against MDL on a specific date.

```kotlin
data class ExchangeRate(
    val currency: Currency,
    val date: LocalDate,
    /** Normalised: Value / Nominal. Rate in MDL per 1 unit of `currency`. */
    val ratePerUnit: BigDecimal
)
```

### 1.3 RawRate (Remote DTO)

Returned by the XML parser before normalisation. Stored temporarily in memory only.

```kotlin
data class RawRate(
    val charCode: String,
    val nominal: BigDecimal,
    val value: BigDecimal,
    val date: LocalDate
)
```

### 1.4 AnalyticsResult (Domain)

```kotlin
sealed class AnalyticsResult {
    /** Not enough data to compute any statistics. */
    object InsufficientData : AnalyticsResult()

    /** Exactly one observation — partial statistics only. */
    data class SinglePoint(
        val date: LocalDate,
        val rate: BigDecimal
        // min=max=mean=rate; delta/percent/trend unavailable, not zero/Flat
    ) : AnalyticsResult()

    /** Full statistics available (>=2 unique effective dates inside current-calendar window). */
    data class Full(
        val observations: List<ExchangeRate>,   // sorted ascending by date
        val min: BigDecimal,
        val max: BigDecimal,
        val mean: BigDecimal,                   // division uses MathContext.DECIMAL128
        val delta: BigDecimal,                  // last.rate − first.rate
        val percent: BigDecimal?,               // null if first.rate == 0
        val trend: Trend,
        val windowStart: LocalDate,
        val windowEnd: LocalDate,
        /** True when requested-date coverage is unresolved/incomplete; n<windowDays alone does not establish failed requests. */
        val isIncomplete: Boolean,
        val availableCount: Int,
        val requestedCount: Int                 // 7 or 30
    ) : AnalyticsResult()
}
```

### 1.5 Trend Enum

```kotlin
enum class Trend(val label: String, val symbol: String) {
    RISING("Rising", "↑"),
    FALLING("Falling", "↓"),
    FLAT("No change", "→");

    fun displayText(): String = "$symbol $label"
}
```

### 1.6 ConversionResult (Domain)

```kotlin
sealed class ConversionResult {
    data class Success(
        val amount: BigDecimal,
        val fromCurrency: Currency,
        val toCurrency: Currency,
        val result: BigDecimal,     // rounded to 2 dp, HALF_UP
        val rateUsed: BigDecimal,   // the cross-rate applied
        val rateDate: LocalDate     // date of the rate used
    ) : ConversionResult()

    data class SameCurrency(val amount: BigDecimal, val currency: Currency) : ConversionResult()

    data class Error(val reason: ConversionError) : ConversionResult()
}

enum class ConversionError {
    INVALID_AMOUNT,
    NEGATIVE_AMOUNT,
    ZERO_AMOUNT,
    NO_RATE_AVAILABLE,
    ZERO_BASE_RATE
}
```

---

## 2. Room Database Entities

### 2.1 RateEntity

```kotlin
@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["currency_code", "rate_date"],
    indices = [Index(value = ["rate_date"])]
)
data class RateEntity(
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "rate_date")     val rateDate: String,       // ISO-8601: "YYYY-MM-DD"
    @ColumnInfo(name = "nominal")       val nominal: String,        // BigDecimal as String
    @ColumnInfo(name = "value")         val value: String,          // BigDecimal as String
    /** Normalised rate = value / nominal. BigDecimal stored as String to preserve precision. */
    @ColumnInfo(name = "rate_per_unit") val ratePerUnit: String
)
```

**Composite primary key:** `(currency_code, rate_date)` — guarantees uniqueness per currency per day, allows `INSERT OR REPLACE` for corrections.

### 2.2 RateDao

```kotlin
@Dao
interface RateDao {
    @Query("SELECT * FROM exchange_rates WHERE currency_code = :code AND rate_date BETWEEN :start AND :end ORDER BY rate_date ASC")
    suspend fun getRatesInRange(code: String, start: String, end: String): List<RateEntity>

    @Query("SELECT MAX(rate_date) FROM exchange_rates WHERE currency_code = :code")
    suspend fun getLatestDate(code: String): String?

    @Query("SELECT MIN(rate_date) FROM exchange_rates WHERE currency_code = :code")
    suspend fun getEarliestDate(code: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRates(rates: List<RateEntity>)

    @Query("DELETE FROM exchange_rates WHERE rate_date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
}
```

### 2.3 AppDatabase

```kotlin
@Database(
    entities = [RateEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao

    companion object {
        const val DATABASE_NAME = "currency_converter.db"
    }
}
```

### 2.4 TypeConverter for BigDecimal

Room does not natively support `BigDecimal`. Stored as `String`:

```kotlin
class BigDecimalConverter {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }
}
```

> [!NOTE]
> `toPlainString()` is used (not `toString()`) to avoid scientific notation in stored strings, for readable storage; BigDecimal(String) itself preserves valid scientific notation precisely.

---

## 3. Remote DTO / XML Mapping

### 3.1 Parsed from BNM XML

```kotlin
data class BnmDayRates(
    val date: LocalDate,            // from attribute Date="DD.MM.YYYY"
    val rates: List<RawRate>
)
```

### 3.2 XML Date Format

BNM uses `DD.MM.YYYY` in the `Date` attribute. The parser converts to `LocalDate` using:

```kotlin
val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val date = LocalDate.parse(dateString, formatter)
```

---

## 4. UI State Models

### 4.1 ConverterUiState

```kotlin
data class ConverterUiState(
    val amount: String = "",
    val fromCurrency: Currency = Currency.EUR,
    val toCurrency: Currency = Currency.MDL,
    val result: ConversionResult? = null,
    val isLoading: Boolean = false,
    val amountError: String? = null,
    val rateDate: LocalDate? = null
)
```

### 4.2 AnalyticsUiState

```kotlin
data class AnalyticsUiState(
    val fromCurrency: Currency = Currency.EUR,
    val toCurrency: Currency = Currency.MDL,
    val windowDays: Int,            // 7 or 30
    val screenState: ScreenState<AnalyticsData> = ScreenState.Loading
)

data class AnalyticsData(
    val analyticsResult: AnalyticsResult,
    val chartPoints: List<ChartPoint>,   // Float values for rendering only
    val isOffline: Boolean = false,
    val lastUpdated: LocalDate? = null
)

data class ChartPoint(
    val x: Float,           // relative day offset: preserve spacing of missing dates
    val y: Float,           // rate as Float (conversion from BigDecimal at rendering layer only)
    val label: String       // date string for axis label, e.g. "Nov 1"
)
```

### 4.3 ScreenState

```kotlin
sealed class ScreenState<out T> {
    object Loading : ScreenState<Nothing>()
    data class Success<T>(val data: T) : ScreenState<T>()
    data class Error(val message: String) : ScreenState<Nothing>() // Fatal error only; recoverable warning stays with typed Success content
    object Empty : ScreenState<Nothing>()
}
```

---

## 5. Mapping Rules

| Mapping | Location |
|---------|----------|
| `RawRate` → `RateEntity` | `data/repository/RateRepositoryImpl` |
| `RateEntity` → `ExchangeRate` | `data/repository/RateRepositoryImpl` |
| `List<ExchangeRate>` → `AnalyticsResult` | `domain/usecase/GetAnalyticsUseCase` |
| `List<ExchangeRate>` → `List<ChartPoint>` | `ui/analytics*/AnalyticsViewModel` |
| `BigDecimal` → `Double` | Only in RateLineChart, at the final rendering boundary |

> [!CAUTION]
> Floating-point conversion is confined to the final chart-rendering boundary in Compose, matching the revised specification. Domain use cases, repository and statistics retain BigDecimal. Vico2.2.0 accepts Double coordinates.

---

## 6. Validation Rules

| Field | Rule | Error |
|-------|------|-------|
| `amount` | Not blank | "Amount is required" |
| `amount` | Parseable as decimal | "Invalid number format" |
| `amount` | > 0 | "Amount must be positive" |
| `amount` | ≤ 1,000,000,000 | "Amount too large" |
| `fromCurrency == toCurrency` | Allowed; show notice | "Same currency selected — no conversion needed" |
| `ratePerUnit` for base | ≠ 0 when computing percent | Show "Cannot compute percentage" |

## Revision1.1 additions (normative)

- RateSnapshot groups same-effective-date currency rates; sourceName and fetchedAt accompany data. MDL rate=1; foreign rate unit=MDL per foreign unit. Pair rate=sourceRate/targetRate.
- RemoteOutcome distinguishes Published(snapshot), Unpublished(requestedDate) and Failure(kind,retryable). Cancellation propagates.
- RequestOutcomeEntity stores requestedDate, checkedAt, status and effectiveDate mapping separately from snapshot/rate tables. Duplicate responses for one effective date yield one observation.
- Store snapshot header and all validated rates transactionally. Invalid/partial update does not delete a previous valid set. Either decimal String fields with explicit mapping OR a consistent BigDecimal converter; avoid redundant mixed representations.
- Domain interfaces return domain models, not Room RateEntity. Repository owns common persistence for all source implementations.
- Clock and ZoneId supply today; Period=[today-6,today] or[today-29,today]. Coverage includes actual range/count and unresolved request dates. Never shift windows to stale cache.
- PairObservation combines source/target rates on same effective date; sorting/deduplication precedes statistics.
- Empty has no metrics. SinglePoint has min/max/mean, unavailable delta/percent/trend. Unknown trend differs from NoChange. Full trend uses sign(delta), not a threshold.
- Ready UI data includes typed cached content, refreshing flag and recoverable warning. Error holds no usable data; do not use Any or undeclared generic T.
- Success metadata: sourceName, effectiveDate, fetchedAt, origin remote/cache and warnings. SameCurrency preserves input and has no official lookup date.
- Use exact decimal strings and DECIMAL128 for non-terminating divisions, monetary display2 HALF_UP, percentage4, reference rate6. Input length<=64, plain decimal only, no grouping/exponents; existing maximum1B retained.

## Implemented analytics mapping · Phase2

GetAnalytics selects actual snapshots inside AnalyticsPeriod, resolves duplicate effective dates by newest fetchedAt and joins both currency rates on that date. PairObservation feeds StatisticsCalculator and the chart. Room Flow updates AnalyticsViewModel; no second network loader is created for analytics. A shared parameterized ViewModel/Screen serves both periods. Room entity schema remains version1; no cache clearing or destructive migration.
