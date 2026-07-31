package com.ramsiers.granitequartz.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultsTest {
    @Test
    fun defaultQuotePagesMatchRequestedWorkflow() {
        val pages = Defaults.setup().pages
        val titles = pages.map { it.title.lowercase() }

        assertFalse(titles.any { it.contains("email should receive") || it == "office email" })
        assertFalse(titles.any { it.contains("square footage price") })

        val sinkPage = pages.first { it.title == "Choose a sink" }
        assertEquals(
            listOf(
                "Rectangle bathroom sink",
                "Oval bathroom sink",
                "Another sink — I want to pick my own"
            ),
            sinkPage.choices.map { it.name }
        )
        assertTrue(sinkPage.choices[0].imageUri.contains("bathroom_sink_rectangle"))
        assertTrue(sinkPage.choices[1].imageUri.contains("bathroom_sink_oval"))

        val otherProjects = pages.indexOfFirst {
            it.title == "Are there any other projects you would like a quote for?"
        }
        val review = pages.indexOfFirst { it.type == PageType.SUMMARY }
        assertEquals(review - 1, otherProjects)
    }
}
