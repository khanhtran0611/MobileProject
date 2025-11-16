package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flashcardapp.databinding.ResetPaswordBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ResetPasswordActivity"
    }

    private lateinit var binding: ResetPaswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ResetPaswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConfirmReset.setOnClickListener {
            val pass1 = binding.etNewPassword.text?.toString()?.trim() ?: ""
            val pass2 = binding.etRetypePassword.text?.toString()?.trim() ?: ""

            if (pass1.isEmpty() || pass2.isEmpty()) {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass1 != pass2) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass1.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Updating password via Supabase")
                    SupabaseProvider.client.auth.updateUser {
                        password = pass1
                    }
                    Toast.makeText(this@ResetPasswordActivity, "Password updated", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Password update failed: ${e.message}", e)
                    Toast.makeText(this@ResetPasswordActivity, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}