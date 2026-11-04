package md.bnm.currencyconverter

import md.bnm.currencyconverter.domain.AmountValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class AmountValidatorTest {
    @Test fun rejectsInvalidAndNonFiniteAmounts() {
        listOf("", " ", "0", "0.00", "-2", "abc", "NaN", "Infinity", "1e3", "1,000.00", ".5", "1.2.3", "1".repeat(65), "1000000000.01").forEach {
            val result = AmountValidator.validate(it)
            assertNull(it, result.value); assertNotNull(it, result.error)
        }
    }
    @Test fun acceptsPositivePlainDecimals() {
        mapOf("100" to "100", "25,50" to "25.50", "0.0001" to "0.0001", "1000000000" to "1000000000", " 2.5 " to "2.5").forEach { (raw, expected) ->
            val result = AmountValidator.validate(raw)
            assertNull(result.error); assertEquals(BigDecimal(expected), result.value)
        }
    }
}
