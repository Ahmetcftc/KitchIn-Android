package com.example.kitchin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReceiptScannerScreen()
                }
            }
        }
    }
}

@Composable
fun ReceiptScannerScreen() {
    val context = LocalContext.current
    var scannedText by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    // Manuel Ürün Ekleme Pop-up Dialog Durumları
    var showManualDialog by remember { mutableStateOf(false) }
    var manualProductName by remember { mutableStateOf("") }
    var manualAmount by remember { mutableStateOf("1") }
    var manualUnit by remember { mutableStateOf("ADET") }

    val confirmedProducts = remember { mutableStateListOf<ProductItem>() }
    val receiptScanner = remember { ReceiptScanner(context) }

    // Galeriden Görsel Seçici
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            receiptScanner.scanReceipt(
                imageUri = it,
                onSuccess = { resultText ->
                    scannedText = parseReceiptText(resultText)
                    isEditing = true
                },
                onFailure = { e ->
                    scannedText = "Hata oluştu: ${e.localizedMessage}"
                }
            )
        }
    }

    // Sesle Tanıma Result Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.getOrNull(0)

            if (!spokenText.isNullOrBlank()) {
                val formattedSpeech = formatSpeechToText(spokenText)
                scannedText = if (scannedText.isEmpty()) {
                    formattedSpeech
                } else {
                    "$scannedText\n$formattedSpeech"
                }
                isEditing = true
            }
        }
    }

    // Mikrofon İzni Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechToTextIntent(speechLauncher)
        } else {
            Toast.makeText(context, "Mikrofon izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "KitchIn - Mutfak Stok Ekleme",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // FİŞ TARAMA BUTONU
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📷 Galeriden Fiş Fotoğrafı Seç & Tara")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SESLE ÜRÜN EKLEME BUTONU
        OutlinedButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    startSpeechToTextIntent(speechLauncher)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎙️ Sesle Ürün Söyle & Ekle")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ELLE ÜRÜN EKLEME BUTONU
        OutlinedButton(
            onClick = { showManualDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✏️ Elle Ürün Ekle")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FİŞ / SES / MANUEL GİRİŞ DÜZENLEME EKRANI
        if (scannedText.isNotEmpty()) {
            Text(
                text = "Algılanan / Eklenen Ürünler (Düzenlenebilir):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = scannedText,
                onValueChange = { scannedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 250.dp),
                label = { Text("Eklenecek Ürün Listesi") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // REDDET BUTONU
                Button(
                    onClick = {
                        scannedText = ""
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Reddet")
                }

                // ONAYLA BUTONU
                Button(
                    onClick = {
                        val newItems = convertTextToProductList(scannedText)
                        confirmedProducts.addAll(newItems)
                        scannedText = ""
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Onayla & Listeye Ekle")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // EKLENEN STOK LİSTESİ GÖRÜNÜMÜ
        if (confirmedProducts.isNotEmpty()) {
            Text(
                text = "Mutfak Stoğundaki Ürünler (${confirmedProducts.size} Çeşit):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                confirmedProducts.forEachIndexed { index, product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${product.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // AZALTMA BUTONU (-)
                                IconButton(
                                    onClick = {
                                        val updatedQuantity = updateQuantity(product.quantity, isIncrease = false)
                                        confirmedProducts[index] = product.copy(quantity = updatedQuantity)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("-", style = MaterialTheme.typography.titleLarge)
                                }

                                Text(
                                    text = product.quantity,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // ARTIRMA BUTONU (+)
                                IconButton(
                                    onClick = {
                                        val updatedQuantity = updateQuantity(product.quantity, isIncrease = true)
                                        confirmedProducts[index] = product.copy(quantity = updatedQuantity)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("+", style = MaterialTheme.typography.titleMedium)
                                }

                                TextButton(
                                    onClick = { confirmedProducts.removeAt(index) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Sil", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val inventoryList = convertToInventoryItems(confirmedProducts)
                    Toast.makeText(
                        context,
                        "${inventoryList.size} çeşit ürün mutfak stoğuna eklendi!",
                        Toast.LENGTH_LONG
                    ).show()

                    confirmedProducts.clear()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ürünleri Mutfak Stoğuna Kaydet")
            }
        }
    }

    // MANUEL ÜRÜN EKLEME POP-UP (ALERTDIALOG)
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Elle Ürün Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualProductName,
                        onValueChange = { manualProductName = it },
                        label = { Text("Ürün Adı (Örn: Peynir)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualAmount,
                            onValueChange = { manualAmount = it },
                            label = { Text("Miktar") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = manualUnit,
                            onValueChange = { manualUnit = it },
                            label = { Text("Birim (ADET/KG/L)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualProductName.isNotBlank()) {
                            val formattedItem = "${manualProductName.uppercase(Locale("tr", "TR")).trim()}|${manualAmount.trim()} ${manualUnit.uppercase(Locale("tr", "TR")).trim()}"

                            scannedText = if (scannedText.isEmpty()) {
                                formattedItem
                            } else {
                                "$scannedText\n$formattedItem"
                            }
                            isEditing = true

                            manualProductName = ""
                            manualAmount = "1"
                            manualUnit = "ADET"
                            showManualDialog = false
                        }
                    }
                ) {
                    Text("Listeye Aktar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

// BİRİM VE MİKTAR DUYARLI ARTIRMA/AZALTMA FONKSİYONU
fun updateQuantity(quantityStr: String, isIncrease: Boolean): String {
    val parts = quantityStr.trim().split(" ")
    val rawAmount = parts.getOrNull(0)?.replace(",", ".")?.toDoubleOrNull() ?: 1.0
    val unit = parts.drop(1).joinToString(" ").ifEmpty { "ADET" }

    val decimalUnits = listOf("KG", "KİLO", "L", "LİTRE", "LT", "GR", "GRAM")
    val isDecimalUnit = decimalUnits.any { unit.uppercase(Locale("tr", "TR")).contains(it) }

    val step = if (isDecimalUnit) 0.1 else 1.0

    var newAmount = if (isIncrease) rawAmount + step else rawAmount - step
    if (newAmount < 0.1) newAmount = 0.1 // 0'ın altına düşmesin

    return if (isDecimalUnit) {
        String.format(Locale.US, "%.1f %s", newAmount, unit)
    } else {
        "${newAmount.toInt()} $unit"
    }
}

private fun startSpeechToTextIntent(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Eklenecek ürünleri söyleyin (Örn: Süt 2 litre, Yumurta 10 adet)")
    }
    launcher.launch(intent)
}

fun formatSpeechToText(spokenText: String): String {
    val upper = spokenText.uppercase(Locale("tr", "TR")).trim()
    val regex = Regex("""(\d+(?:[.,]\d+)?)\s*(KİLO|KG|GRAM|GR|ADET|AD|PAKET|PK|LİTRE|LT|L)""")
    val matches = regex.findAll(upper).toList()
    val resultLines = mutableListOf<String>()

    if (matches.isNotEmpty()) {
        for (i in matches.indices) {
            val match = matches[i]
            val amount = match.groupValues[1]
            var unit = match.groupValues[2]

            unit = when (unit) {
                "KİLO" -> "KG"
                "GRAM" -> "GR"
                "LİTRE", "LT" -> "L"
                "AD" -> "ADET"
                "PK" -> "PAKET"
                else -> unit
            }

            val startIndex = match.range.last + 1
            val endIndex = if (i + 1 < matches.size) matches[i + 1].range.first else upper.length
            var productName = upper.substring(startIndex, endIndex).trim()
            productName = productName.replace(Regex("""\b(VE|VE YA|İLE|SONRA)\b"""), "").trim()

            if (productName.isNotBlank()) {
                resultLines.add("$productName|$amount $unit")
            }
        }
    }

    if (resultLines.isEmpty()) {
        val cleanItems = upper.split(Regex("""\b(VE|VE YA|İLE|SONRA)\b"""))
        for (item in cleanItems) {
            val trimmed = item.trim()
            if (trimmed.isNotBlank()) {
                resultLines.add("$trimmed|1 ADET")
            }
        }
    }

    return resultLines.joinToString("\n")
}

data class ProductItem(
    val name: String,
    val quantity: String,
    val isAdded: Boolean = true
)

fun convertToInventoryItems(productItems: List<ProductItem>): List<InventoryItem> {
    return productItems.map { product ->
        val parts = product.quantity.split(" ")
        val amount = parts.getOrNull(0)?.toDoubleOrNull() ?: 1.0
        val unit = parts.drop(1).joinToString(" ").ifEmpty { "ADET" }

        InventoryItem(
            urunId = UUID.randomUUID().toString(),
            urunAdi = product.name,
            miktar = amount,
            birim = unit,
            sonTuketimTarihi = null
        )
    }
}

fun convertTextToProductList(scannedText: String): List<ProductItem> {
    val lines = scannedText.split("\n")
    val productList = mutableListOf<ProductItem>()

    for (line in lines) {
        val trimmedLine = line.trim()
        if (trimmedLine.isBlank()) continue

        val parts = trimmedLine.split("|")
        val name = parts.getOrNull(0)?.trim() ?: trimmedLine
        val quantity = parts.getOrNull(1)?.trim() ?: "1 ADET"

        productList.add(
            ProductItem(
                name = name,
                quantity = quantity
            )
        )
    }

    return productList
}

fun parseReceiptText(rawText: String): String {
    val lines = rawText.split("\n")
    val parsedItems = mutableListOf<String>()

    val ignoredKeywords = listOf(
        "KDV", "TOPLAM", "TOPKDV", "TCKN", "TARİH", "TRH", "SAAT", "FİŞ", "FIS", "NO", "KASA",
        "BİM", "BIM", "A101", "MİGROS", "ŞOK", "SOK", "WELCOME", "TEŞEKKÜR", "MERKEZ", "VERGİ",
        "MATRAH", "NRO", "PARA", "NAKİT", "KREDİ", "KART", "ÜSTÜ", "DEĞİŞİM",
        "BİLGİ", "MALİ", "HAFIZA", "POS", "BANKA", "ARATOPLAM",
        "ŞUBE", "SUBE", "CAD", "CADDESİ", "SOK", "SOKAĞI", "MAH", "MAHALLESİ", "TEL", "MERSİS", "HOŞGELDİNİZ",
        "FATURA", "ETNN", "ETKN", "SERIAL", "SERİ", "SIRA", "E-FATURA", "E-ARŞİV", "EARŞİV", "BARKOD", "PLU",
        "VD", "V.D", "VDM", "V.D.M", "MÜKELLEF", "MUKELLEF", "BÜYÜK MÜKELLEFLER", "BUYUK MUKELLEFLER",
        "POŞET", "POSET", "AMBALAJ", "PLASTİK POŞET", "ALŞ.POŞETİ", "TASIMA POSETI",
        "TALAS", "KAYSERİ", "KAYSERI", "SİVAS", "SIVAS", "İLÇESİ", "İLCESİ",
        "HARMAN", "MH", "MH.", "MEZARLIK", "SK", "SK.", "NO:", "NO"
    )

    val stopKeywords = listOf("PARA ÜSTÜ", "TOPKDV", "TOPLAM", "TOPKDY", "TOPLAN", "ARATOP", "NAKİT", "KREDİ KART", "MALİ HAFIZA", "ARATOPLAM")
    val quantityRegex = Regex("""(?i)\b(\d+([.,]\d+)?\s*(ML|L|G|KG|GR|ADET|AD|PAKET|PK))\b""")
    val hasPriceRegex = Regex("""\d+([.,]\d+)?\s*(TL|₺)?$""")

    var hasStartedProducts = false

    for (line in lines) {
        val upperLine = line.uppercase(Locale("tr", "TR")).trim()

        if (stopKeywords.any { upperLine.contains(it) }) break
        if (upperLine.length < 3 || ignoredKeywords.any { upperLine.contains(it) }) continue

        val isBarcodeOrEtnn = upperLine.matches(Regex("""^\d{6,}$""")) ||
                upperLine.matches(Regex("""^[A-Z0-9-]{12,}$""")) ||
                upperLine.contains("ETNN") || upperLine.contains("ETKN") ||
                upperLine.contains("SK") || upperLine.contains("NO:")

        if (isBarcodeOrEtnn) continue

        val quantityMatch = quantityRegex.find(line)
        val hasPrice = hasPriceRegex.containsMatchIn(line)

        if (!hasStartedProducts) {
            if (quantityMatch != null || hasPrice) {
                hasStartedProducts = true
            } else {
                continue
            }
        }

        val finalItem = if (quantityMatch != null) {
            val cutIndex = quantityMatch.range.first
            line.substring(0, cutIndex).trim()
        } else {
            line.replace(Regex("""%\s*\d+"""), "")
                .replace(Regex("""\s+\d+([.,]\d+)?\s*(TL|₺)?$"""), "")
                .replace(Regex("""[*#-].*"""), "")
                .trim()
        }

        val cleanName = finalItem
            .replace(Regex("""[*#%-]"""), "")
            .trim()
            .uppercase(Locale("tr", "TR"))

        val containsDateOrTime = cleanName.contains(Regex("""\d{2}[./:]\d{2}"""))
        val isOnlyDigits = cleanName.all { it.isDigit() || it.isWhitespace() }
        val isOnlyVolumeOrWeight = cleanName.matches(Regex("""(?i)^\d+([.,]\d+)?\s*(ML|L|G|KG|GR|ADET|AD|PAKET|PK)$"""))
        val isMultiplierPattern = cleanName.matches(Regex("""(?i)^\d+\s*[X*x]$"""))

        if (cleanName.length >= 3
            && !containsDateOrTime
            && !isOnlyDigits
            && !isOnlyVolumeOrWeight
            && !isMultiplierPattern
            && !ignoredKeywords.any { cleanName.contains(it) }
        ) {
            parsedItems.add(cleanName)
        }
    }

    if (parsedItems.isNotEmpty()) {
        val groupedMap = LinkedHashMap<String, Int>()
        for (item in parsedItems) {
            groupedMap[item] = groupedMap.getOrDefault(item, 0) + 1
        }

        val formattedList = groupedMap.map { (item, count) ->
            "$item|$count ADET"
        }

        return formattedList.joinToString("\n")
    } else {
        return "Net ürün tespit edilemedi, lütfen manuel giriniz."
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiptScannerScreenPreview() {
    ReceiptScannerScreen()
}