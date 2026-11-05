package md.bnm.currencyconverter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import md.bnm.currencyconverter.domain.Currency
import md.bnm.currencyconverter.domain.ConversionResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val Pine = Color(0xFF087F67)
internal val Ink = Color(0xFF172C2A)
internal val Muted = Color(0xFF64746D)
private val Paper = Color(0xFFF4F6F1)
internal val Mist = Color(0xFFE8F2EB)
private val Gold = Color(0xFFFFF3D8)
internal val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private fun money(value: BigDecimal): String = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).apply {
    maximumFractionDigits = maxOf(2, value.scale())
}.format(value)

@Composable
fun CurrencyApp(
    state: ConverterUiState, onAmount: (String) -> Unit, onSource: (Currency) -> Unit,
    onTarget: (Currency) -> Unit, onSwap: () -> Unit, onConvert: () -> Unit, onRefresh: () -> Unit,
    analyticsContent: (@Composable (Int) -> Unit)? = null,
) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Pine, background = Paper, surface = Color.White,
        onBackground = Ink, onSurface = Ink, secondaryContainer = Mist, onSecondaryContainer = Pine)) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val current = entry?.destination?.route ?: "converter"
        Scaffold(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }, containerColor = Paper,
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    listOf(Triple("converter", "Convert", "⇄"), Triple("week", "7 days", "▥"), Triple("month", "30 days", "▤")).forEach { (route,label,symbol) ->
                        NavigationBarItem(selected = current == route, onClick = {
                            nav.navigate(route) { launchSingleTop = true; popUpTo(nav.graph.startDestinationId) { saveState = true }; restoreState = true }
                        }, icon = { Text(symbol, fontSize = 24.sp) }, label = { Text(label) }, modifier = Modifier.testTag("nav_$route"))
                    }
                }
            }) { padding ->
            NavHost(nav, startDestination = "converter", modifier = Modifier.padding(padding)) {
                composable("converter") { ConverterScreen(state, onAmount, onSource, onTarget, onSwap, onConvert, onRefresh) }
                composable("week") {
                    if (analyticsContent != null) analyticsContent(7)
                    else AnalyticsScreen(AnalyticsUiState(md.bnm.currencyconverter.domain.AnalyticsPeriod(7, java.time.LocalDate.now())), {}, {}, {}, onRefresh)
                }
                composable("month") {
                    if (analyticsContent != null) analyticsContent(30)
                    else AnalyticsScreen(AnalyticsUiState(md.bnm.currencyconverter.domain.AnalyticsPeriod(30, java.time.LocalDate.now())), {}, {}, {}, onRefresh)
                }
            }
        }
    }
}

@Composable
internal fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Pine), contentAlignment = Alignment.Center) {
            Text("⇄", color = Color.White, fontSize = 27.sp)
        }
        Column {
            Text("MOLDOVA", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Currency tools", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Surface(color = Mist, shape = RoundedCornerShape(20.dp)) {
            Text("BNM DATA", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Pine, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConverterScreen(
    state: ConverterUiState, onAmount: (String) -> Unit, onSource: (Currency) -> Unit,
    onTarget: (Currency) -> Unit, onSwap: () -> Unit, onConvert: () -> Unit, onRefresh: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().testTag("converter_screen"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Brand() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("EVERY CONVERSION, CLEAR.", color = Pine, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("A little more certainty.", color = Ink, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                Text("Official reference rates. A result you can understand.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("YOUR CONVERSION", color = Pine, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    OutlinedTextField(value = state.amount, onValueChange = onAmount, label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth().testTag("amount_input"), singleLine = true,
                        isError = state.amountError != null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp))
                    state.amountError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.testTag("validation_error"))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CurrencyPicker("From", state.source, onSource, "source_currency", Modifier.weight(1f))
                        CurrencyPicker("To", state.target, onTarget, "target_currency", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Latest available rate", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onSwap, modifier = Modifier.testTag("swap_button")) { Text("⇄ Swap") }
                    }
                    Button(onClick = onConvert, enabled = state.canConvert, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("convert_button"), shape = RoundedCornerShape(14.dp)) {
                        if (state.converting) { CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                        Text(if (state.converting) "Getting your rate…" else "Convert currency", fontWeight = FontWeight.SemiBold)
                    }
                    Text("MDL · EUR · USD · RON · GBP · UAH", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
        state.error?.let { message -> item { Notice(message, true, "service_error") } }
        state.result?.let { result -> item { ResultCard(result) } }
        if (state.result == null && state.error == null) item {
            Surface(color = Mist, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("↗", color = Pine, fontSize = 26.sp)
                    Column { Text("Your result will appear here", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Amount, source and the date actually used.", color = Muted, fontSize = 12.sp) }
                }
            }
        }
        item { SavedDataCard(state, onRefresh) }
        item { Text("Reference rates may differ from bank or exchange-office rates. Transaction fees are not included.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp) }
    }
}

@Composable
internal fun CurrencyPicker(label: String, currency: Currency, onSelect: (Currency) -> Unit, tag: String, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(5.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag(tag), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(12.dp)) {
                Text(currency.name, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("⌄")
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp).semantics { testTagsAsResourceId = true }) {
                Currency.entries.forEach { option -> DropdownMenuItem(text = { Text("${option.name} · ${option.displayName}") },
                    onClick = { onSelect(option); expanded = false }, modifier = Modifier.testTag("${tag}_${option.name}")) }
            }
        }
    }
}

@Composable
private fun ResultCard(result: ConversionResult) {
    val resolved = result.resolved
    Card(modifier = Modifier.fillMaxWidth().testTag("conversion_result"), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Mist)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val status = when { resolved == null -> "Same currency · no rate lookup"; resolved.fromCache -> "Saved snapshot · offline"; resolved.usedPreviousDate -> "Previous available date"; else -> "Official rate retrieved" }
            Text(status, color = Pine, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("YOU RECEIVE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text("${money(result.converted)} ${result.target}", color = Ink, fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("result_amount"))
            Text("${money(result.amount)} ${result.source} → ${result.target}", color = Muted, fontSize = 12.sp)
            HorizontalDivider(color = Color(0xFFCCDFD2))
            Text("1 ${result.source} = ${result.rate.setScale(6, RoundingMode.HALF_UP).toPlainString()} ${result.target}", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("exchange_rate"))
            if (resolved == null) Text("Your original amount is unchanged. No BNM rate was requested.", color = Muted, fontSize = 12.sp)
            else {
                Metadata("Effective date", resolved.snapshot.date.format(dateFormat), "rate_date")
                Metadata("Source", "National Bank of Moldova", "source_name")
                Text("Retrieved ${resolved.snapshot.fetchedAt.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ENGLISH))}", color = Muted, fontSize = 11.sp)
                if (resolved.fromCache) Notice("Live rates are unavailable. Using the saved snapshot from ${resolved.snapshot.date.format(dateFormat)}.", false, "offline_banner")
                else if (resolved.usedPreviousDate) Notice("No rate was available for today. The effective date above was used.", false, "fallback_notice")
            }
        }
    }
}
@Composable
private fun Metadata(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth().testTag(tag), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Muted, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
        Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}
@Composable
internal fun Notice(message: String, error: Boolean, tag: String) {
    Surface(color = if (error) Color(0xFFFFEBE9) else Gold, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Text(message, Modifier.padding(14.dp), color = if (error) Color(0xFF8C2923) else Color(0xFF735113), fontSize = 12.sp, lineHeight = 18.sp)
    }
}
@Composable
private fun SavedDataCard(state: ConverterUiState, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("A little continuity, too.", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(state.savedDate?.let { "Saved rates · ${it.format(dateFormat)}" } ?: "Valid rates are saved on this device.", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("saved_date"))
            }
            TextButton(onClick = onRefresh, enabled = !state.historyRefreshing, modifier = Modifier.testTag("refresh_button")) { Text("Refresh") }
        }
        if (state.historyRefreshing) {
            LinearProgressIndicator(progress = { state.historyProgress / 30f }, modifier = Modifier.fillMaxWidth(), color = Pine, trackColor = Mist)
            Text("Preparing history · ${state.historyProgress} / 30 dates", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("history_progress"))
        }
        state.historyWarning?.let { Notice(it, false, "history_warning") }
        Text("Saved data remains available after you close the app.", color = Muted, fontSize = 11.sp)
    }
}
