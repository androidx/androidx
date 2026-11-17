/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.biometric.integration.testapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.biometric.integration.testapp.databinding.MainActivityBinding
import androidx.fragment.app.FragmentActivity

/** Main activity for the AndroidX Biometric test app. */
class MainActivity : FragmentActivity() {
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set button callback for Compose scenario tabs.
        binding.composeSamplesButton.setOnClickListener {
            launch<BiometricComposeSamplesActivity>()
        }

        // Set button callbacks for focused scenario activities.
        binding.simpleBiometricAuthButton.setOnClickListener {
            launch<SimpleBiometricAuthActivity>()
        }
        binding.credentialOnlyAuthButton.setOnClickListener { launch<CredentialOnlyAuthActivity>() }
        binding.fallbackOptionsAuthButton.setOnClickListener {
            launch<FallbackOptionsAuthActivity>()
        }
        binding.contentViewsAuthButton.setOnClickListener { launch<ContentViewsAuthActivity>() }
        binding.advancedAuthButton.setOnClickListener { launch<AdvancedAuthActivity>() }

        // Set button callbacks for full config / QA test activities.
        binding.biometricPromptButton.setOnClickListener { launch<BiometricPromptTestActivity>() }
        binding.authResultButton.setOnClickListener { launch<AuthenticationResultTestActivity>() }
    }

    /** Launches an instance of the given test activity [T]. */
    private inline fun <reified T : Activity> launch() {
        startActivity(Intent(this, T::class.java))
    }
}
