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

package androidx.camera.extensions.internal

import android.os.Build
import androidx.camera.extensions.internal.util.ExtensionsTestUtil.resetSingleton
import androidx.camera.extensions.internal.util.ExtensionsTestUtil.setTestApiVersion
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = arrayOf("androidx.camera.extensions.internal")
)
class ExtensionVersionMinimumCompatibleTest(private val config: TestConfig) {

    @Before
    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun setUp() {
        ClientVersion.setCurrentVersion(ClientVersion(config.targetVersion))
    }

    @After
    fun tearDown() {
        resetSingleton(ExtensionVersion::class.java, "sExtensionVersion")
    }

    @Test
    fun isMinimumCompatibleVersion() {
        setTestApiVersion(config.targetVersion)
        val version = Version.parse(config.minimumCompatibleVersion)!!
        assertThat(ExtensionVersion.isMinimumCompatibleVersion(version))
            .isEqualTo(config.expectedResult)
    }

    data class TestConfig(
        val targetVersion: String,
        val minimumCompatibleVersion: String,
        val expectedResult: Boolean
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                TestConfig("1.1.0", "1.1.0", true),
                TestConfig("1.1.0", "1.0.0", true),
                TestConfig("1.1.0", "1.2.0", false),

                // Test to ensure the patch version is ignored
                TestConfig("1.1.1", "1.1.0", true),
                TestConfig("1.1.0", "1.1.1", true),
            )
        }
    }
}
