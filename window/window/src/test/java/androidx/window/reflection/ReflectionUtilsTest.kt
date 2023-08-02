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

package androidx.window.reflection

import androidx.window.reflection.ReflectionUtils.doesReturn
import androidx.window.reflection.ReflectionUtils.isPublic
import androidx.window.reflection.ReflectionUtils.validateReflection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test for reflection utilities
 */
class ReflectionUtilsTest {

    private lateinit var classLoader: ClassLoader

    @Before
    fun setup() {
        classLoader = this::class.java.classLoader!!
    }

    @Test
    fun testValidateReflectionSuccess() {
        val result = validateReflection {
            true
        }
        assertTrue(result)
    }

    @Test
    fun testValidateReflectionFail() {
        val result = validateReflection {
            classLoader.loadClass("SomeUnExistedClass.java")
            true
        }
        assertFalse(result)
    }

    @Test
    fun testMethodModifier() {
        val result = validateReflection {
            val testClass = this::class.java
            val privateMethod = testClass.getDeclaredMethod("testMethod").isPublic
            assertFalse(privateMethod)
            val publicMethod = testClass.getDeclaredMethod("testMethodModifier").isPublic
            assertTrue(publicMethod)
            true
        }
        assertTrue(result)
    }

    @Test
    fun testDoesReturn() {
        val result = validateReflection {
            val testClass = this::class.java
            val privateMethod = testClass.getDeclaredMethod("testMethod")
            assertTrue(privateMethod.doesReturn(Int::class.java))
            assertTrue(privateMethod.doesReturn(Int::class))
            assertFalse(privateMethod.doesReturn(Any::class.java))
            assertFalse(privateMethod.doesReturn(Any::class))
            true
        }
        assertTrue(result)
    }

    // method for testing
    @Suppress("unused")
    private fun testMethod(): Int {
        return 0
    }
}
