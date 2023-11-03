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

package androidx.glance.testing

import androidx.glance.testing.unit.GlanceMappedNode
import androidx.glance.text.EmittableText
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssertionErrorMessagesTest {
    @Test
    fun countMismatch_expectedNone() {
        val resultMessage = buildErrorMessageWithReason(
            errorMessageOnFail = "Failed assert",
            reason = buildErrorReasonForCountMismatch(
                matcherDescription = "testTag = 'my-node'",
                expectedCount = 0,
                actualCount = 1
            )
        )

        assertThat(resultMessage).isEqualTo(
            "Failed assert" +
                "\nReason: Did not expect any node matching condition: testTag = 'my-node'" +
                ", but found '1'"
        )
    }

    @Test
    fun countMismatch_expectedButFoundNone() {
        val resultMessage = buildErrorMessageWithReason(
            errorMessageOnFail = "Failed assert",
            reason = buildErrorReasonForCountMismatch(
                matcherDescription = "testTag = 'my-node'",
                expectedCount = 2,
                actualCount = 0
            )
        )

        assertThat(resultMessage).isEqualTo(
            "Failed assert" +
                "\nReason: Expected '2' node(s) matching condition: testTag = 'my-node'" +
                ", but found '0'"
        )
    }

    @Test
    fun generalErrorMessage_singleNode() {
        val node = GlanceMappedNode(
            EmittableText().also { it.text = "test text" }
        )

        val resultMessage = buildGeneralErrorMessage(
            errorMessage = "Failed to match the condition: (testTag = 'my-node')",
            node = node
        )

        assertThat(resultMessage).isEqualTo(
            "Failed to match the condition: (testTag = 'my-node')" +
                "\nGlance Node: ${node.toDebugString()}"
        )
    }

    @Test
    fun generalErrorMessage_multipleNodes() {
        val node1 = GlanceMappedNode(
            EmittableText().also { it.text = "text1" }
        )
        val node2 = GlanceMappedNode(
            EmittableText().also { it.text = "text2" }
        )

        val resultMessage = buildGeneralErrorMessage(
            errorMessage = "Failed to match the condition: (testTag = 'my-node')",
            nodes = listOf(node1, node2)
        )

        assertThat(resultMessage).isEqualTo(
            "Failed to match the condition: (testTag = 'my-node')" +
                "\nFound 2 node(s) that don't match." +
                "\nNon-matching Glance Node #1: ${node1.toDebugString()}" +
                "\nNon-matching Glance Node #2: ${node2.toDebugString()}"
        )
    }
}
