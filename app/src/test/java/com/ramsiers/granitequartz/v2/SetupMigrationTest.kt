package com.ramsiers.granitequartz.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SetupMigrationTest {
    @Test
    fun addsWhiteAndBiscuitToExistingVanitySinkChoices() {
        val rectangle = Choice(id = "rectangle", name = "Rectangle bathroom sink")
        val oval = Choice(id = "oval", name = "Oval bathroom sink")
        val other = Choice(id = "other", name = "Another sink")
        val setup = AppSetup(
            version = 1,
            pages = listOf(
                FormPage(
                    id = "sink-page",
                    title = "My edited vanity sink page",
                    type = PageType.PRODUCT,
                    choices = listOf(rectangle, oval, other)
                )
            ),
            answers = mapOf("sink-page" to QuoteAnswer(value = "rectangle"))
        )

        val migrated = migrateSetup(setup)
        val choices = migrated.pages.single().choices

        assertEquals(3, migrated.version)
        assertEquals(
            listOf(
                "Rectangle vanity sink - White",
                "Rectangle vanity sink - Biscuit",
                "Oval vanity sink - White",
                "Oval vanity sink - Biscuit",
                "Another sink"
            ),
            choices.map { it.name }
        )
        assertEquals("rectangle", choices[0].id)
        assertNotEquals("rectangle", choices[1].id)
        assertEquals("oval", choices[2].id)
        assertEquals("rectangle", migrated.answers.getValue("sink-page").value)
    }

    @Test
    fun migrationDoesNotDuplicateExistingColorChoices() {
        val setup = Defaults.setup()
        val migrated = migrateSetup(migrateSetup(setup))

        assertEquals(
            setup.pages.first { it.title == "Choose a sink" }.choices.map { it.name },
            migrated.pages.first { it.title == "Choose a sink" }.choices.map { it.name }
        )
    }

    @Test
    fun removesRequestedPagesAndMovesProjectNotesBeforeSummary() {
        val pages = (1..7).map { FormPage(id = "page-$it", title = "Page $it") } +
            listOf(
                FormPage(id = "price", title = "What is the square footage price?"),
                FormPage(id = "notes", title = "Notes"),
                FormPage(id = "review", title = "Review", type = PageType.SUMMARY)
            )
        val setup = AppSetup(
            version = 2,
            pages = pages,
            answers = mapOf("notes" to QuoteAnswer(value = "Keep these notes"))
        )

        val migrated = migrateSetup(setup)

        assertEquals(3, migrated.version)
        assertFalse(migrated.pages.any { it.id == "page-6" })
        assertFalse(migrated.pages.any {
            it.title.contains("square footage price", ignoreCase = true)
        })
        assertEquals("Project notes", migrated.pages[migrated.pages.lastIndex - 1].title)
        assertEquals("notes", migrated.pages[migrated.pages.lastIndex - 1].id)
        assertEquals("Keep these notes", migrated.answers.getValue("notes").value)
    }
}
