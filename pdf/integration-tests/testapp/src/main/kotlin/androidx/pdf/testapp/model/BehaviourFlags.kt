/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.testapp.model

import android.os.Bundle

/**
 * A data class for behavior flags used across the test app to toggle feature behavior. This class
 * provides a structured way to encapsulate feature flags.
 */
internal data class BehaviourFlags(val customLinkHandlingEnabled: Boolean = false) {
    fun toBundle(outState: Bundle): Bundle {
        outState.putBoolean(KEY_CUSTOM_LINK_HANDLING_ENABLED, customLinkHandlingEnabled)
        return outState
    }

    fun fromBundle(bundle: Bundle): BehaviourFlags {
        val customLinkHandlingEnabled = bundle.getBoolean(KEY_CUSTOM_LINK_HANDLING_ENABLED)
        return BehaviourFlags(customLinkHandlingEnabled)
    }

    companion object {
        private const val KEY_CUSTOM_LINK_HANDLING_ENABLED = "custom_link_handling_enabled"
    }
}
