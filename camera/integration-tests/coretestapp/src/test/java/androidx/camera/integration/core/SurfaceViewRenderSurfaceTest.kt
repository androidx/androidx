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

package androidx.camera.integration.core

import android.app.Activity
import android.os.Build
import android.util.Pair
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowDisplay

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class SurfaceViewRenderSurfaceTest {

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun releaseSurfaceInfoAfterDetach_defersReparentAndReleaseUntilDetachFutureCompletes() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val surfaceView = SurfaceView(activity)
        activity.setContentView(surfaceView)

        val pair: Pair<Surface, SurfaceControl> =
            checkNotNull(
                SurfaceViewRenderSurface.createSurfaceControlSurfaceAndReparent(
                    surfaceView,
                    /* format= */ 1,
                    /* width= */ 640,
                    /* height= */ 480,
                )
            )
        assertThat(pair.first.isValid).isTrue()
        assertThat(pair.second.isValid).isTrue()

        lateinit var detachCompleter: CallbackToFutureAdapter.Completer<Void>
        val detachFuture =
            CallbackToFutureAdapter.getFuture<Void> { completer ->
                detachCompleter = completer
                "TestDetachFuture"
            }

        SurfaceViewRenderSurface.releaseSurfaceInfoAfterDetach(pair, detachFuture)

        // While OpenGLRenderer detach is still pending, Surface and SurfaceControl stay valid.
        assertThat(pair.first.isValid).isTrue()
        assertThat(pair.second.isValid).isTrue()

        // Completing the detach future unparents, closes the transaction, and releases both.
        detachCompleter.set(null)
        assertThat(pair.first.isValid).isFalse()
        assertThat(pair.second.isValid).isFalse()
    }

    @Test
    fun resolveSurfaceRotation_defaultsToZeroWhenDisplayIsNull() {
        assertThat(SurfaceViewRenderSurface.resolveSurfaceRotation(null)).isEqualTo(0)

        val defaultDisplay = ShadowDisplay.getDefaultDisplay()
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        try {
            assertThat(SurfaceViewRenderSurface.resolveSurfaceRotation(defaultDisplay))
                .isEqualTo(90)
        } finally {
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        }
    }
}
