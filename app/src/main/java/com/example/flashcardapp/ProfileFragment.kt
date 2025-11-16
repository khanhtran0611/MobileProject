package com.example.flashcardapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardapp.databinding.ProfileScreenBinding
import com.example.flashcardapp.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(){
    private var _binding: ProfileScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ProfileScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
        val usernameArg = arguments?.getString(MainLayoutActivity.ARG_USERNAME) ?: UserSession.username
        Log.d("ProfileFragment", "userId=$userId usernameArg=$usernameArg")

        // Prefill with known values, then refresh from Supabase
        binding.tvProfileUsername.text = usernameArg ?: getString(R.string.profile_username_placeholder)
        binding.tvProfileEmail.text = SupabaseProvider.client.auth.currentUserOrNull()?.email
            ?: getString(R.string.profile_email_placeholder)

        // Refresh user info from Supabase
        if (!userId.isNullOrEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val users = SupabaseProvider.client.from("User")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<User>()
                    users.firstOrNull()?.let { u ->
                        binding.tvProfileUsername.text = u.username
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Failed to load user info: ${e.message}", e)
                }
            }
        }

        // Reset Password navigation
        binding.btnResetPassword.setOnClickListener {
            startActivity(Intent(requireContext(), ResetPasswordActivity::class.java))
        }

        // Sign out button
        binding.btnSignOut.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    SupabaseProvider.client.auth.signOut()
                    // Clear local session
                    UserSession.userId = null
                    UserSession.username = null
                    // Navigate back to sign-in
                    val intent = Intent(requireContext(), SigninActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Sign out failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Sign out failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Edit profile dialog
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun showEditProfileDialog() {
        val currentEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: ""
        val currentUsername = binding.tvProfileUsername.text?.toString() ?: UserSession.username ?: ""

        val dialogView = layoutInflater.inflate(R.layout.deck_add_info, null) // reuse a simple layout? Better create dynamic linear layout.
        // Instead of reusing layout, build a lightweight container programmatically.
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }
        val etUsername = EditText(requireContext()).apply {
            hint = "Username"
            setText(currentUsername)
        }
        val etEmail = EditText(requireContext()).apply {
            hint = "Email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(currentEmail)
        }
        container.addView(etUsername)
        container.addView(etEmail)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(container)
            .setPositiveButton("Save") { d, _ ->
                val newUsername = etUsername.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                if (newUsername.isEmpty() || newEmail.isEmpty()) {
                    Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    updateProfile(newUsername, newEmail)
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun updateProfile(newUsername: String, newEmail: String) {
        val userId = UserSession.userId
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing user id", Toast.LENGTH_SHORT).show(); return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            var usernameUpdated = false
            var emailUpdated = false
            try {
                // Update username in User table
                SupabaseProvider.client.from("User").update(
                    kotlinx.serialization.json.buildJsonObject { put("username", kotlinx.serialization.json.JsonPrimitive(newUsername)) }
                ) {
                    filter { eq("user_id", userId) }
                }
                usernameUpdated = true
                UserSession.username = newUsername // propagate instantly
                binding.tvProfileUsername.text = newUsername
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Failed updating username: ${e.message}", e)
            }
            try {
                // Update email via auth (Supabase may send confirmation email depending on settings)
                SupabaseProvider.client.auth.updateUser {
                    email = newEmail
                }
                emailUpdated = true
                binding.tvProfileEmail.text = newEmail
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Failed updating email: ${e.message}", e)
            }
            val msg = when {
                usernameUpdated && emailUpdated -> "Profile updated"
                usernameUpdated -> "Username updated"
                emailUpdated -> "Email updated"
                else -> "No changes saved"
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}