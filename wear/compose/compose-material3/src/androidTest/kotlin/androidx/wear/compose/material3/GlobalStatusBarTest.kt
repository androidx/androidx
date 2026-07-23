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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlobalStatusBarTest {
    @get:Rule val composeTestRule = createComposeRule()

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

        val checker = DefaultManifestMetadataChecker()
        val result = checker.hasMetadata(throwingContext, GlobalStatusBarDefaults.METADATA_KEY)

        Assert.assertFalse("Expected checker to return false when PackageManager throws", result)
    }

    @Test
    fun localGlobalStatusBarEnabled_canBeOverridden() {
        var actual: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalGlobalStatusBarEnabled provides true) {
                actual = LocalGlobalStatusBarEnabled.current
            }
        }
        Assert.assertEquals(true, actual)
    }

    private fun assertStatusField(testCase: TestCase) {
        val fakeSysPropReader =
            object : SystemPropertyReader {
                override fun getBoolean(name: String, defaultValue: Boolean): Boolean =
                    testCase.sysProp
            }
        val fakeMetadataChecker =
            object : ManifestMetadataChecker {
                override fun hasMetadata(context: Context, key: String): Boolean {
                    return testCase.hasMetadata ?: false
                }
            }

        val testChecker =
            GlobalStatusBarChecker(
                sysPropReader = fakeSysPropReader,
                metadataChecker = fakeMetadataChecker,
            )

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val actual = testChecker.isEnabled(targetContext)
        Assert.assertEquals(testCase.expected, actual)
    }
}
