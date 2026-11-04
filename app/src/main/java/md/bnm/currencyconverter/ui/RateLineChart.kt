package md.bnm.currencyconverter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import md.bnm.currencyconverter.domain.AnalyticsPeriod
import md.bnm.currencyconverter.domain.PairObservation
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/** Double conversion is confined to chart rendering. Domain statistics remain BigDecimal. */
@Composable
fun RateLineChart(observations: List<PairObservation>, period: AnalyticsPeriod, unit: String) {
    val description = "$unit. ${observations.size} observations. ${observations.first().date} to ${observations.last().date}."
    if (observations.size == 1) {
        Column(Modifier.testTag("rate_chart").semantics { contentDescription = description; stateDescription = "Ready" }) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                drawLine(Muted.copy(alpha = .2f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
                drawCircle(Pine, 6.dp.toPx(), Offset(size.width / 2, size.height / 2))
            }
            Text("${observations.single().date.format(dateFormat)} · ${observations.single().rate.toPlainString()}", color = Muted, fontSize = 11.sp)
            Text("One observed point; no line or change can be inferred.", color = Muted, fontSize = 11.sp)
        }
        return
    }
    val producer = remember(period, unit) { CartesianChartModelProducer() }
    var ready by remember(period, unit) { mutableStateOf(false) }
    val xs = remember(observations, period) { observations.map { ChronoUnit.DAYS.between(period.start, it.date).toDouble() } }
    val ys = remember(observations) { observations.map { it.rate.toDouble() } }
    LaunchedEffect(producer, observations) {
        ready = false
        producer.runTransaction { lineSeries { series(xs, ys) } }
        ready = true
    }
    val minimum = ys.min()
    val maximum = ys.max()
    val pad = max((maximum - minimum) * .2, max(abs(maximum) * .0005, .000001))
    val range = remember(period, minimum, maximum) {
        CartesianLayerRangeProvider.fixed(minX = 0.0, maxX = (period.days - 1).toDouble(), minY = max(0.0, minimum - pad), maxY = maximum + pad)
    }
    val point = LineCartesianLayer.point(rememberShapeComponent(fill(Pine)), size = 5.dp)
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(Pine)),
        areaFill = LineCartesianLayer.AreaFill.single(fill(Pine.copy(alpha = .08f))),
        pointProvider = LineCartesianLayer.PointProvider.single(point),
    )
    val dateFormatter = remember(period) { CartesianValueFormatter { _, value, _ ->
        period.start.plusDays(value.roundToLong()).format(DateTimeFormatter.ofPattern(if (period.days == 30) "dd\nMMM" else "dd MMM", Locale.ENGLISH))
    } }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(listOf(line)), rangeProvider = range),
            startAxis = VerticalAxis.rememberStart(label = rememberAxisLabelComponent(color = Muted, textSize = 10.sp),
                itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { 4 }) },
                valueFormatter = CartesianValueFormatter { _, value, _ -> String.format(Locale.US, "%.4f", value) }),
            bottomAxis = HorizontalAxis.rememberBottom(label = rememberAxisLabelComponent(color = Muted, textSize = 10.sp, lineCount = 2), valueFormatter = dateFormatter,
                itemPlacer = remember(period.days) { HorizontalAxis.ItemPlacer.aligned(spacing = { if (period.days == 7) 2 else 8 }) }),
            getXStep = { 1.0 },
        ),
        modelProducer = producer,
        modifier = Modifier.fillMaxWidth().height(215.dp).testTag("rate_chart").semantics { contentDescription = description; stateDescription = if (ready) "Ready" else "Loading" },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        animationSpec = null, animateIn = false,
    )
}
