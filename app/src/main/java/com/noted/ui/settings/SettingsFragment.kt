package com.noted.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.noted.data.ApiKeyStore
import com.noted.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val store = ApiKeyStore(requireContext())

        binding.claudeEnabledSwitch.isChecked = store.isEnabled()
        binding.apiKeyInput.setText(store.getKey() ?: "")

        binding.saveButton.setOnClickListener {
            val key = binding.apiKeyInput.text.toString().trim()
            val enabled = binding.claudeEnabledSwitch.isChecked

            if (enabled && key.isEmpty()) {
                binding.apiKeyInput.error = "Enter your Anthropic API key to enable Claude"
                return@setOnClickListener
            }

            store.saveKey(key)
            store.setEnabled(enabled)
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
