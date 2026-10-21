/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.impl

import androidx.work.Configuration
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 24)
class ProcessIsolationConfigurationTest {

    @Test
    fun isProcessIsolationEnabled_defaultsToFalse() {
        val configuration = Configuration.Builder().build()
        assertThat(configuration.isProcessIsolationEnabled).isFalse()
    }

    @Test
    fun setProcessIsolationEnabled_enablesAndDisablesIsolation() {
        val enabledConfig = Configuration.Builder().setProcessIsolationEnabled(true).build()
        assertThat(enabledConfig.isProcessIsolationEnabled).isTrue()

        val disabledConfig =
            Configuration.Builder()
                .setProcessIsolationEnabled(true)
                .setProcessIsolationEnabled(false)
                .build()
        assertThat(disabledConfig.isProcessIsolationEnabled).isFalse()
    }

    @Test
    fun builderCopyConstructor_preservesProcessIsolationFlag() {
        val enabledConfig = Configuration.Builder().setProcessIsolationEnabled(true).build()
        val copiedEnabled = Configuration.Builder(enabledConfig).build()
        assertThat(copiedEnabled.isProcessIsolationEnabled).isTrue()

        val disabledConfig = Configuration.Builder().setProcessIsolationEnabled(false).build()
        val copiedDisabled = Configuration.Builder(disabledConfig).build()
        assertThat(copiedDisabled.isProcessIsolationEnabled).isFalse()
    }

    @Test
    fun isolationInternalBridge_mirrorsConfigurationProperty() {
        val defaultConfig = Configuration.Builder().build()
        assertThat(IsolationInternalBridge.isProcessIsolationEnabled(defaultConfig)).isFalse()

        val enabledConfig = Configuration.Builder().setProcessIsolationEnabled(true).build()
        assertThat(IsolationInternalBridge.isProcessIsolationEnabled(enabledConfig)).isTrue()

        val disabledConfig = Configuration.Builder().setProcessIsolationEnabled(false).build()
        assertThat(IsolationInternalBridge.isProcessIsolationEnabled(disabledConfig)).isFalse()
    }
}
