package com.example.kitchin // Kendi paket adınla aynı olduğundan emin ol

data class User(
    val userId: String = "",
    val ad: String = "",
    val evId: String = ""
)

data class Household(
    val evId: String = "",
    val aileAdi: String = "",
    val uyeler: List<String> = emptyList()
)

data class InventoryItem(
    val urunId: String = "",
    val urunAdi: String = "",
    val miktar: Double = 0.0,
    val birim: String = "",
    val sonTuketimTarihi: String? = null
)

data class ShoppingItem(
    val listeId: String = "",
    val urunAdi: String = "",
    val miktar: String = "",
    val durum: String = "Alınacak",
    val ekleyenUye: String = ""
)