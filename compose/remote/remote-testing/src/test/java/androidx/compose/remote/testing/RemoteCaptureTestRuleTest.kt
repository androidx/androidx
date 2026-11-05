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

package androidx.compose.remote.testing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.action.rememberPendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Config.TARGET_SDK])
class RemoteCaptureTestRuleTest {

    @get:Rule val captureRule = RemoteCaptureTestRule()

    @Test
    fun captureDocument_returnsDocument(): Unit = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val document =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red.rc)) {}
            }

        assertNotNull(document)
    }

    @Test
    fun captureRecordedDocument_returnsBytesAndTheirDecodedDocument(): Unit = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val document =
            captureRule.captureRecordedDocument(context) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red.rc)) {}
            }

        assertThat(document.bytes).isNotEmpty()
        assertThat(document.profile).isSameInstanceAs(RcPlatformProfiles.ANDROIDX)
        assertThat(document.coreDocument.operations).isNotEmpty()
    }

    @Test
    fun captureRecordedDocument_capturesWithTheGivenClock(): Unit = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clock = RecordingClock()

        val document =
            captureRule.captureRecordedDocument(context, clock = clock) {
                LaunchedEffect(Unit) { withFrameNanos {} }
                RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red.rc)) {}
            }

        assertThat(clock.nanoTimeCalls).isGreaterThan(0)
        assertThat(document.coreDocument.clock).isSameInstanceAs(clock)
    }

    @Test
    fun captureDocument_capturesWithTheGivenClock(): Unit = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clock = RecordingClock()

        val document =
            captureRule.captureDocument(context, clock = clock) {
                LaunchedEffect(Unit) { withFrameNanos {} }
                RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red.rc)) {}
            }

        assertThat(clock.nanoTimeCalls).isGreaterThan(0)
        assertThat(document.clock).isSameInstanceAs(clock)
    }

    @Test
    fun captureRecordedDocument_preservesPendingIntents(): Unit = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent("androidx.compose.remote.testing.TEST").setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )

        val document =
            captureRule.captureRecordedDocument(context) {
                val action = rememberPendingIntentAction { pendingIntent }
                RemoteBox(modifier = RemoteModifier.size(100.rdp).clickable(action))
            }

        assertThat(document.pendingIntents[0]).isSameInstanceAs(pendingIntent)
    }

    /**
     * Delegates to [RemoteClock.SYSTEM] and counts the [nanoTime] calls. A capture reads its clock
     * to stamp frames, so content under test awaits a frame.
     */
    private class RecordingClock : RemoteClock by RemoteClock.SYSTEM {
        var nanoTimeCalls = 0

        override fun nanoTime(): Long {
            nanoTimeCalls++
            return RemoteClock.SYSTEM.nanoTime()
        }
    }
}
