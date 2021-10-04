/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.graphics.opengl.egl

import android.opengl.EGL14
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EglConfigAttributesTest {

    @Test
    fun testConfig8888() {
        with(EglConfigAttributes8888.attrs) {
            assertTrue(find(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT))
            assertTrue(find(EGL14.EGL_RED_SIZE, 8))
            assertTrue(find(EGL14.EGL_GREEN_SIZE, 8))
            assertTrue(find(EGL14.EGL_BLUE_SIZE, 8))
            assertTrue(find(EGL14.EGL_ALPHA_SIZE, 8))
            assertTrue(find(EGL14.EGL_DEPTH_SIZE, 0))
            assertTrue(find(EGL14.EGL_CONFIG_CAVEAT, EGL14.EGL_NONE))
            assertTrue(find(EGL14.EGL_STENCIL_SIZE, 0))
            assertTrue(find(EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT))
            assertEquals(this[size - 1], EGL14.EGL_NONE)
            assertEquals(19, size)
        }
    }

    @Test
    fun testConfig1010102() {
        with(EglConfigAttributes1010102.attrs) {
            assertTrue(find(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT))
            assertTrue(find(EGL14.EGL_RED_SIZE, 10))
            assertTrue(find(EGL14.EGL_GREEN_SIZE, 10))
            assertTrue(find(EGL14.EGL_BLUE_SIZE, 10))
            assertTrue(find(EGL14.EGL_ALPHA_SIZE, 2))
            assertTrue(find(EGL14.EGL_DEPTH_SIZE, 0))
            assertTrue(find(EGL14.EGL_STENCIL_SIZE, 0))
            assertTrue(find(EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT))
            assertEquals(this[size - 1], EGL14.EGL_NONE)
            assertEquals(17, size)
        }
    }

    @Test
    fun testConfigF16() {
        with(EglConfigAttributesF16.attrs) {
            assertTrue(find(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT))
            assertTrue(find(EglColorComponentTypeExt, EglColorComponentTypeFloatExt))
            assertTrue(find(EGL14.EGL_RED_SIZE, 16))
            assertTrue(find(EGL14.EGL_GREEN_SIZE, 16))
            assertTrue(find(EGL14.EGL_BLUE_SIZE, 16))
            assertTrue(find(EGL14.EGL_ALPHA_SIZE, 16))
            assertTrue(find(EGL14.EGL_DEPTH_SIZE, 0))
            assertTrue(find(EGL14.EGL_STENCIL_SIZE, 0))
            assertTrue(find(EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT))
            assertEquals(this[size - 1], EGL14.EGL_NONE)
            assertEquals(19, size)
        }
    }

    @Test
    fun testInclude() {
        // Verify that custom config that uses an include initially and overwrites
        // individual values is handled appropriately even if the config is technically invalid
        val customConfig = EglConfigAttributes {
            include(EglConfigAttributes8888)
            EGL14.EGL_RED_SIZE to 27
            EglColorComponentTypeExt to EglColorComponentTypeFloatExt
            EGL14.EGL_STENCIL_SIZE to 32
        }

        with(customConfig.attrs) {
            assertTrue(find(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT))
            assertTrue(find(EGL14.EGL_RED_SIZE, 27))
            assertTrue(find(EglColorComponentTypeExt, EglColorComponentTypeFloatExt))
            assertTrue(find(EGL14.EGL_STENCIL_SIZE, 32))
            assertEquals(this[size - 1], EGL14.EGL_NONE)
            assertEquals(21, size)
        }
    }

    private fun IntArray.find(key: Int, value: Int): Boolean {
        // size - 1 to skip trailing EGL_NONE
        for (i in 0 until this.size - 1 step 2) {
            if (this[i] == key) {
                return this[i + 1] == value
            }
        }
        return false
    }
}