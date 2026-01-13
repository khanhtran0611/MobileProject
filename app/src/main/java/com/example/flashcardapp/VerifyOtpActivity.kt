package com.example.flashcardapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flashcardapp.databinding.VerifyOtpBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class VerifyOtpActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VerifyOtpActivity"
    }

    private lateinit var binding: VerifyOtpBinding
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get email from intent
        userEmail = intent.getStringExtra(ForgotPasswordActivity.EXTRA_EMAIL)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Reset password button
        binding.btnResetPassword.setOnClickListener {
            val otp = binding.etOtp.text?.toString()?.trim() ?: ""
            val newPassword = binding.etNewPassword.text?.toString() ?: ""
            val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

            // Validation
            if (otp.isEmpty()) {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all password fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(otp, newPassword)
        }
    }

    private fun resetPassword(otp: String, newPassword: String) {
        Log.d(TAG, "Verifying OTP and resetting password")
        lifecycleScope.launch {
            try {
                if (userEmail == null) {
                    Toast.makeText(this@VerifyOtpActivity, "Email not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Verify OTP and set new password using Supabase
                SupabaseProvider.client.auth.verifyEmailOtp(
                    type = io.github.jan.supabase.auth.OtpType.Email.RECOVERY,
                    email = userEmail!!,
                    token = otp
                )

                // After successful OTP verification, update the password
                SupabaseProvider.client.auth.updateUser {
                    password = newPassword
                }

                Log.d(TAG, "Password reset successful")
                Toast.makeText(
                    this@VerifyOtpActivity,
                    "Password reset successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate back to sign in
                val intent = Intent(this@VerifyOtpActivity, SigninActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Password reset failed: ${e.message}", e)
                Toast.makeText(
                    this@VerifyOtpActivity,
                    "Failed to reset password: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

