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

package androidx.build.playground

import androidx.build.playground.VerifyPlaygroundGradleConfigurationTask.Companion.extractGradleVersion
import com.google.common.truth.Truth.assertThat
import java.util.Properties
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VerifyPlaygroundGradleConfigurationTaskTest {
    @Test
    fun badValues() {
        assertNull(extractGradleVersion("abd"))
        // no /gradle prefix
        assertNull(extractGradleVersion("/not-gradle-1.2.3-all.zip"))
        // no bin or all distribution type
        assertNull(extractGradleVersion("/gradle-1.2.3.zip"))
        // no version
        assertNull(extractGradleVersion("/gradle-all.zip"))
        // no version but has dashes
        assertNull(extractGradleVersion("/gradle--all.zip"))
    }

    @Test
    fun goodValues() {
        // playground
        assertEquals(
            "7.3-rc-2",
            extractGradleVersion(
                "https\\://services.gradle.org/distributions/gradle-7.3-rc-2-all.zip"
            ),
        )
        // androidx
        assertEquals(
            "7.3-rc-1",
            extractGradleVersion("../../../../tools/external/gradle/gradle-7.3-rc-1-bin.zip"),
        )
        assertEquals(
            "7.3",
            extractGradleVersion("https\\://services.gradle.org/distributions/gradle-7.3-all.zip"),
        )
    }

    @Test
    fun testValidateProperties_whenNewOrMismatch() {
        val rootProps =
            Properties().apply {
                put("prop1", "value1")
                put("prop2", "value2")
                put("ignored", "value3")
                put("newProp", "newValue")
            }
        val playgroundProps =
            Properties().apply {
                put("prop1", "value1")
                put("prop2", "mismatch")
                put("ignored", "different")
            }
        val ignoredProperties = setOf("ignored")
        val expectedProperties = mapOf("expected" to "expectedValue")

        val errors =
            validateProperties(rootProps, playgroundProps, expectedProperties, ignoredProperties)

        assertThat(errors).hasSize(2)
        assertThat(errors)
            .containsExactly(
                "prop2: AndroidX value: value2, Playground value: mismatch",
                "newProp: AndroidX value: newValue, Playground value: null",
            )
    }

    @Test
    fun testExpectedPropertiesWhenWrongValue() {
        val rootProps = Properties().apply { put("expected", "expectedValue") }
        val playgroundProps = Properties().apply { put("expected", "wrongValue") }
        val ignoredProperties = emptySet<String>()
        val expectedProperties = mapOf("expected" to "expectedValue")

        val errors =
            validateProperties(rootProps, playgroundProps, expectedProperties, ignoredProperties)
        assertThat(errors).hasSize(1)
        assertThat(errors)
            .containsExactly(
                "Expected playground property 'expected' to have value 'expectedValue' but was 'wrongValue'"
            )
    }

    @Test
    fun testExpectedPropertiesWhenMismatchWhenExpected() {
        val rootProps = Properties().apply { put("expected", "rootValue") }
        val playgroundProps = Properties().apply { put("expected", "expectedValue") }
        val ignoredProperties = emptySet<String>()
        val expectedProperties = mapOf("expected" to "expectedValue")

        val errors =
            validateProperties(rootProps, playgroundProps, expectedProperties, ignoredProperties)
        assertThat(errors).isEmpty()
    }
}
