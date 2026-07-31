package com.ramsiers.granitequartz.v2

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class AppScreen { QUOTE, EDITOR }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Color(0xFFE36B19),
                    secondary = Color(0xFF333333)
                )
            ) {
                RamsiersApp()
            }
        }
    }
}

@Composable
private fun RamsiersApp() {
    val context = LocalContext.current
    val storage = remember { AppStorage(context) }
    var setup by remember { mutableStateOf(storage.load()) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.QUOTE) }
    val undo = remember { mutableStateListOf<List<FormPage>>() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(storage.export(setup))
            }
            scope.launch { snackbar.showSnackbar("Complete app setup exported") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            runCatching {
                val raw = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    reader.readText()
                } ?: error("The selected file was empty")
                storage.import(raw)
            }.onSuccess { imported ->
                undo.add(setup.pages)
                setup = imported
                scope.launch { snackbar.showSnackbar("Setup and current quote imported") }
            }.onFailure {
                scope.launch { snackbar.showSnackbar("That JSON backup could not be imported") }
            }
        }
    }

    LaunchedEffect(setup) { storage.save(setup) }

    fun changePages(newPages: List<FormPage>) {
        if (newPages != setup.pages) {
            undo.add(setup.pages)
            if (undo.size > 30) undo.removeAt(0)
            setup = setup.copy(pages = newPages)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                AppScreen.QUOTE -> QuoteScreen(
                    setup = setup,
                    onAnswer = { id, answer ->
                        setup = setup.copy(answers = setup.answers + (id to answer))
                    },
                    onOpenEditor = { screen = AppScreen.EDITOR }
                )
                AppScreen.EDITOR -> EditorScreen(
                    pages = setup.pages,
                    canUndo = undo.isNotEmpty(),
                    onPagesChange = ::changePages,
                    onUndo = {
                        if (undo.isNotEmpty()) {
                            setup = setup.copy(pages = undo.removeAt(undo.lastIndex))
                        }
                    },
                    onRestore = {
                        changePages(Defaults.setup().pages)
                        setup = setup.copy(answers = emptyMap())
                    },
                    onExport = { exportLauncher.launch("ramsiers-v2-complete-setup.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                    onBack = { screen = AppScreen.QUOTE }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteScreen(
    setup: AppSetup,
    onAnswer: (String, QuoteAnswer) -> Unit,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    val pages = setup.pages.filter { it.enabled }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val page = pages.getOrNull(index)
    LaunchedEffect(pages.size) {
        if (index > pages.lastIndex) index = pages.lastIndex.coerceAtLeast(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RAMSIER'S GRANITE & QUARTZ V2") },
                actions = {
                    IconButton(onClick = onOpenEditor) {
                        Icon(Icons.Default.Edit, "Open Advanced Live Editor")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (page == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No pages are enabled.")
                Button(onClick = onOpenEditor) { Text("Open Advanced Live Editor") }
            }
            return@Scaffold
        }

        val answer = setup.answers[page.id] ?: QuoteAnswer()
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding()
        ) {
            Text(
                "Question ${index + 1} of ${pages.size}",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(page.title, style = MaterialTheme.typography.headlineSmall)
                    if (page.required) {
                        Text("Required", color = MaterialTheme.colorScheme.primary)
                    }
                    if (page.helpText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(page.helpText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(18.dp))
                    AnswerField(
                        page = page,
                        answer = answer,
                        allPages = pages,
                        allAnswers = setup.answers,
                        onChange = { onAnswer(page.id, it) }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { index = (index - 1).coerceAtLeast(0) },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = {
                        if (!QuoteMath.isAnswered(page, setup.answers[page.id])) {
                            scope.launch { snackbar.showSnackbar("Please answer this question") }
                        } else if (index < pages.lastIndex) {
                            index++
                        } else {
                            scope.launch { snackbar.showSnackbar("Quote saved automatically") }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (index == pages.lastIndex) "Save" else "Next")
                }
            }
        }
    }
}

@Composable
private fun AnswerField(
    page: FormPage,
    answer: QuoteAnswer,
    allPages: List<FormPage>,
    allAnswers: Map<String, QuoteAnswer>,
    onChange: (QuoteAnswer) -> Unit
) {
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onChange(answer.copy(imageUri = it.toString()))
        }
    }

    when (page.type) {
        PageType.TEXT, PageType.DATE -> {
            if (page.isAddressPage()) {
                AddressAnswerField(
                    value = answer.value,
                    onValueChange = { onChange(answer.copy(value = it)) }
                )
            } else {
                OutlinedTextField(
                    value = answer.value,
                    onValueChange = { onChange(answer.copy(value = it)) },
                    label = { Text(if (page.type == PageType.DATE) "Date" else "Answer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        PageType.NUMBER, PageType.CURRENCY -> OutlinedTextField(
            value = answer.value,
            onValueChange = { onChange(answer.copy(value = it)) },
            label = { Text(page.unit.ifBlank { if (page.type == PageType.CURRENCY) "Amount" else "Number" }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        PageType.YES_NO -> ChoiceRadios(
            values = listOf("Yes" to "Yes", "No" to "No"),
            selected = answer.value,
            onSelected = { onChange(answer.copy(value = it)) }
        )
        PageType.MULTIPLE_CHOICE, PageType.PRODUCT -> {
            ChoiceRadios(
                values = page.choices.map { it.id to buildString {
                    append(it.name)
                    if (it.price != 0.0) append(" — ${QuoteMath.money(it.price)} ${it.unit}")
                } },
                selected = answer.value,
                images = page.choices.associate { it.id to it.imageUri },
                onSelected = { onChange(answer.copy(value = it)) }
            )
            if (page.type == PageType.PRODUCT) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = answer.quantity.toEditable(),
                    onValueChange = { onChange(answer.copy(quantity = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Quantity / linear feet") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        PageType.PHOTO -> {
            if (answer.imageUri.isNotBlank()) {
                AsyncImage(
                    model = answer.imageUri,
                    contentDescription = "Selected kitchen or countertop",
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }
            Button(onClick = { photoLauncher.launch(arrayOf("image/*")) }) {
                Text(if (answer.imageUri.isBlank()) "Choose photo" else "Replace photo")
            }
        }
        PageType.QR_SCAN -> {
            OutlinedTextField(
                value = answer.value,
                onValueChange = { onChange(answer.copy(value = it)) },
                label = { Text("Slab QR code or manual entry") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX)
                    .enableAutoZoom()
                    .build()
                GmsBarcodeScanning.getClient(context, options).startScan()
                    .addOnSuccessListener { code -> onChange(answer.copy(value = code.rawValue.orEmpty())) }
            }) {
                Text("Scan MSI slab code")
            }
        }
        PageType.MEASUREMENT -> {
            DecimalField("Length (inches)", answer.length) {
                onChange(answer.copy(length = it))
            }
            DecimalField("Width (inches)", answer.width) {
                onChange(answer.copy(width = it))
            }
            DecimalField("Quantity", answer.quantity) {
                onChange(answer.copy(quantity = it))
            }
            Text(
                "Square footage: ${"%.2f".format(QuoteMath.squareFeet(answer))}",
                fontWeight = FontWeight.Bold
            )
            if (page.price > 0) {
                Text("Section price: ${QuoteMath.money(QuoteMath.pageTotal(page, answer))}")
            }
        }
        PageType.SUMMARY -> QuoteSummary(allPages, allAnswers)
    }
}

internal fun FormPage.isAddressPage(): Boolean =
    type == PageType.TEXT && title.contains("address", ignoreCase = true)

@Composable
private fun AddressAnswerField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedValue by remember { mutableStateOf("") }

    LaunchedEffect(value) {
        if (!shouldSearchAddressSuggestions(value) || value == selectedValue) {
            suggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(450)
        suggestions = findAddressSuggestions(context, value)
        isSearching = false
    }

    OutlinedTextField(
        value = value,
        onValueChange = {
            selectedValue = ""
            onValueChange(it)
        },
        label = { Text("Type the project address") },
        modifier = Modifier.fillMaxWidth()
    )
    if (isSearching) {
        Spacer(Modifier.height(6.dp))
        Text("Finding addresses...", style = MaterialTheme.typography.bodySmall)
    }
    if (suggestions.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                suggestions.forEach { suggestion ->
                    TextButton(
                        onClick = {
                            selectedValue = suggestion
                            suggestions = emptyList()
                            onValueChange(suggestion)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(suggestion, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = { openAddressInMaps(context, value) },
        enabled = value.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open in Google Maps")
    }
}

internal fun shouldSearchAddressSuggestions(query: String): Boolean =
    query.trim().length >= 4

private suspend fun findAddressSuggestions(
    context: android.content.Context,
    query: String
): List<String> = withContext(Dispatchers.IO) {
    runCatching {
        Geocoder(context, Locale.US)
            .getFromLocationName(query, 5)
            .orEmpty()
            .mapNotNull { it.toSuggestionText() }
            .distinct()
    }.getOrDefault(emptyList())
}

private fun Address.toSuggestionText(): String? {
    val direct = getAddressLine(0)?.trim()
    if (!direct.isNullOrBlank()) return direct

    return listOfNotNull(
        thoroughfare,
        locality,
        adminArea,
        postalCode
    ).joinToString(", ").takeIf { it.isNotBlank() }
}

private fun openAddressInMaps(context: android.content.Context, address: String) {
    val encoded = Uri.encode(address)
    val googleMaps = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:0,0?q=$encoded")
    ).apply {
        setPackage("com.google.android.apps.maps")
    }
    runCatching {
        context.startActivity(googleMaps)
    }.onFailure {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
            )
        )
    }
}

@Composable
private fun ChoiceRadios(
    values: List<Pair<String, String>>,
    selected: String,
    images: Map<String, String> = emptyMap(),
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { (id, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected == id, onClick = { onSelected(id) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == id, onClick = { onSelected(id) })
                val image = images[id].orEmpty()
                if (image.isNotBlank()) {
                    AsyncImage(
                        model = image,
                        contentDescription = label,
                        modifier = Modifier.size(72.dp).padding(end = 8.dp)
                    )
                }
                Text(label)
            }
        }
    }
}

@Composable
private fun DecimalField(label: String, value: Double, onChange: (Double) -> Unit) {
    OutlinedTextField(
        value = value.toEditable(),
        onValueChange = { onChange(it.toDoubleOrNull() ?: 0.0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

private fun Double.toEditable(): String =
    if (this == 0.0) "" else if (this % 1.0 == 0.0) toLong().toString() else toString()

@Composable
private fun QuoteSummary(pages: List<FormPage>, answers: Map<String, QuoteAnswer>) {
    val context = LocalContext.current
    val lines = pages.filter { it.type != PageType.SUMMARY }.mapNotNull { page ->
        val answer = answers[page.id] ?: return@mapNotNull null
        val shown = when (page.type) {
            PageType.PRODUCT, PageType.MULTIPLE_CHOICE ->
                page.choices.firstOrNull { it.id == answer.value }?.name.orEmpty()
            PageType.MEASUREMENT -> "${"%.2f".format(QuoteMath.squareFeet(answer))} sq ft"
            PageType.PHOTO -> if (answer.imageUri.isNotBlank()) "Photo attached in app" else ""
            else -> answer.value
        }
        if (shown.isBlank()) null else "${page.title}: $shown"
    }
    val total = QuoteMath.total(pages, answers)
    val officeEmail = pages.firstOrNull { it.title.equals("Office email", true) }
        ?.let { answers[it.id]?.value }.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { Text(it) }
        HorizontalDivider()
        Text("Estimated total: ${QuoteMath.money(total)}", style = MaterialTheme.typography.titleLarge)
        Button(onClick = {
            val body = (lines + "Estimated total: ${QuoteMath.money(total)}").joinToString("\n")
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(officeEmail).filter { it.isNotBlank() }.toTypedArray())
                        putExtra(Intent.EXTRA_SUBJECT, "RAMSIER'S countertop quote")
                        putExtra(Intent.EXTRA_TEXT, body)
                    },
                    "Send quote"
                )
            )
        }) {
            Icon(Icons.Default.Share, null)
            Text(" Send quote")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
    pages: List<FormPage>,
    canUndo: Boolean,
    onPagesChange: (List<FormPage>) -> Unit,
    onUndo: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf(pages.firstOrNull()?.id.orEmpty()) }
    var showRestore by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val selectedIndex = pages.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val selected = pages.getOrNull(selectedIndex)

    LaunchedEffect(pages, selectedId) {
        if (pages.isNotEmpty()) {
            if (pages.none { it.id == selectedId }) selectedId = pages.first().id
            listState.animateScrollToItem(pages.indexOfFirst { it.id == selectedId }.coerceAtLeast(0))
        }
    }

    if (showRestore) {
        AlertDialog(
            onDismissRequest = { showRestore = false },
            title = { Text("Restore default app?") },
            text = { Text("This replaces edited pages and clears the current quote. You can use Undo afterward.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestore = false
                    onRestore()
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestore = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Live Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to quote") }
                },
                actions = {
                    IconButton(onClick = onUndo, enabled = canUndo) { Icon(Icons.Default.Redo, "Undo") }
                    IconButton(onClick = onExport) { Icon(Icons.Default.Save, "Export JSON backup") }
                    IconButton(onClick = onImport) { Icon(Icons.Default.Home, "Import JSON backup") }
                    IconButton(onClick = { showRestore = true }) { Icon(Icons.Default.Restore, "Restore defaults") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pages (${pages.size})", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    val newPage = FormPage()
                    onPagesChange(pages + newPage)
                    selectedId = newPage.id
                }) {
                    Icon(Icons.Default.Add, null)
                    Text(" Add page")
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(pages, key = { _, item -> item.id }) { itemIndex, item ->
                    Card(
                        onClick = { selectedId = item.id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${itemIndex + 1}. ${item.title}", modifier = Modifier.weight(1f))
                            Switch(
                                checked = item.enabled,
                                onCheckedChange = { enabled ->
                                    onPagesChange(pages.updated(item.id) { it.copy(enabled = enabled) })
                                }
                            )
                            IconButton(
                                enabled = itemIndex > 0,
                                onClick = {
                                    onPagesChange(pages.moved(itemIndex, itemIndex - 1))
                                    selectedId = item.id
                                    scope.launch { listState.animateScrollToItem(itemIndex - 1) }
                                }
                            ) { Icon(Icons.Default.ArrowUpward, "Move up") }
                            IconButton(
                                enabled = itemIndex < pages.lastIndex,
                                onClick = {
                                    onPagesChange(pages.moved(itemIndex, itemIndex + 1))
                                    selectedId = item.id
                                    scope.launch { listState.animateScrollToItem(itemIndex + 1) }
                                }
                            ) { Icon(Icons.Default.ArrowDownward, "Move down") }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (selected != null) {
                EditorDetails(
                    page = selected,
                    previewPages = pages,
                    onChange = { changed ->
                        onPagesChange(pages.updated(selected.id) { changed })
                    },
                    onDuplicate = {
                        val copy = selected.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            title = "${selected.title} copy",
                            choices = selected.choices.map {
                                it.copy(id = java.util.UUID.randomUUID().toString())
                            }
                        )
                        onPagesChange(pages.toMutableList().apply { add(selectedIndex + 1, copy) })
                        selectedId = copy.id
                    },
                    onDelete = {
                        onPagesChange(pages.filterNot { it.id == selected.id })
                        selectedId = pages.getOrNull((selectedIndex - 1).coerceAtLeast(0))?.id.orEmpty()
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorDetails(
    page: FormPage,
    previewPages: List<FormPage>,
    onChange: (FormPage) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var typeMenu by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var imageChoiceId by remember { mutableStateOf<String?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = imageChoiceId
        if (uri != null && id != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onChange(page.copy(choices = page.choices.updatedChoice(id) { it.copy(imageUri = uri.toString()) }))
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(onClick = { preview = !preview }) {
                Text(if (preview) "Edit" else "Live preview")
            }
            OutlinedButton(onClick = onDuplicate) {
                Icon(Icons.Default.ContentCopy, null)
                Text(" Duplicate")
            }
            OutlinedButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null)
                Text(" Delete")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (preview) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(page.title, style = MaterialTheme.typography.headlineSmall)
                    if (page.helpText.isNotBlank()) Text(page.helpText)
                    Spacer(Modifier.height(12.dp))
                    AnswerField(
                        page = page,
                        answer = QuoteAnswer(),
                        allPages = previewPages,
                        allAnswers = emptyMap(),
                        onChange = {}
                    )
                }
            }
            return@Column
        }

        OutlinedTextField(
            value = page.title,
            onValueChange = { onChange(page.copy(title = it)) },
            label = { Text("Question / page name") },
            modifier = Modifier.fillMaxWidth()
        )
        Box {
            OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Page type: ${page.type.label}")
            }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                PageType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            typeMenu = false
                            onChange(page.copy(type = type))
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = page.helpText,
            onValueChange = { onChange(page.copy(helpText = it)) },
            label = { Text("Help text") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = page.required,
                onCheckedChange = { onChange(page.copy(required = it)) }
            )
            Text("Required")
            Spacer(Modifier.weight(1f))
            Text(if (page.enabled) "Enabled" else "Disabled")
            Switch(
                checked = page.enabled,
                onCheckedChange = { onChange(page.copy(enabled = it)) }
            )
        }
        OutlinedTextField(
            value = page.unit,
            onValueChange = { onChange(page.copy(unit = it)) },
            label = { Text("Unit (each, sq ft, linear foot, etc.)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = page.price.toEditable(),
            onValueChange = { onChange(page.copy(price = it.toDoubleOrNull() ?: 0.0)) },
            label = { Text("Page price / rate") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = page.formula,
            onValueChange = { onChange(page.copy(formula = it)) },
            label = { Text("Formula (optional)") },
            supportingText = { Text("Variables: value, quantity, length, width, squareFeet, price, choicePrice") },
            modifier = Modifier.fillMaxWidth()
        )
        if (page.type == PageType.MULTIPLE_CHOICE || page.type == PageType.PRODUCT) {
            Spacer(Modifier.height(10.dp))
            Text("Choices and products", style = MaterialTheme.typography.titleMedium)
            page.choices.forEach { choice ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        if (choice.imageUri.isNotBlank()) {
                            AsyncImage(
                                model = choice.imageUri,
                                contentDescription = choice.name,
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                        OutlinedTextField(
                            value = choice.name,
                            onValueChange = { name ->
                                onChange(page.copy(choices = page.choices.updatedChoice(choice.id) {
                                    it.copy(name = name)
                                }))
                            },
                            label = { Text("Choice name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = choice.price.toEditable(),
                                onValueChange = { price ->
                                    onChange(page.copy(choices = page.choices.updatedChoice(choice.id) {
                                        it.copy(price = price.toDoubleOrNull() ?: 0.0)
                                    }))
                                },
                                label = { Text("Price") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = choice.unit,
                                onValueChange = { unit ->
                                    onChange(page.copy(choices = page.choices.updatedChoice(choice.id) {
                                        it.copy(unit = unit)
                                    }))
                                },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row {
                            TextButton(onClick = {
                                imageChoiceId = choice.id
                                imageLauncher.launch(arrayOf("image/*"))
                            }) {
                                Text(if (choice.imageUri.isBlank()) "Add picture" else "Replace picture")
                            }
                            TextButton(onClick = {
                                onChange(page.copy(choices = page.choices.filterNot { it.id == choice.id }))
                            }) { Text("Delete choice") }
                        }
                    }
                }
            }
            Button(onClick = { onChange(page.copy(choices = page.choices + Choice())) }) {
                Icon(Icons.Default.Add, null)
                Text(" Add choice")
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}

private fun List<FormPage>.updated(id: String, transform: (FormPage) -> FormPage): List<FormPage> =
    map { if (it.id == id) transform(it) else it }

private fun List<Choice>.updatedChoice(id: String, transform: (Choice) -> Choice): List<Choice> =
    map { if (it.id == id) transform(it) else it }

private fun <T> List<T>.moved(from: Int, to: Int): List<T> =
    toMutableList().apply { add(to, removeAt(from)) }
