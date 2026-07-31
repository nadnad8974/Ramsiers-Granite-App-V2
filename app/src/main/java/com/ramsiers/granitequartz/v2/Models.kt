package com.ramsiers.granitequartz.v2

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PageType(val label: String) {
    TEXT("Text"),
    NUMBER("Number"),
    CURRENCY("Currency"),
    DATE("Date"),
    YES_NO("Yes / No"),
    MULTIPLE_CHOICE("Multiple choice"),
    PRODUCT("Product selection"),
    PHOTO("Photo"),
    QR_SCAN("QR scan"),
    MEASUREMENT("Measurement"),
    SUMMARY("Summary")
}

@Serializable
data class Choice(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New choice",
    val price: Double = 0.0,
    val unit: String = "each",
    val imageUri: String = ""
)

@Serializable
data class FormPage(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New question",
    val type: PageType = PageType.TEXT,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val helpText: String = "",
    val unit: String = "",
    val price: Double = 0.0,
    val formula: String = "",
    val choices: List<Choice> = emptyList()
)

@Serializable
data class QuoteAnswer(
    val value: String = "",
    val quantity: Double = 1.0,
    val length: Double = 0.0,
    val width: Double = 0.0,
    val imageUri: String = ""
)

@Serializable
data class AppSetup(
    val version: Int = 3,
    val officeEmail: String = "",
    val pages: List<FormPage> = emptyList(),
    val answers: Map<String, QuoteAnswer> = emptyMap()
)
