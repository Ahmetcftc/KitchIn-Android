package com.example.kitchin

import android.net.Uri
import android.os.Bundle
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

    // Onaylanan stok ürünlerinin tutulacağı geçici liste
    val confirmedProducts = remember { mutableStateListOf<ProductItem>() }

    val receiptScanner = remember { ReceiptScanner(context) }

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

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Galeriden Fiş Fotoğrafı Seç & Tara")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FİŞ OKUNDUĞUNDA DÜZENLEME EKRANI
        if (scannedText.isNotEmpty()) {
            Text(
                text = "Fişten Algılanan Ürünler (Düzenlenebilir):",
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

        // EKLENEN STOK LİSTESİ GÖRÜNÜMÜ (SİLME VE ADET DÜZENLEME DAHİL)
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
                            // Ürün Adı
                            Text(
                                text = "${index + 1}. ${product.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )

                            // Adet Artırma / Azaltma ve Silme Butonları
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Adet Azalt (-)
                                IconButton(
                                    onClick = {
                                        val currentCount = extractCount(product.quantity)
                                        if (currentCount > 1) {
                                            confirmedProducts[index] = product.copy(
                                                quantity = "${currentCount - 1} ADET"
                                            )
                                        }
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

                                // Adet Artır (+)
                                IconButton(
                                    onClick = {
                                        val currentCount = extractCount(product.quantity)
                                        confirmedProducts[index] = product.copy(
                                            quantity = "${currentCount + 1} ADET"
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("+", style = MaterialTheme.typography.titleMedium)
                                }

                                // Tekli Ürün Silme Buton (Kırmızı Metin Buton)
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

            // VERİTABANINA / ANA STOĞA KAYDET BUTONU
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
}

fun extractCount(quantityStr: String): Int {
    return quantityStr.split(" ").getOrNull(0)?.toIntOrNull() ?: 1
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
        val unit = parts.getOrNull(1) ?: "ADET"

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