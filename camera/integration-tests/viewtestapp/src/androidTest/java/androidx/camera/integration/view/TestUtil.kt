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

package androidx.camera.integration.view

import android.app.Instrumentation
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

internal object TestUtil {
    private const val TIMEOUT_SECONDS = 10L

    fun <T : Fragment> FragmentScenario<T>.getFragment(): T {
        var fragment: T? = null
        this.onFragment { newValue: T -> fragment = newValue }
        return fragment!!
    }

    fun EffectsFragment.assertPreviewStreamingState(
        streamState: PreviewView.StreamState,
        instrumentation: Instrumentation,
    ) {
        val previewStreaming = Semaphore(0)
        instrumentation.runOnMainSync {
            previewView.previewStreamState.observe(this) {
                if (it == streamState) {
                    previewStreaming.release()
                }
            }
        }
        assertThat(previewStreaming.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }
}
