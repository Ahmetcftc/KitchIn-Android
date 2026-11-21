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
                        // 1. Metni ProductItem listesine çevir
                        val newItems = convertTextToProductList(scannedText)
                        // 2. Stok listesine ekle
                        confirmedProducts.addAll(newItems)
                        // 3. Ekranı temizle ve düzenleme modundan çık
                        scannedText = ""
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Onayla & Stoğa Ekle")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // EKLENEN STOK LİSTESİ GÖRÜNÜMÜ
        if (confirmedProducts.isNotEmpty()) {
            Text(
                text = "Mutfak Stoğundaki Ürünler (${confirmedProducts.size} Adet):",
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
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${product.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = product.quantity,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
data class ProductItem(
    val name: String,
    val quantity: String,
    val isAdded: Boolean = true
)
// ADIM 2: METNİ ÜRÜN OBJELERİNE DÖNÜŞTÜREN YARDIMCI FONKSİYON (TAM BURAYA)
fun convertTextToProductList(scannedText: String): List<ProductItem> {
    val lines = scannedText.split("\n")
    val productList = mutableListOf<ProductItem>()
    val quantityRegex = Regex("""(?i)\b(\d+([.,]\d+)?\s*(ML|L|G|KG|GR|ADET|AD|PAKET|PK))\b""")

    for (line in lines) {
        val trimmedLine = line.trim()
        if (trimmedLine.isBlank()) continue

        val match = quantityRegex.find(trimmedLine)
        if (match != null) {
            val quantity = match.value.trim()
            val name = trimmedLine.substring(0, match.range.start).trim()

            productList.add(
                ProductItem(
                    name = if (name.isNotBlank()) name else trimmedLine,
                    quantity = quantity
                )
            )
        } else {
            productList.add(ProductItem(name = trimmedLine, quantity = "1 ADET"))
        }
    }
    return productList
}

fun parseReceiptText(rawText: String): String {
    val lines = rawText.split("\n")
    val parsedItems = mutableListOf<String>()

    // Çöpe atılacak gereksiz fiş, başlık, fatura künyesi, vergi dairesi ve dipnot terimleri
    val ignoredKeywords = listOf(
        "KDV", "TOPLAM", "TOPKDV", "TOPLAM", "TCKN", "TARİH", "TRH", "SAAT", "FİŞ", "FIS", "NO", "KASA",
        "BİM", "BIM", "A101", "MİGROS", "ŞOK", "SOK", "WELCOME", "TEŞEKKÜR", "MERKEZ", "VERGİ",
        "MATRAH", "NRO", "PARA", "NAKİT", "KREDİ", "KART", "ÜSTÜ", "DEĞİŞİM",
        "BİLGİ", "MALİ", "HAFIZA", "POS", "BANKA", "TOPKDV", "ARATOPLAM",
        "ŞUBE", "SUBE", "CAD", "CADDESİ", "SOK", "SOKAĞI", "MAH", "MAHALLESİ", "TEL", "MERSİS", "HOŞGELDİNİZ",
        "FATURA", "ETNN", "ETKN", "SERIAL", "SERİ", "SIRA", "E-FATURA", "E-ARŞİV", "EARŞİV", "BARKOD", "PLU",
        "VD", "V.D", "VDM", "V.D.M", "MÜKELLEF", "MUKELLEF", "BÜYÜK MÜKELLEFLER", "BUYUK MUKELLEFLER",
        "POŞET", "POSET", "AMBALAJ", "PLASTİK POŞET", "ALŞ.POŞETİ", "TASIMA POSETI"
    )

    // Fişin sonuna gelindiğini gösteren durdurma kelimeleri
    val stopKeywords = listOf("PARA ÜSTÜ", "TOPKDV", "TOPLAM", "TOPKDY", "TOPLAN", "ARATOP", "NAKİT", "KREDİ KART", "TOPLAM", "MALİ HAFIZA", "TOPKDV", "ARATOPLAM")

    // Miktar/Birim ifadelerini yakalayan desen (200ML, 1.5L, 2ADET, 500G, 3KG, 2AD vb.)
    val quantityRegex = Regex("""(?i)\b(\d+([.,]\d+)?\s*(ML|L|G|KG|GR|ADET|AD|PAKET|PK))\b""")

    // Satır sonunda fiyat rakamı olup olmadığını kontrol eden desen (Örn: 25.00, 10,50, *15.00)
    val hasPriceRegex = Regex("""\d+([.,]\d+)?\s*(TL|₺)?$""")

    var hasStartedProducts = false

    for (line in lines) {
        val upperLine = line.uppercase().trim()

        // 1. Alt Kısım Kesme: Fişin sonundaki para üstü/toplam kısmında taramayı DURDUR
        if (stopKeywords.any { upperLine.contains(it) }) {
            break
        }

        // Fatura no, tarih, saat, ETNN gibi üst künye terimlerini içeren satırları direkt ATLA
        if (upperLine.length < 3 || ignoredKeywords.any { upperLine.contains(it) }) {
            continue
        }

        // BARKOD & ETNN FİLTRESİ:
        // - 6 haneden uzun tam sayılar (Barkod / Mersis No / Fiş No)
        // - Tireli/karmaşık uzun ETNN kodları (örn: 1a2b3c-4d5e...)
        val isBarcodeOrEtnn = upperLine.matches(Regex("""^\d{6,}$""")) ||
                upperLine.matches(Regex("""^[A-Z0-9-]{12,}$""")) ||
                upperLine.contains("ETNN") || upperLine.contains("ETKN")

        if (isBarcodeOrEtnn) {
            continue
        }

        val quantityMatch = quantityRegex.find(line)
        val hasPrice = hasPriceRegex.containsMatchIn(line)

        // 2. Kesin Ürün Başlangıcı Tespiti:
        if (!hasStartedProducts) {
            if (quantityMatch != null || hasPrice) {
                hasStartedProducts = true
            } else {
                continue // Fatura detayları ve üst başlıklar bitene kadar atla
            }
        }

        val finalItem = if (quantityMatch != null) {
            // Miktarın bittiği yere kadar al (Ürün Adı + Miktar)
            val cutIndex = quantityMatch.range.last + 1
            line.substring(0, cutIndex).trim()
        } else {
            // Miktar yoksa KDV (%20) ve satır sonu fiyatlarını sil
            line.replace(Regex("""%\s*\d+"""), "")
                .replace(Regex("""\s+\d+([.,]\d+)?\s*(TL|₺)?$"""), "")
                .replace(Regex("""[*#-].*"""), "")
                .trim()
        }

        // Temizlik: Başta/sonda kalan sembolleri temizle
        val cleanName = finalItem
            .replace(Regex("""[*#%-]"""), "")
            .trim()

        // Son Kontrol: Tarih/saat veya barkod kalıntısı barındırmayan geçerli ürün adı mı?
        val containsDateOrTime = cleanName.contains(Regex("""\d{2}[./:]\d{2}"""))
        val isOnlyDigits = cleanName.all { it.isDigit() || it.isWhitespace() }

        if (cleanName.length >= 3 && !containsDateOrTime && !isOnlyDigits && !ignoredKeywords.any { cleanName.uppercase().contains(it) }) {
            parsedItems.add(cleanName)
        }
    }

    return if (parsedItems.isNotEmpty()) {
        parsedItems.joinToString("\n")
    } else {
        "Net ürün tespit edilemedi, lütfen manuel giriniz."
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiptScannerScreenPreview() {
    ReceiptScannerScreen()
}
