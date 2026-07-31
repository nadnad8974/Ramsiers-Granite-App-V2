package com.ramsiers.granitequartz.v2

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class AppStorage(context: Context) {
    private val preferences = context.getSharedPreferences("ramsiers_v2", Context.MODE_PRIVATE)
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppSetup {
        val saved = preferences.getString(KEY, null) ?: return Defaults.setup()
        return runCatching {
            migrateSetup(json.decodeFromString<AppSetup>(saved))
        }.getOrElse { Defaults.setup() }
    }

    fun save(setup: AppSetup) {
        preferences.edit().putString(KEY, json.encodeToString(setup)).apply()
    }

    fun export(setup: AppSetup): String = json.encodeToString(setup)

    fun import(raw: String): AppSetup = migrateSetup(json.decodeFromString(raw))

    private companion object {
        const val KEY = "complete_app_setup"
    }
}

internal fun migrateSetup(setup: AppSetup): AppSetup {
    var migratedPages = setup.pages.map { page ->
        val hasRectangle = page.choices.any {
            it.name.contains("Rectangle", ignoreCase = true)
        }
        val hasOval = page.choices.any {
            it.name.contains("Oval", ignoreCase = true)
        }
        if (!hasRectangle || !hasOval) return@map page

        val alreadyHasColors = page.choices.any {
            it.name.contains("Biscuit", ignoreCase = true)
        }
        if (alreadyHasColors) return@map page

        page.copy(
            choices = page.choices.flatMap { sink ->
                when {
                    sink.name.contains("Rectangle", ignoreCase = true) -> listOf(
                        sink.copy(name = "Rectangle vanity sink - White"),
                        sink.copy(
                            id = UUID.randomUUID().toString(),
                            name = "Rectangle vanity sink - Biscuit"
                        )
                    )
                    sink.name.contains("Oval", ignoreCase = true) -> listOf(
                        sink.copy(name = "Oval vanity sink - White"),
                        sink.copy(
                            id = UUID.randomUUID().toString(),
                            name = "Oval vanity sink - Biscuit"
                        )
                    )
                    else -> listOf(sink)
                }
            }
        )
    }
    if (setup.version < 3 && migratedPages.any { it.type == PageType.SUMMARY }) {
        migratedPages = migratedPages
            .filterIndexed { index, _ -> index != 5 }
            .filterNot {
                it.title.contains("square footage price", ignoreCase = true)
            }

        val notesPage = migratedPages.firstOrNull {
            it.title.equals("Notes", ignoreCase = true) ||
                it.title.equals("Project notes", ignoreCase = true)
        }?.copy(title = "Project notes") ?: FormPage(title = "Project notes")

        val withoutNotes = migratedPages.filterNot {
            it.title.equals("Notes", ignoreCase = true) ||
                it.title.equals("Project notes", ignoreCase = true)
        }
        val summaryIndex = withoutNotes.indexOfFirst { it.type == PageType.SUMMARY }
        migratedPages = withoutNotes.toMutableList().apply {
            add(if (summaryIndex >= 0) summaryIndex else size, notesPage)
        }
    }

    return setup.copy(version = 3, pages = migratedPages)
}
