package com.example.flashcardapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.flashcardapp.databinding.SignUpBinding
import com.example.flashcardapp.model.User
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SignUpActivity : AppCompatActivity() {
    lateinit var binding: SignUpBinding

    companion object {
        private const val TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigate to SignIn when "Already registered" button is clicked
        binding.alreadyRegisteredButton.setOnClickListener {
            Log.d(TAG, "Already registered button clicked, navigating to SigninActivity")
            val intent = Intent(this, SigninActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Listen to the Sign Up button click
        binding.signUpButton.setOnClickListener {
            val username = binding.usernameInput.text?.toString()?.trim() ?: ""
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            val password = binding.passwordInput.text?.toString() ?: ""
            val retype = binding.retypePasswordInput.text?.toString() ?: ""

            Log.d(TAG, "Sign up button clicked. Username: $username, Email: $email")

            // Basic validation
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || retype.isEmpty()) {
                Log.w(TAG, "Validation failed: One or more fields are empty")
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != retype) {
                Log.w(TAG, "Validation failed: Passwords do not match")
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform sign-up with Supabase in a coroutine
            Log.d(TAG, "Starting sign up process...")
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Attempting to sign up with Supabase Auth...")
                    val signUpResult = SupabaseProvider.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // Extract user id from the sign-up result
                    // The signUpResult object contains the user data
                    val userId = signUpResult?.id
                    Log.d(TAG, "Sign up successful. User ID: $userId")

                    if (userId == null) {
                        Log.e(TAG, "Sign up failed: Could not get user ID from result")
                        Toast.makeText(this@SignUpActivity, "Sign up failed: Could not get user ID", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Insert into "User" table with correct field names from User.kt model
                    try {
                        val nowIso = OffsetDateTime.now(ZoneOffset.UTC).toString()
                        val newUser = User(
                            id = null, // Will be auto-incremented by Supabase
                            username = username,
                            today = 0,
                            total_created = 0,
                            total_learned = 0,
                            user_id = userId,
                            lastime_access = nowIso,
                            streak = 0
                        )

                        Log.d(TAG, "Inserting user data into database: $newUser")

                        // Use Postgrest to insert (id will be auto-incremented by Supabase)
                        SupabaseProvider.client.from("User").insert(newUser)

                        Log.d(TAG, "User data inserted successfully into database")
                        Toast.makeText(this@SignUpActivity, "Sign up successful! Please sign in.", Toast.LENGTH_SHORT).show()

                        // Navigate to sign in screen
                        Log.d(TAG, "Navigating to sign in screen...")
                        val intent = Intent(this@SignUpActivity, SigninActivity::class.java)
                        startActivity(intent)
                        finish()

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create user record in database", e)
                        Log.e(TAG, "Error type: ${e::class.simpleName}")
                        Log.e(TAG, "Error message: ${e.message}")
                        Log.e(TAG, "Stack trace:", e)
                        Toast.makeText(this@SignUpActivity, "Failed to create user record: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Sign up failed during authentication", e)
                    Log.e(TAG, "Error type: ${e::class.simpleName}")
                    Log.e(TAG, "Error message: ${e.message}")
                    Log.e(TAG, "Stack trace:", e)
                    Toast.makeText(this@SignUpActivity, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}