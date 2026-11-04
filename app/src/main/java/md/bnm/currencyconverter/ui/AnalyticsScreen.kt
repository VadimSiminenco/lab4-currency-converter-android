package md.bnm.currencyconverter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.bnm.currencyconverter.domain.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun rateText(value: BigDecimal, digits: Int = 6): String = value.setScale(digits, RoundingMode.HALF_UP).toPlainString()
internal fun changeText(value: BigDecimal?, digits: Int = 6): String {
    if (value == null) return "Unavailable"
    val rounded = value.setScale(digits, RoundingMode.HALF_UP)
    val sign = if (value.signum() > 0) "+" else if (value.signum() < 0) "−" else ""
    if (value.signum() != 0 && rounded.signum() == 0) return sign + "<" + BigDecimal.ONE.movePointLeft(digits).toPlainString()
    return sign + rounded.abs().toPlainString()
}

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onSource: (Currency) -> Unit, onTarget: (Currency) -> Unit, onSwap: () -> Unit, onRefresh: () -> Unit,
    refreshing: Boolean = false, progress: Int = 0, refreshWarning: String? = null,
) {
    val days = state.period.days
    val data = state.data
    val points = data.result.observations
    val stats = data.result.statistics
    val unit = "${state.target} per 1 ${state.source}"
    val trendColor = when (stats?.trend) { Trend.RISING -> Pine; Trend.FALLING -> Color(0xFFA7463E); else -> Muted }
    LazyColumn(Modifier.fillMaxSize().testTag("analytics_${days}_screen"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Brand() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (days == 7) "A WEEK IN PERSPECTIVE" else "THE BIGGER PICTURE", color = Pine, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Your $days-day view", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("${state.period.start.format(dateFormat)} — ${state.period.end.format(dateFormat)}", color = Muted, fontSize = 12.sp, modifier = Modifier.testTag("analytics_period"))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CurrencyPicker("From", state.source, onSource, "analytics_source", Modifier.weight(1f))
                        CurrencyPicker("To", state.target, onTarget, "analytics_target", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onSwap, modifier = Modifier.testTag("analytics_swap")) { Text("⇄ Swap pair") }
                        TextButton(onClick = onRefresh, enabled = !refreshing, modifier = Modifier.testTag("analytics_refresh")) { Text("Refresh data") }
                    }
                    if (refreshing) {
                        LinearProgressIndicator(progress = { progress.coerceIn(0,30) / 30f }, modifier = Modifier.fillMaxWidth(), color = Pine, trackColor = Mist)
                        Text("Updating history · $progress / 30 dates", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        state.error?.let { item { Notice(it, true, "analytics_error") } }
        if (refreshWarning != null) item { Notice("History refresh: $refreshWarning Showing available saved observations with their actual dates.", false, "analytics_cache_warning") }
        if (points.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Mist), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (state.reading || refreshing) "Preparing your perspective…" else "No observations in this period", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("analytics_empty"))
                        Text(if (state.reading || refreshing) "Saved dates and newly received rates will appear here as they become available." else "Connect and refresh to load this pair. Older saved dates never move the requested window into the past.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                        if (!refreshing) OutlinedButton(onClick = onRefresh, modifier = Modifier.testTag("analytics_retry")) { Text("Retry refresh") }
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp), modifier = Modifier.testTag("analytics_chart_card")) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("${state.source} / ${state.target}", color = Pine, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(rateText(points.last().rate), color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("analytics_latest"))
                        Text("$unit · latest observation ${points.last().date.format(dateFormat)}", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("analytics_unit"))
                        RateLineChart(points, state.period, unit)
                        Text("${points.size} observations across $days calendar days", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("analytics_coverage"))
                        Text("Y-axis focuses on the observed range. Straight lines join available dates; no missing rates are estimated.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }
            if (points.size == 1) item { Notice("Only one observation is available. Minimum, maximum and average are known; change and trend cannot be determined.", false, "analytics_single_point") }
            if (stats != null) {
                item { Text("THE PERIOD, AT A GLANCE", color = Pine, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Minimum", rateText(stats.min), unit, "stat_min", Modifier.weight(1f))
                    StatCard("Maximum", rateText(stats.max), unit, "stat_max", Modifier.weight(1f))
                } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Average", rateText(stats.mean), unit, "stat_mean", Modifier.weight(1f))
                    StatCard("Absolute change", changeText(stats.delta), "${state.target} per ${state.source}", "stat_delta", Modifier.weight(1f), trendColor)
                } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Period change", stats.percent?.let { changeText(it,4) + "%" } ?: "Unavailable", "First to last observation", "stat_percent", Modifier.weight(1f), trendColor)
                    StatCard("Direction", stats.trend?.label ?: "Unavailable", "Uses unrounded change", "stat_trend", Modifier.weight(1f), trendColor)
                } }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("analytics_metadata")) {
                Text("National Bank of Moldova · Official reference rates", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag("analytics_source_name"))
                if (points.isNotEmpty()) Text("Observed: ${points.first().date.format(dateFormat)} — ${points.last().date.format(dateFormat)}", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("analytics_actual_dates"))
                data.fetchedAt?.let { time -> Text("Last received ${time.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm",Locale.ENGLISH))}", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("analytics_updated")) }
                if (data.unresolvedRequests > 0) Text("Latest refresh: ${data.unresolvedRequests} dates are unconfirmed or failed. Saved observations above are retained.", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("analytics_unresolved"))
                if (data.missingPairDates > 0) Text("Snapshots missing this pair: ${data.missingPairDates}.", color = Muted, fontSize = 11.sp)
                Text("Statistics use actual available observations, not invented values for missing dates. Reference rates exclude transaction fees.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, note: String, tag: String, modifier: Modifier, valueColor: Color = Ink) {
    Card(modifier = modifier.heightIn(min = 112.dp).testTag(tag), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Muted, fontSize = 11.sp)
            Text(value, color = valueColor, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(note, color = Muted, fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
}
