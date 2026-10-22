package com.example.kitchin // Kendi paket adınla aynı olduğundan emin ol

import com.google.firebase.auth.FirebaseAuth

class AuthManager {
    private val auth = FirebaseAuth.getInstance()

    // Yeni kullanıcı kayıt fonksiyonu
    fun kayitOl(email: String, sifre: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, sifre)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, auth.currentUser?.uid) // Başarılıysa User ID döner
                } else {
                    onResult(false, task.exception?.localizedMessage) // Hata varsa mesaj döner
                }
            }
    }

    // Mevcut kullanıcı giriş fonksiyonu
    fun girisYap(email: String, sifre: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, sifre)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, auth.currentUser?.uid)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }
}