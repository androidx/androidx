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

package androidx.xr.compose.subspace.semantics

import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubspaceSemanticsConfigurationTest {

    @Test
    fun contains_keyIsPresent_returnsTrue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        config.testTag = "presentTag"
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: Boolean = configuration.contains(key = SemanticsProperties.TestTag)

        assertTrue(result)
    }

    @Test
    fun contains_keyIsMissing_returnsFalse() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: Boolean = configuration.contains(key = SemanticsProperties.TestTag)

        assertFalse(result)
    }

    @Test
    fun getOrElseNullable_keyIsPresent_returnsValue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        config.testTag = "presentTag"
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String? =
            configuration.getOrElseNullable(
                key = SemanticsProperties.TestTag,
                defaultValue = { null },
            )

        assertThat(result).isEqualTo("presentTag")
    }

    @Test
    fun getOrElseNullable_keyIsMissing_returnsDefaultValue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String? =
            configuration.getOrElseNullable(
                key = SemanticsProperties.TestTag,
                defaultValue = { "defaultNullableTag" },
            )

        assertThat(result).isEqualTo("defaultNullableTag")
    }

    @Test
    fun getOrElseNullable_keyIsMissing_returnsNullDefaultValue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String? =
            configuration.getOrElseNullable(
                key = SemanticsProperties.TestTag,
                defaultValue = { null },
            )

        assertNull(result)
    }

    @Test
    fun getOrElse_keyIsPresent_returnsValue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        config.testTag = "presentTag"
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String =
            configuration.getOrElse(
                key = SemanticsProperties.TestTag,
                defaultValue = { "defaultTag" },
            )

        assertThat(result).isEqualTo("presentTag")
    }

    @Test
    fun getOrElse_keyIsMissing_returnsDefaultValue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String =
            configuration.getOrElse(
                key = SemanticsProperties.TestTag,
                defaultValue = { "defaultTag" },
            )

        assertThat(result).isEqualTo("defaultTag")
    }

    @Test
    fun get_keyIsPresent_returnsValue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        config.testTag = "presentTag"
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String = configuration[SemanticsProperties.TestTag]

        assertThat(result).isEqualTo("presentTag")
    }

    @Test
    fun get_keyIsMissing_throwsException() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val exception: IllegalStateException =
            assertFailsWith<IllegalStateException> {
                val unused: String = configuration[SemanticsProperties.TestTag]
            }

        assertThat(exception).hasMessageThat().contains("Key not present")
    }

    @Test
    fun getOrNull_keyIsPresent_returnsValue() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        config.testTag = "presentTag"
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String? = configuration.getOrNull(key = SemanticsProperties.TestTag)

        assertThat(result).isEqualTo("presentTag")
    }

    @Test
    fun getOrNull_keyIsMissing_returnsNull() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val configuration: SubspaceSemanticsConfiguration =
            SubspaceSemanticsConfiguration(config = config)

        val result: String? = configuration.getOrNull(key = SemanticsProperties.TestTag)

        assertNull(result)
    }

    @Test
    fun set_updatesUnderlyingConfiguration() {
        val config = SemanticsConfiguration()
        val subspaceConfig = SubspaceSemanticsConfiguration(config)
        val key = SemanticsPropertyKey<String>("TestKey")

        subspaceConfig[key] = "TestValue"

        assertTrue(subspaceConfig.contains(key))
        assertThat(subspaceConfig[key]).isEqualTo("TestValue")
        assertThat(subspaceConfig.getOrNull(key)).isEqualTo("TestValue")
        assertThat(subspaceConfig.getOrElse(key) { "Default" }).isEqualTo("TestValue")
    }

    @Test
    fun iterator_returnsExpectedEntries() {
        val config = SemanticsConfiguration()
        val subspaceConfig = SubspaceSemanticsConfiguration(config)
        val key1 = SemanticsPropertyKey<String>("Key1")
        val key2 = SemanticsPropertyKey<Int>("Key2")

        subspaceConfig[key1] = "Value1"
        subspaceConfig[key2] = 42

        val entries = subspaceConfig.toList()

        assertThat(entries).hasSize(2)
        assertThat(entries.map { it.key.name }).containsExactly("Key1", "Key2")
        assertThat(entries.map { it.value }).containsExactly("Value1", 42)
    }
}
