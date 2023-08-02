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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class EGLVersionTest {

    @Test
    fun testDestructuringComponents() {
        val (major, minor) = EGLVersion(8, 3)
        assertEquals(8, major)
        assertEquals(3, minor)
    }

    @Test
    fun testEquals() {
        assertEquals(EGLVersion(2, 9), EGLVersion(2, 9))
    }

    @Test
    fun testToString() {
        assertEquals("EGL version 5.9", EGLVersion(5, 9).toString())
    }

    @Test
    fun testHashCode() {
        val hashCode = 31 * 8 + 4
        assertEquals(hashCode, EGLVersion(8, 4).hashCode())
    }
}
