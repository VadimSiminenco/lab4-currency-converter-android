package md.bnm.currencyconverter

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import md.bnm.currencyconverter.domain.Currency
import md.bnm.currencyconverter.ui.ConverterScreen
import md.bnm.currencyconverter.ui.ConverterUiState
import md.bnm.currencyconverter.ui.CurrencyApp
import org.junit.Rule
import org.junit.Test

class ConverterScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invalidInputDisablesConvertAndShowsError() {
        compose.setContent {
            MaterialTheme {
                ConverterScreen(ConverterUiState(amount = "0", amountError = "Amount must be greater than zero."), {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("convert_button").assertIsNotEnabled()
        compose.onNodeWithTag("validation_error").assertTextContains("greater than zero", substring = true)
    }

    @Test fun validInputExposesCurrencyControlsAndConversionAction() {
        var clicked = false
        compose.setContent {
            MaterialTheme {
                ConverterScreen(ConverterUiState(source = Currency.USD, target = Currency.MDL), {}, {}, {}, {}, { clicked = true }, {})
            }
        }
        compose.onNodeWithTag("source_currency").assertExists()
        compose.onNodeWithTag("target_currency").assertExists()
        compose.onNodeWithTag("convert_button").assertIsEnabled().performClick()
        compose.runOnIdle { check(clicked) }
    }

    @Test fun threeNavigationDestinationsRemainReachable() {
        compose.setContent { CurrencyApp(ConverterUiState(), {}, {}, {}, {}, {}, {}) }
        compose.onNodeWithTag("nav_week").performClick()
        compose.onNodeWithText("Your 7-day view").assertIsDisplayed()
        compose.onNodeWithTag("nav_month").performClick()
        compose.onNodeWithText("Your 30-day view").assertIsDisplayed()
        compose.onNodeWithTag("nav_converter").performClick()
        compose.onNodeWithTag("converter_screen").assertExists()
    }
}
