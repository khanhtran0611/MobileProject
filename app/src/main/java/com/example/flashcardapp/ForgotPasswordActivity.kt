package com.example.flashcardapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flashcardapp.databinding.ForgotPasswordBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ForgotPasswordActivity"
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var binding: ForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Send verification code button
        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendResetCode(email)
        }
    }

    private fun sendResetCode(email: String) {
        Log.d(TAG, "Sending reset code to: $email")
        lifecycleScope.launch {
            try {
                // Send password reset email with OTP
                SupabaseProvider.client.auth.resetPasswordForEmail(email)

                Log.d(TAG, "Reset code sent successfully")
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Verification code sent to $email",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate to OTP verification screen
                val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                intent.putExtra(EXTRA_EMAIL, email)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send reset code: ${e.message}", e)
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Failed to send code: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

