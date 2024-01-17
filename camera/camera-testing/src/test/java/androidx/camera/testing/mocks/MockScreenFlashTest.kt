/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.mocks

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ScreenFlashListener
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.camera.testing.impl.mocks.MockScreenFlash.APPLY
import androidx.camera.testing.impl.mocks.MockScreenFlash.CLEAR
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MockScreenFlashTest {
    private val dummyListener = ScreenFlashListener {
        // no-op
    }

    private lateinit var mMockScreenFlash: MockScreenFlash

    @Before
    fun setUp() {
        mMockScreenFlash = MockScreenFlash()
    }

    @Test
    fun getScreenFlashEvents_invocationsRecordedExactlyInSameOrder() {
        mMockScreenFlash.clear()
        mMockScreenFlash.apply(
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(
                ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS
            ),
            dummyListener,
        )
        mMockScreenFlash.clear()

        assertThat(mMockScreenFlash.screenFlashEvents).isEqualTo(listOf(
            CLEAR,
            APPLY,
            CLEAR,
        ))
    }

    @Test
    fun awaitApply_listenerCompletedAutomaticallyByDefault() {
        var isCompleted = false
        val listener = ScreenFlashListener { isCompleted = true }
        mMockScreenFlash.apply(
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(
                ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS
            ),
            listener,
        )

        assertThat(isCompleted).isTrue()
    }

    @Test
    fun awaitClear_returnsFalseWhenClearNotInvoked() {
        assertThat(mMockScreenFlash.awaitClear(3000)).isFalse()
    }

    @Test
    fun awaitClear_returnsTrueWhenClearInvokedEarlier() {
        mMockScreenFlash.clear()
        assertThat(mMockScreenFlash.awaitClear(3000)).isTrue()
    }

    @SuppressLint("BanThreadSleep")
    @Test
    fun awaitClear_returnsTrueWhenClearInvokedLater() {
        Thread({
            try {
                // ensure clearScreenFlashUi is not invoked immediately, but after some delay and
                // from another thread
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            mMockScreenFlash.clear()
        }, "test thread").start()

        assertThat(mMockScreenFlash.awaitClear(3000)).isTrue()
    }
}
