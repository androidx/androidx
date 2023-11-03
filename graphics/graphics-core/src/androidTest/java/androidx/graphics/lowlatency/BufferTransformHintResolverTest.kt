/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.lowlatency

import android.os.Build
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import androidx.graphics.lowlatency.BufferTransformHintResolver.Companion.ORIENTATION_0
import androidx.graphics.lowlatency.BufferTransformHintResolver.Companion.ORIENTATION_180
import androidx.graphics.lowlatency.BufferTransformHintResolver.Companion.ORIENTATION_270
import androidx.graphics.lowlatency.BufferTransformHintResolver.Companion.ORIENTATION_90
import androidx.graphics.lowlatency.BufferTransformHintResolver.Companion.UNKNOWN_TRANSFORM
import androidx.graphics.surface.JniBindings
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_IDENTITY
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_180
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_270
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_90
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
@RunWith(AndroidJUnit4::class)
@SmallTest
internal class BufferTransformHintResolverTest() {

    @Test
    fun testTransformHintFromUnknownOrientation() {
        val transform = BufferTransformHintResolver()
        assertEquals(
            UNKNOWN_TRANSFORM,
            transform.getBufferTransformHintFromInstallOrientation(
                "ORIENTATION_45",
                ROTATION_0
            )
        )
    }

    @Test
    fun testTransformHintOrientation90() {
        with(BufferTransformHintResolver()) {
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_90,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_90, ROTATION_0)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_180,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_90, ROTATION_90)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_270,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_90, ROTATION_180)
            )
            assertEquals(
                BUFFER_TRANSFORM_IDENTITY,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_90, ROTATION_270)
            )
            assertEquals(
                UNKNOWN_TRANSFORM,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_90, -123)
            )
        }
    }

    @Test
    fun testTransformHintOrientation180() {
        with(BufferTransformHintResolver()) {
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_180,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_180, ROTATION_0)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_270,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_180, ROTATION_90)
            )
            assertEquals(
                BUFFER_TRANSFORM_IDENTITY,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_180, ROTATION_180)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_90,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_180, ROTATION_270)
            )
            assertEquals(
                UNKNOWN_TRANSFORM,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_180, -123)
            )
        }
    }

    @Test
    fun testTransformHintOrientation270() {
        with(BufferTransformHintResolver()) {
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_270,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_270, ROTATION_0)
            )
            assertEquals(
                BUFFER_TRANSFORM_IDENTITY,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_270, ROTATION_90)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_90,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_270, ROTATION_180)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_180,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_270, ROTATION_270)
            )
            assertEquals(
                UNKNOWN_TRANSFORM,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_270, -123)
            )
        }
    }

    @Test
    fun testTransformHintOrientation0() {
        with(BufferTransformHintResolver()) {
            assertEquals(
                BUFFER_TRANSFORM_IDENTITY,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_0, ROTATION_0)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_90,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_0, ROTATION_90)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_180,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_0, ROTATION_180)
            )
            assertEquals(
                BUFFER_TRANSFORM_ROTATE_270,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_0, ROTATION_270)
            )
            assertEquals(
                UNKNOWN_TRANSFORM,
                getBufferTransformHintFromInstallOrientation(ORIENTATION_0, -123)
            )
        }
    }

    @Test
    fun testGetDisplayOrientationMethodLinked() {
        try {
            JniBindings.nGetDisplayOrientation()
        } catch (linkError: UnsatisfiedLinkError) {
            fail("Unable to resolve getDisplayOrientation")
        } catch (exception: Exception) {
            // Ignore other errors
        }
    }
}
