package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.flashcardapp.databinding.StatScreenBinding
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import com.example.flashcardapp.model.Deck
import com.example.flashcardapp.model.User
import com.example.flashcardapp.model.LearningLogs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapp.ui.DeckStatsAdapter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.XAxis
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

class StatFragment : Fragment(){
    private var _binding: StatScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        _binding = StatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
        val username = arguments?.getString(MainLayoutActivity.ARG_USERNAME) ?: UserSession.username
        Log.d("StatFragment", "userId=$userId username=$username")

        // Set labels per requirement using string resources
        binding.statALabel.text = getString(R.string.statA_label)
        binding.statBLabel.text = getString(R.string.statB_label)

        if (userId.isNullOrEmpty()) {
            binding.statAValue.text = "0"
            binding.statBValue.text = "0"
            return
        }

        // Setup RecyclerView for deck stats
        val statsAdapter = DeckStatsAdapter()
        binding.rvStatsList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStatsList.adapter = statsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val decks: List<Deck> = SupabaseProvider.client
                    .from("Deck")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                val totalCompleted = decks.count { d ->
                    val t = d.total
                    val p = d.progress
                    (t != null && t > 0 && p != null && t == p)
                }
                binding.statAValue.text = totalCompleted.toString()
                // Submit decks to adapter for list display
                statsAdapter.submitList(decks)
            } catch (e: Exception) {
                Log.e("StatFragment", "Failed computing total completed: ${e.message}", e)
                binding.statAValue.text = "0"
            }

            try {
                val users: List<User> = SupabaseProvider.client
                    .from("User")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                val totalDecks = users.firstOrNull()?.total_created ?: 0
                binding.statBValue.text = totalDecks.toString()
            } catch (e: Exception) {
                Log.e("StatFragment", "Failed loading total_created: ${e.message}", e)
                binding.statBValue.text = "0"
            }

            // Load learning logs and setup chart
            try {
                val learningLogs: List<LearningLogs> = SupabaseProvider.client
                    .from("learning_logs")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                Log.d("StatFragment", "Loaded ${learningLogs.size} learning logs")
                setupLineChart(learningLogs)
            } catch (e: Exception) {
                Log.e("StatFragment", "Failed loading learning logs: ${e.message}", e)
            }
        }
    }

    private fun setupLineChart(logs: List<LearningLogs>) {
        if (logs.isEmpty()) {
            Log.d("StatFragment", "No learning logs data")
            // Show empty chart or message
            binding.lineChart.clear()
            binding.lineChart.setNoDataText("No learning data available")
            binding.lineChart.invalidate()
            return
        }

        // Sort logs by day (ascending)
        val sortedLogs = logs.sortedBy { it.day }

        val entries = mutableListOf<Entry>()
        val dateLabels = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        sortedLogs.forEachIndexed { index, log ->
            val totalLearned = log.total_learned?.toFloat() ?: 0f
            entries.add(Entry(index.toFloat(), totalLearned))

            // Format date for x-axis labels
            val dayStr = log.day ?: ""
            try {
                val parsedDate = inputDateFormat.parse(dayStr.take(19))
                dateLabels.add(parsedDate?.let { dateFormat.format(it) } ?: dayStr.take(10))
            } catch (e: Exception) {
                try {
                    val simpleParse = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayStr.take(10))
                    dateLabels.add(simpleParse?.let { dateFormat.format(it) } ?: dayStr.take(10))
                } catch (e2: Exception) {
                    dateLabels.add(dayStr.take(10))
                }
            }
        }

        // Create dataset with styling
        val dataSet = LineDataSet(entries, "Decks Learned Per Day").apply {
            color = Color.parseColor("#6200EE")
            lineWidth = 3f
            circleRadius = 5f
            setCircleColor(Color.parseColor("#3700B3"))
            setDrawCircleHole(false)
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            setDrawValues(true)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#BB86FC")
            fillAlpha = 50
        }

        val lineData = LineData(dataSet)

        binding.lineChart.apply {
            data = lineData
            description.isEnabled = false
            setDrawGridBackground(false)

            // X-axis configuration
            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value.toInt() in dateLabels.indices) {
                            dateLabels[value.toInt()]
                        } else ""
                    }
                }
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 10f
                textColor = Color.DKGRAY
            }

            // Y-axis configuration
            axisLeft.apply {
                granularity = 1f
                axisMinimum = 0f
                textSize = 11f
                textColor = Color.DKGRAY
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }
            axisRight.isEnabled = false

            // Legend configuration
            legend.isEnabled = true
            legend.textSize = 12f

            // Enable touch gestures
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            // Animation
            animateX(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}