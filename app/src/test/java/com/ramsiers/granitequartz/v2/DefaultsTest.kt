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
                "Rectangle vanity sink - White",
                "Rectangle vanity sink - Biscuit",
                "Oval vanity sink - White",
                "Oval vanity sink - Biscuit",
                "Another sink — I want to pick my own"
            ),
            sinkPage.choices.map { it.name }
        )
        assertTrue(sinkPage.choices[0].imageUri.contains("bathroom_sink_rectangle"))
        assertTrue(sinkPage.choices[1].imageUri.contains("bathroom_sink_rectangle"))
        assertTrue(sinkPage.choices[2].imageUri.contains("bathroom_sink_oval"))
        assertTrue(sinkPage.choices[3].imageUri.contains("bathroom_sink_oval"))

        assertEquals(
            listOf(
                "Customer name",
                "Phone number",
                "Email address",
                "Job address",
                "Choose a sink",
                "Cooktop or extra cutouts",
                "Add a RAMSIER'S faucet?",
                "Basket drains",
                "Big sink grids",
                "Are the cabinets installed?",
                "Approximate cabinet installation date",
                "Would you like to buy cabinets from RAMSIER'S?",
                "Are there any other projects you would like a quote for?",
                "Countertop section measurements",
                "Add a kitchen or countertop photo",
                "Choose the edge detail",
                "MSI color or slab name",
                "Scan the MSI slab QR code",
                "Notes",
                "Review and send the quote"
            ),
            pages.map { it.title }
        )
        assertFalse(pages.first { it.title == "Countertop section measurements" }.enabled)
        assertFalse(titles.any { it == "stove opening" })
    }
}
