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
import androidx.camera.core.ImageCapture.ScreenFlashUiCompleter
import androidx.camera.testing.impl.mocks.MockScreenFlashUiControl
import androidx.camera.testing.impl.mocks.MockScreenFlashUiControl.APPLY_SCREEN_FLASH
import androidx.camera.testing.impl.mocks.MockScreenFlashUiControl.CLEAR_SCREEN_FLASH
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MockScreenFlashUiControlTest {
    private val dummyCompleter = ScreenFlashUiCompleter { }

    private lateinit var mMockScreenFlashUiControl: MockScreenFlashUiControl

    @Before
    fun setUp() {
        mMockScreenFlashUiControl = MockScreenFlashUiControl()
    }

    @Test
    fun getScreenFlashUiEvents_invocationsRecordedExactlyInSameOrder() {
        mMockScreenFlashUiControl.clearScreenFlashUi()
        mMockScreenFlashUiControl.applyScreenFlashUi(dummyCompleter)
        mMockScreenFlashUiControl.clearScreenFlashUi()

        assertThat(mMockScreenFlashUiControl.screenFlashUiEvents).isEqualTo(listOf(
            CLEAR_SCREEN_FLASH,
            APPLY_SCREEN_FLASH,
            CLEAR_SCREEN_FLASH,
        ))
    }

    @Test
    fun awaitScreenFlashUiApply_completerCompletedAutomaticallyByDefault() {
        var isCompleted = false
        val completer = ScreenFlashUiCompleter { isCompleted = true }
        mMockScreenFlashUiControl.applyScreenFlashUi(completer)

        assertThat(isCompleted).isTrue()
    }

    @Test
    fun awaitScreenFlashUiClear_returnsFalseWhenClearScreenFlashUiNotInvoked() {
        assertThat(mMockScreenFlashUiControl.awaitScreenFlashUiClear(3000)).isFalse()
    }

    @Test
    fun awaitScreenFlashUiClear_returnsTrueWhenClearScreenFlashUiInvokedEarlier() {
        mMockScreenFlashUiControl.clearScreenFlashUi()
        assertThat(mMockScreenFlashUiControl.awaitScreenFlashUiClear(3000)).isTrue()
    }

    @SuppressLint("BanThreadSleep")
    @Test
    fun awaitScreenFlashUiClear_returnsTrueWhenClearScreenFlashUiInvokedLater() {
        Thread({
            try {
                // ensure clearScreenFlashUi is not invoked immediately, but after some delay and
                // from another thread
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            mMockScreenFlashUiControl.clearScreenFlashUi()
        }, "test thread").start()

        assertThat(mMockScreenFlashUiControl.awaitScreenFlashUiClear(3000)).isTrue()
    }
}
