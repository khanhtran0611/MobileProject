package com.example.flashcardapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flashcardapp.databinding.SignInBinding
import com.example.flashcardapp.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive

class SigninActivity : AppCompatActivity() {
    lateinit var binding: SignInBinding

    companion object {
        private const val TAG = "SigninActivity"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigate to SignUp when "No register yet" button is clicked
        binding.noRegisterButton.setOnClickListener {
            Log.d(TAG, "No register button clicked, navigating to SignUpActivity")
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Sign in button click listener
        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            val password = binding.passwordInput.text?.toString() ?: ""

            // Basic validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform sign-in with Supabase
            signIn(email, password)
        }
    }

    private fun signIn(email: String, password: String) {
        Log.d(TAG, "Sign in attempt for email: $email")
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Attempting authentication with Supabase...")
                SupabaseProvider.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Log.d(TAG, "Authentication successful")

                // Get the authenticated user ID
                val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
                Log.d(TAG, "Retrieved user ID: $userId")

                if (userId == null) {
                    Log.e(TAG, "Failed to get user ID after successful authentication")
                    Toast.makeText(this@SigninActivity, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Retrieve user data from the User table
                Log.d(TAG, "Fetching user data from database for user ID: $userId")
                val userList = SupabaseProvider.client.from("User")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<User>()

                Log.d(TAG, "User data retrieved. Count: ${userList.size}")

                if (userList.isNotEmpty()) {
                    val user = userList.first()
                    Log.d(TAG, "Sign in successful for user: ${user.username}")

                    // Compare lastime_access (date only) with today UTC
                    val now = OffsetDateTime.now(ZoneOffset.UTC)
                    val todayUtc = now.toLocalDate()
                    val yesterdayUtc = todayUtc.minusDays(1)
                    val lastDate: LocalDate? = try {
                        user.lastime_access?.let { OffsetDateTime.parse(it).toLocalDate() }
                    } catch (e: DateTimeParseException) {
                        Log.w(TAG, "Failed to parse lastime_access='${user.lastime_access}', will treat as no recent access", e)
                        null
                    }

                    when {
                        lastDate == todayUtc -> {
                            Log.d(TAG, "Last access is today; no streak update.")
                        }
                        lastDate == yesterdayUtc -> {
                            // Exactly yesterday: increment streak and update lastime_access to now
                            val newStreak = (user.streak ?: 0) + 1
                            Log.d(TAG, "Yesterday login detected. Incrementing streak to $newStreak and updating lastime_access to $now")
                            try {
                                val payload = buildJsonObject {
                                    put("streak", JsonPrimitive(newStreak))
                                    put("lastime_access", JsonPrimitive(now.toString()))
                                }
                                SupabaseProvider.client.from("User").update(payload) {
                                    filter { eq("user_id", userId) }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update streak/lastime_access: ${e.message}", e)
                            }
                        }
                        else -> {
                            // Not yesterday or today: reset streak to 0 and update lastime_access
                            Log.d(TAG, "Last access not yesterday or today (lastDate=$lastDate). Resetting streak to 0 and updating lastime_access to $now")
                            try {
                                val payload = buildJsonObject {
                                    put("streak", JsonPrimitive(0))
                                    put("lastime_access", JsonPrimitive(now.toString()))
                                }
                                SupabaseProvider.client.from("User").update(payload) {
                                    filter { eq("user_id", userId) }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to reset streak/update lastime_access: ${e.message}", e)
                            }
                        }
                    }

                    Toast.makeText(this@SigninActivity, "Welcome ${user.username}!", Toast.LENGTH_SHORT).show()

                    // Set globals
                    UserSession.userId = userId
                    UserSession.username = user.username

                    // Navigate to MainLayoutActivity and pass extras
                    val intent = Intent(this@SigninActivity, MainLayoutActivity::class.java)
                    intent.putExtra(EXTRA_USERNAME, user.username)
                    intent.putExtra(EXTRA_USER_ID, userId)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e(TAG, "User data not found in database for user ID: $userId")
                    Toast.makeText(this@SigninActivity, "User data not found", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed with exception: ${e::class.simpleName}", e)
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                Toast.makeText(this@SigninActivity, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}