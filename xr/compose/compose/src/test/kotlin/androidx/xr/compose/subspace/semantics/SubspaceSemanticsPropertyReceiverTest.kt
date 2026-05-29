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
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubspaceSemanticsPropertyReceiverTest {

    @Test
    fun set_assignValue_setsOnDelegate() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val receiver: SubspaceSemanticsPropertyReceiverImpl =
            SubspaceSemanticsPropertyReceiverImpl(delegate = config)
        val customKey: SemanticsPropertyKey<String> = SemanticsPropertyKey(name = "customKey")

        receiver[customKey] = "customValue"

        assertThat(config.getOrNull(key = customKey)).isEqualTo("customValue")
    }

    @Test
    fun testTag_assignValue_setsOnDelegate() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val receiver: SubspaceSemanticsPropertyReceiverImpl =
            SubspaceSemanticsPropertyReceiverImpl(delegate = config)

        receiver.testTag = "testTagValue"

        assertThat(config.getOrNull(key = SemanticsProperties.TestTag)).isEqualTo("testTagValue")
    }

    @Test
    fun contentDescription_assignValue_setsOnDelegate() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val receiver: SubspaceSemanticsPropertyReceiverImpl =
            SubspaceSemanticsPropertyReceiverImpl(delegate = config)

        receiver.contentDescription = "contentDescriptionValue"

        assertThat(config.getOrNull(key = SemanticsProperties.ContentDescription))
            .containsExactly("contentDescriptionValue")
    }

    @Test
    fun testTag_getValue_throwsException() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val receiver: SubspaceSemanticsPropertyReceiverImpl =
            SubspaceSemanticsPropertyReceiverImpl(delegate = config)

        val exception: UnsupportedOperationException =
            assertFailsWith<UnsupportedOperationException> {
                val unused: String = receiver.testTag
            }

        assertThat(exception).hasMessageThat().contains("You cannot read semantics properties")
    }

    @Test
    fun contentDescription_getValue_throwsException() {
        val config: SemanticsConfiguration = SemanticsConfiguration()
        val receiver: SubspaceSemanticsPropertyReceiverImpl =
            SubspaceSemanticsPropertyReceiverImpl(delegate = config)

        val exception: UnsupportedOperationException =
            assertFailsWith<UnsupportedOperationException> {
                val unused: String = receiver.contentDescription
            }

        assertThat(exception).hasMessageThat().contains("You cannot read semantics properties")
    }
}
