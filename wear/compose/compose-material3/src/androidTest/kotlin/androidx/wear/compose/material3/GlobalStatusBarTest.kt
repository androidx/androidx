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

package androidx.wear.compose.material3

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlobalStatusBarTest {
    private data class TestCase(
        val sysProp: Boolean,
        val hasMetadata: Boolean?,
        val expected: Boolean,
    )

    @Test
    fun localGlobalStatusBarEnabled_whenPropTrue_metadataTrue_returnsTrue() {
        assertStatusField(TestCase(sysProp = true, hasMetadata = true, expected = true))
    }

    @Test
    fun localGlobalStatusBarEnabled_whenPropTrue_metadataFalse_returnsFalse() {
        assertStatusField(TestCase(sysProp = true, hasMetadata = false, expected = false))
    }

    @Test
    fun localGlobalStatusBarEnabled_whenPropTrue_metadataNull_returnsFalse() {
        assertStatusField(TestCase(sysProp = true, hasMetadata = null, expected = false))
    }

    @Test
    fun localGlobalStatusBarEnabled_whenPropFalse_metadataTrue_returnsFalse() {
        assertStatusField(TestCase(sysProp = false, hasMetadata = true, expected = false))
    }

    @Test
    fun localGlobalStatusBarEnabled_whenPropFalse_metadataFalse_returnsFalse() {
        assertStatusField(TestCase(sysProp = false, hasMetadata = false, expected = false))
    }

    @Test
    fun defaultManifestMetadataChecker_whenPackageManagerAccessThrows_returnsFalse() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val throwingContext =
            object : ContextWrapper(targetContext) {
                override fun getPackageManager(): PackageManager {
                    // Simulate exception when accessing the packageManager inside the execution
                    throw RuntimeException("Fake package manager error during lookup")
                }
            }

        val result =
            GlobalStatusBarDefaults.DefaultManifestMetadataChecker.hasMetadata(
                throwingContext,
                GlobalStatusBarDefaults.METADATA_KEY,
            )

        Assert.assertFalse("Expected checker to return false when PackageManager throws", result)
    }

    private fun assertStatusField(testCase: TestCase) {
        val fakeSysPropReader =
            object : GlobalStatusBarDefaults.SystemPropertyReader {
                override fun getBoolean(name: String, defaultValue: Boolean): Boolean =
                    testCase.sysProp
            }
        val fakeMetadataChecker =
            object : GlobalStatusBarDefaults.ManifestMetadataChecker {
                override fun hasMetadata(context: Context, key: String): Boolean {
                    return testCase.hasMetadata ?: false
                }
            }

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val actual =
            isGlobalStatusBarEnabled(
                targetContext,
                sysPropReader = fakeSysPropReader,
                metadataChecker = fakeMetadataChecker,
            )
        Assert.assertEquals(testCase.expected, actual)
    }
}
