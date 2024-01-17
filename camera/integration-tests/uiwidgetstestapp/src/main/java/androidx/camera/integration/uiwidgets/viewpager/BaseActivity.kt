/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.viewpager

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A basic activity used in UI widgets to share the common resources for testing. */
open class BaseActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BasicActivity"
        private const val LATCH_TIMEOUT: Long = 5000
        internal const val INTENT_LENS_FACING = "lens-facing"
        internal const val INTENT_IMPLEMENTATION_MODE = "implementation-mode"
        internal const val PERFORMANCE_MODE = 0
        internal const val COMPATIBLE_MODE = 1
    }

    // The expected final streamState
    private var expectedStreamState: PreviewView.StreamState = PreviewView.StreamState.STREAMING
    private var latchForState: CountDownLatch = CountDownLatch(0)
    var previewView: PreviewView? = null

    private val streamStateObserver = Observer<PreviewView.StreamState> { state ->
        when (state) {
            PreviewView.StreamState.STREAMING -> {
                Log.d(TAG, "PreviewView.StreamState.STREAMING")
                if (expectedStreamState == PreviewView.StreamState.STREAMING) {
                    latchForState.countDown()
                }
            }
            PreviewView.StreamState.IDLE -> {
                Log.d(TAG, "PreviewView.StreamState.IDLE")
                if (expectedStreamState == PreviewView.StreamState.IDLE) {
                    latchForState.countDown()
                }
            }
            else -> {
                Log.e(TAG, "Wrong PreviewView.StreamState! Return IDLE still.")
            }
        }
    }

    @VisibleForTesting
    suspend fun waitForStreamState(expectedState: PreviewView.StreamState): Boolean {
        latchForState = CountDownLatch(1)
        expectedStreamState = expectedState
        runOnUiThread {
            previewView!!.previewStreamState.removeObservers(this)
            previewView!!.previewStreamState.observe(this, streamStateObserver)
        }

        return latchForState.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS)
    }
}
