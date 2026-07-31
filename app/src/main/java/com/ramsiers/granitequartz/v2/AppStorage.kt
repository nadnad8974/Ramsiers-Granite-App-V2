package com.ramsiers.granitequartz.v2

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppStorage(context: Context) {
    private val preferences = context.getSharedPreferences("ramsiers_v2", Context.MODE_PRIVATE)
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppSetup {
        val saved = preferences.getString(KEY, null) ?: return Defaults.setup()
        return runCatching { json.decodeFromString<AppSetup>(saved) }.getOrElse { Defaults.setup() }
    }

    fun save(setup: AppSetup) {
        preferences.edit().putString(KEY, json.encodeToString(setup)).apply()
    }

    fun export(setup: AppSetup): String = json.encodeToString(setup)

    fun import(raw: String): AppSetup = json.decodeFromString(raw)

    private companion object {
        const val KEY = "complete_app_setup"
    }
}
