package com.ramsiers.granitequartz.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteMathTest {
    @Test
    fun measurementUsesInchesAndQuantity() {
        val answer = QuoteAnswer(length = 120.0, width = 25.5, quantity = 2.0)
        assertEquals(42.5, QuoteMath.squareFeet(answer), 0.0001)
    }

    @Test
    fun productUsesSelectedPriceAndQuantity() {
        val selected = Choice(name = "Basket drain", price = 35.0)
        val page = FormPage(type = PageType.PRODUCT, choices = listOf(selected))
        val answer = QuoteAnswer(value = selected.id, quantity = 3.0)
        assertEquals(105.0, QuoteMath.pageTotal(page, answer), 0.0001)
    }

    @Test
    fun extraCutoutsUsePageRate() {
        val page = FormPage(type = PageType.NUMBER, price = 100.0)
        assertEquals(200.0, QuoteMath.pageTotal(page, QuoteAnswer(value = "2")), 0.0001)
    }

    @Test
    fun customFormulaUsesEditableVariables() {
        val selected = Choice(price = 10.0)
        val page = FormPage(
            type = PageType.PRODUCT,
            price = 5.0,
            formula = "(choicePrice + price) * quantity",
            choices = listOf(selected)
        )
        assertEquals(
            45.0,
            QuoteMath.pageTotal(page, QuoteAnswer(value = selected.id, quantity = 3.0)),
            0.0001
        )
    }

    @Test
    fun requiredMeasurementNeedsBothDimensions() {
        val page = FormPage(type = PageType.MEASUREMENT, required = true)
        assertFalse(QuoteMath.isAnswered(page, QuoteAnswer(length = 100.0)))
        assertTrue(QuoteMath.isAnswered(page, QuoteAnswer(length = 100.0, width = 25.0)))
    }
}
