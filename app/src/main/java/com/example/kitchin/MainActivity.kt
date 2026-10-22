package com.example.kitchin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kitchin.ui.theme.KitchInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Firebase veritabanını çağır
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

// Modelden test verileri oluştur
        val testEv = Household(evId = "ev123", aileAdi = "Çiftçi Ailesi", uyeler = listOf("Ahmet", "Enes"))
        val testUrun = InventoryItem(urunId = "urun1", urunAdi = "Süt", miktar = 2.0, birim = "litre")

// Veriyi Firestore'a gönder
        db.collection("Households").document(testEv.evId).set(testEv)
            .addOnSuccessListener {
                android.util.Log.d("FirebaseTest", "Ev başarıyla eklendi!")

                // Evin içine Inventory (Envanter) koleksiyonu açıp ürünü ekleyelim
                db.collection("Households").document(testEv.evId)
                    .collection("Inventory").document(testUrun.urunId).set(testUrun)
                    .addOnSuccessListener {
                        android.util.Log.d("FirebaseTest", "Ürün başarıyla eklendi!")
                    }
            }

        setContent {
            KitchInTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KitchInTheme {
        Greeting("Android")
    }
}