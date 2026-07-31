package com.ramsiers.granitequartz.v2

import java.text.NumberFormat

object QuoteMath {
    fun squareFeet(answer: QuoteAnswer): Double =
        (answer.length * answer.width / 144.0) * answer.quantity.coerceAtLeast(0.0)

    fun pageTotal(page: FormPage, answer: QuoteAnswer?): Double {
        if (answer == null) return 0.0
        val selectedPrice = page.choices.firstOrNull { it.id == answer.value }?.price ?: 0.0
        if (page.formula.isNotBlank()) {
            val variables = mapOf(
                "value" to (answer.value.toDoubleOrNull() ?: 0.0),
                "quantity" to answer.quantity,
                "length" to answer.length,
                "width" to answer.width,
                "squareFeet" to squareFeet(answer),
                "price" to page.price,
                "choicePrice" to selectedPrice
            )
            FormulaEvaluator.evaluate(page.formula, variables)?.let { return it }
        }
        return when (page.type) {
            PageType.MEASUREMENT -> squareFeet(answer) * page.price
            PageType.PRODUCT, PageType.MULTIPLE_CHOICE -> {
                selectedPrice * answer.quantity.coerceAtLeast(0.0)
            }
            PageType.YES_NO -> if (answer.value.equals("Yes", true)) page.price else 0.0
            PageType.NUMBER -> answer.value.toDoubleOrNull()?.times(page.price) ?: 0.0
            PageType.CURRENCY -> answer.value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    fun total(pages: List<FormPage>, answers: Map<String, QuoteAnswer>): Double =
        pages.filter { it.enabled }.sumOf { pageTotal(it, answers[it.id]) }

    fun money(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

    fun isAnswered(page: FormPage, answer: QuoteAnswer?): Boolean {
        if (!page.required) return true
        if (page.type == PageType.SUMMARY) return true
        if (answer == null) return false
        return when (page.type) {
            PageType.MEASUREMENT -> answer.length > 0 && answer.width > 0
            PageType.PHOTO -> answer.imageUri.isNotBlank()
            else -> answer.value.isNotBlank()
        }
    }
}

internal object FormulaEvaluator {
    fun evaluate(expression: String, variables: Map<String, Double>): Double? = runCatching {
        Parser(expression, variables).parse()
    }.getOrNull()?.takeIf { it.isFinite() }

    private class Parser(
        private val source: String,
        private val variables: Map<String, Double>
    ) {
        private var position = 0

        fun parse(): Double {
            val result = expression()
            skipSpaces()
            require(position == source.length) { "Unexpected formula text" }
            return result
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                skipSpaces()
                value = when {
                    take('+') -> value + term()
                    take('-') -> value - term()
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = factor()
            while (true) {
                skipSpaces()
                value = when {
                    take('*') -> value * factor()
                    take('/') -> value / factor()
                    else -> return value
                }
            }
        }

        private fun factor(): Double {
            skipSpaces()
            if (take('-')) return -factor()
            if (take('(')) {
                val value = expression()
                skipSpaces()
                require(take(')')) { "Missing )" }
                return value
            }
            if (position < source.length && (source[position].isLetter() || source[position] == '_')) {
                val start = position
                while (position < source.length &&
                    (source[position].isLetterOrDigit() || source[position] == '_')
                ) position++
                return variables[source.substring(start, position)]
                    ?: error("Unknown variable")
            }
            val start = position
            while (position < source.length &&
                (source[position].isDigit() || source[position] == '.')
            ) position++
            require(start != position) { "Expected number" }
            return source.substring(start, position).toDouble()
        }

        private fun skipSpaces() {
            while (position < source.length && source[position].isWhitespace()) position++
        }

        private fun take(character: Char): Boolean {
            if (position < source.length && source[position] == character) {
                position++
                return true
            }
            return false
        }
    }
}
