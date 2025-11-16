package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import com.example.flashcardapp.model.User

class StudyActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StudyActivity"
    }

    private var deckId: Int = -1
    private var recentUpdated = false // guard to avoid double updates
    private val recentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study)

        deckId = intent.getIntExtra(DeckDetailActivity.EXTRA_DECK_ID, -1)
        if (deckId == -1) {
            Toast.makeText(this, "Missing deck id for study", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d(TAG, "Starting study for deckId=$deckId")

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.studyContainer, StudyScreenFragment.newInstance(deckId))
                .commit()
        }
    }

    override fun onPause() {
        // Trigger before onStop/onDestroy to reduce cancellation risk
        if (!recentUpdated) {
            Log.d(TAG, "onPause: triggering recent decks update")
            updateRecentDecks()
        } else {
            Log.d(TAG, "onPause: recent decks already updated")
        }
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called; recentUpdated=$recentUpdated")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Activity finishing=${isFinishing}")
        super.onDestroy()
    }

    private fun updateRecentDecks() {
        val userId = UserSession.userId
        if (userId.isNullOrEmpty() || deckId == -1) {
            Log.w(TAG, "Skipping recent update: userId=$userId deckId=$deckId")
            return
        }
        recentScope.launch {
            withContext(NonCancellable) {
                try {
                    Log.d(TAG, "[RecentUpdate] Fetching user row user_id=$userId")
                    val user = SupabaseProvider.client.from("User")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<User>()
                        .firstOrNull()
                    if (user == null) {
                        Log.w(TAG, "[RecentUpdate] No user row found; abort")
                        return@withContext
                    }

                    val r1 = user.recent1?.takeIf { it != 0 }
                    val r2 = user.recent2?.takeIf { it != 0 }

                    // Duplicate avoidance: if deck already at recent1 or recent2 we won't re-write
                    if (r1 == deckId || r2 == deckId) {
                        Log.d(TAG, "[RecentUpdate] Deck $deckId already in recents (r1=$r1 r2=$r2); no changes")
                        recentUpdated = true
                        return@withContext
                    }

                    val body = buildJsonObject {
                        when {
                            r1 == null -> {
                                // First time: set recent1
                                Log.d(TAG, "[RecentUpdate] Setting recent1=$deckId (initial)")
                                put("recent1", JsonPrimitive(deckId))
                            }
                            r2 == null -> {
                                // Fill recent2 with new deck
                                Log.d(TAG, "[RecentUpdate] recent1 exists ($r1), recent2 empty -> set recent2=$deckId")
                                put("recent2", JsonPrimitive(deckId))
                            }
                            else -> {
                                // Both exist: shift recent2 up, new deck becomes recent2
                                Log.d(TAG, "[RecentUpdate] Shift: recent2($r2) -> recent1; new recent2=$deckId (old recent1=$r1)")
                                put("recent1", JsonPrimitive(r2))
                                put("recent2", JsonPrimitive(deckId))
                            }
                        }
                    }

                    SupabaseProvider.client.from("User").update(body) { filter { eq("user_id", userId) } }

                    val confirm = SupabaseProvider.client.from("User")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<User>()
                        .firstOrNull()
                    Log.d(TAG, "[RecentUpdate] Post-update values: recent1=${confirm?.recent1} recent2=${confirm?.recent2}")
                    recentUpdated = true
                } catch (e: CancellationException) {
                    Log.e(TAG, "[RecentUpdate] CancellationException: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e(TAG, "[RecentUpdate] Failed: ${e.message}", e)
                }
            }
        }
    }
}
