package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.flashcardapp.databinding.StatScreenBinding

class StatFragment : Fragment(){
    private var _binding: StatScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View? {
        _binding = StatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
        val username = arguments?.getString(MainLayoutActivity.ARG_USERNAME) ?: UserSession.username
        Log.d("StatFragment", "userId=$userId username=$username")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}