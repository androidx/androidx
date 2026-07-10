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

package androidx.biometric.internal.viewmodel

import androidx.biometric.internal.ui.FingerprintDialogState
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FingerprintDialogViewModelTest {
    private val viewModel: FingerprintDialogViewModel = FingerprintDialogViewModel()

    @Test
    fun testInitialState() = runTest {
        assertThat(viewModel.isDismissedInstantly).isTrue()
        assertThat(viewModel.previousState).isEqualTo(FingerprintDialogState.NONE)
        assertThat(viewModel.state.first()).isEqualTo(FingerprintDialogState.NONE)
        val actualHelpMessageInfo = viewModel.helpMessageInfo.first()
        assertThat(actualHelpMessageInfo.first).isEqualTo("")
        assertThat(actualHelpMessageInfo.second).isFalse()
    }

    @Test
    fun testSetStateAndHelpMessage() = runTest {
        val testState = FingerprintDialogState.FINGERPRINT_ERROR
        val testMessage = "test message"

        viewModel.setState(testState, testMessage)

        assertThat(viewModel.state.first()).isEqualTo(testState)
        val actualHelpMessageInfo = viewModel.helpMessageInfo.first()
        assertThat(actualHelpMessageInfo.first).isEqualTo(testMessage)
        assertThat(actualHelpMessageInfo.second).isTrue()
    }

    @Test
    fun testSetCancelPending() = runTest {
        var cancelPending = false
        val job = launch { viewModel.isCancelPending.collect { cancelPending = it } }

        viewModel.setCancelPending(false)
        runCurrent()
        assertThat(cancelPending).isFalse()

        viewModel.setCancelPending(true)
        runCurrent()
        assertThat(cancelPending).isTrue()
        job.cancel()
    }
}
