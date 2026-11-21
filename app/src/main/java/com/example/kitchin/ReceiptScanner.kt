package com.example.kitchin

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ReceiptScanner(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Galeriden veya kameradan gelen görselin URI'sini alıp metne çevirir
    fun scanReceipt(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Fiş üzerindeki tüm metin başarıyla okundu
                    onSuccess(visionText.text)
                }
                .addOnFailureListener { e ->
                    // Okuma sırasında hata oluştu
                    onFailure(e)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}