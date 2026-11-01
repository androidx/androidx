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

package androidx.pdf.view

import android.content.ComponentCallbacks2
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class BackgroundBitmapControllerTest {
    private var releaseCount = 0
    private var restoreCount = 0

    private val controller =
        BackgroundBitmapController(
            releaseBitmaps = { releaseCount++ },
            restoreBitmaps = { restoreCount++ },
        )

    @Test
    fun trimBackground_releasesBitmaps() {
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

        assertThat(releaseCount).isEqualTo(1)
        assertThat(controller.areBitmapsReleased).isTrue()
    }

    @Test
    fun trimComplete_releasesBitmaps() {
        @Suppress("DEPRECATION") controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

        assertThat(releaseCount).isEqualTo(1)
    }

    @Test
    fun trimUiHidden_doesNotRelease() {
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        controller.onWindowVisibilityChanged(isVisible = true)

        assertThat(releaseCount).isEqualTo(0)
        assertThat(restoreCount).isEqualTo(0)
    }

    @Test
    fun trimLevelBelowUiHidden_doesNotRelease() {
        @Suppress("DEPRECATION")
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)

        assertThat(releaseCount).isEqualTo(0)
    }

    @Test
    fun repeatedTrimSignals_releaseOnce() {
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        @Suppress("DEPRECATION") controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

        assertThat(releaseCount).isEqualTo(1)
    }

    @Test
    fun windowVisibleAfterRelease_restoresBitmapsOnce() {
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

        controller.onWindowVisibilityChanged(isVisible = true)
        controller.onWindowVisibilityChanged(isVisible = false)
        controller.onWindowVisibilityChanged(isVisible = true)

        assertThat(restoreCount).isEqualTo(1)
        assertThat(controller.areBitmapsReleased).isFalse()
    }

    @Test
    fun windowVisibilityChanges_withoutTrimSignal_doNotReleaseOrRestore() {
        controller.onWindowVisibilityChanged(isVisible = true)
        controller.onWindowVisibilityChanged(isVisible = false)
        controller.onWindowVisibilityChanged(isVisible = true)

        assertThat(releaseCount).isEqualTo(0)
        assertThat(restoreCount).isEqualTo(0)
    }

    @Test
    fun releaseAfterRestore_releasesAgain() {
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        controller.onWindowVisibilityChanged(isVisible = true)
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

        assertThat(releaseCount).isEqualTo(2)
        assertThat(restoreCount).isEqualTo(1)
    }

    @Test
    fun reset_afterRelease_doesNotRestoreOnNextVisible() {
        controller.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

        // PdfView re-requests bitmaps itself when re-attached, so no explicit restore is needed
        controller.reset()
        controller.onWindowVisibilityChanged(isVisible = true)

        assertThat(restoreCount).isEqualTo(0)
        assertThat(controller.areBitmapsReleased).isFalse()
    }
}
