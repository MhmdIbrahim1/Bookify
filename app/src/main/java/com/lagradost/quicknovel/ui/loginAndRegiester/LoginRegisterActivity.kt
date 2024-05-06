package com.lagradost.quicknovel.ui.loginAndRegiester

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.lagradost.quicknovel.databinding.ActivityLoginRegisterBinding


class LoginRegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLoginRegisterBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }



}