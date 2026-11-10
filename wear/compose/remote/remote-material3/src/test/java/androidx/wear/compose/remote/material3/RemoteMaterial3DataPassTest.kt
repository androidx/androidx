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

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonExtraSmall
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonLarge
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonMedium
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonSmall
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteMaterial3DataPassTest {
    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun CoreDocument.runDataPass() {
        val remoteContext = AndroidRemoteContext()
        remoteContext.useCanvas(Canvas(Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)))
        initializeContext(remoteContext)
    }

    @Test
    fun remoteEdgeButton_initializeContext_doesNotCrashDuringDataPass() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteEdgeButtonExtraSmall()
                RemoteEdgeButtonSmall()
                RemoteEdgeButtonMedium()
                RemoteEdgeButtonLarge()
            }

        doc.runDataPass()
        assertThat(doc.docInfo.mNumberOfOps).isGreaterThan(0)
    }

    @Test
    fun remoteCurvedProgressIndicator_initializeContext_doesNotCrashDuringDataPass() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteCurvedProgressIndicator(progress = 0.5f.rf)
            }

        doc.runDataPass()
        assertThat(doc.docInfo.mNumberOfOps).isGreaterThan(0)
    }

    @Test
    fun remotePageIndicator_initializeContext_doesNotCrashDuringDataPass() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteHorizontalPageIndicator(
                    state = DefaultRemotePageIndicatorState(pageCount = 5, selectedPage = 2.ri)
                )
            }

        doc.runDataPass()
        assertThat(doc.docInfo.mNumberOfOps).isGreaterThan(0)
    }

    @Test
    fun remoteStepper_initializeContext_doesNotCrashDuringDataPass() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteStepper(value = 5f.rf) { RemoteText("5".rs) }
            }

        doc.runDataPass()
        assertThat(doc.docInfo.mNumberOfOps).isGreaterThan(0)
    }
}
