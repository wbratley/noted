package com.noted.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.noted.R
import com.noted.data.AiProvider
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

        binding.aiEnabledSwitch.isChecked = store.isEnabled()
        binding.apiKeyInput.setText(store.getKey() ?: "")

        when (store.getProvider()) {
            AiProvider.ANTHROPIC -> binding.providerGroup.check(R.id.provider_anthropic)
            AiProvider.DEEPSEEK -> binding.providerGroup.check(R.id.provider_deepseek)
        }
        updateKeyHint(binding.providerGroup.checkedRadioButtonId)

        binding.providerGroup.setOnCheckedChangeListener { _, id -> updateKeyHint(id) }

        binding.saveButton.setOnClickListener {
            val key = binding.apiKeyInput.text.toString().trim()
            val enabled = binding.aiEnabledSwitch.isChecked

            if (enabled && key.isEmpty()) {
                binding.apiKeyInput.error = "Enter your API key to enable AI"
                return@setOnClickListener
            }

            val provider =
                if (binding.providerGroup.checkedRadioButtonId == R.id.provider_deepseek) {
                    AiProvider.DEEPSEEK
                } else {
                    AiProvider.ANTHROPIC
                }

            store.saveKey(key)
            store.setEnabled(enabled)
            store.setProvider(provider)
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updateKeyHint(providerId: Int) {
        binding.apiKeyInput.hint =
            if (providerId == R.id.provider_deepseek) "sk-..." else "sk-ant-..."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
