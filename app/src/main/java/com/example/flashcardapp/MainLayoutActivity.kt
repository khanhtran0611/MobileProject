package com.example.flashcardapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.flashcardapp.databinding.MainLayoutScreenBinding

class MainLayoutActivity : AppCompatActivity(){
    lateinit var binding: MainLayoutScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainLayoutScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set welcome text if username was passed
        intent.getStringExtra(SigninActivity.EXTRA_USERNAME)?.let { username ->
            binding.welcomeText.text = getString(R.string.welcome_back, username)
            UserSession.username = username
        }
        // Receive and store userId globally
        intent.getStringExtra(SigninActivity.EXTRA_USER_ID)?.let { uid ->
            UserSession.userId = uid
        }

        // Load initial (home) fragment only first time
        if (savedInstanceState == null) {
            replaceFragment(createHomeFragmentWithArgs(), HOME_TAG)
            binding.bottomNavigation.selectedItemId = R.id.navigation_home
        }

        // Set up bottom navigation item selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(findOrCreate(HOME_TAG) { createHomeFragmentWithArgs() }, HOME_TAG)
                    true
                }
                R.id.navigation_cards -> {
                    replaceFragment(findOrCreate(DECK_TAG) { createDeckFragmentWithArgs() }, DECK_TAG)
                    true
                }
                R.id.navigation_stats -> {
                    replaceFragment(findOrCreate(STATS_TAG) { createStatsFragmentWithArgs() }, STATS_TAG)
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(findOrCreate(PROFILE_TAG) { createProfileFragmentWithArgs() }, PROFILE_TAG)
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.setOnItemReselectedListener { /* no-op */ }
    }

    private fun baseArgs(): Bundle = Bundle().apply {
        UserSession.userId?.let { putString(ARG_USER_ID, it) }
        UserSession.username?.let { putString(ARG_USERNAME, it) }
    }

    private fun createHomeFragmentWithArgs(): Fragment = HomeScreenFragment().apply {
        arguments = baseArgs()
    }
    private fun createDeckFragmentWithArgs(): Fragment = DeckManagementFragment().apply {
        arguments = baseArgs()
    }
    private fun createStatsFragmentWithArgs(): Fragment = StatFragment().apply {
        arguments = baseArgs()
    }
    private fun createProfileFragmentWithArgs(): Fragment = ProfileFragment().apply {
        arguments = baseArgs()
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val current = supportFragmentManager.findFragmentById(binding.mainContent.id)
        if (current?.tag == tag) return
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(binding.mainContent.id, fragment, tag)
            .commit()
    }

    private fun findOrCreate(tag: String, create: () -> Fragment): Fragment {
        return supportFragmentManager.findFragmentByTag(tag) ?: create()
    }

    companion object {
        private const val HOME_TAG = "fragment_home"
        private const val DECK_TAG = "fragment_deck_management"
        private const val STATS_TAG = "fragment_stats"
        private const val PROFILE_TAG = "fragment_profile"
        const val ARG_USER_ID = "arg_user_id"
        const val ARG_USERNAME = "arg_username"
    }
}