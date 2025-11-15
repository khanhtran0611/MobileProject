package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.flashcardapp.databinding.HomeScreenBinding

class HomeScreenFragment : Fragment(){
    private var _binding : HomeScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = HomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
        val username = arguments?.getString(MainLayoutActivity.ARG_USERNAME) ?: UserSession.username
        Log.d("HomeScreenFragment", "userId=$userId username=$username")
        // TODO: use userId to query user-specific data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}