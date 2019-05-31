/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.test.helpers.FakeSemanticsTreeInteraction
import com.google.common.truth.Truth
import org.junit.Test

class FindersTests {
    @Test
    fun findByTag_zeroOutOfOne_findsNone() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withSemantics(newNode(
                    SemanticsConfiguration().apply {
                        testTag = "not_myTestTag"
                    }
                ))
        }

        val foundNodes = findByTag("myTestTag")
            .findAllMatching()

        Truth.assertThat(foundNodes).isEmpty()
    }

    @Test
    fun findByTag_oneOutOfTwo_findsOne() {
        val node1 = newNode(SemanticsConfiguration().apply {
            testTag = "myTestTag"
        })
        var node2 = newNode(SemanticsConfiguration().apply {
            testTag = "myTestTag2"
        })

        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withSemantics(node1, node2)
        }

        val foundNodes = findByTag("myTestTag")
            .findAllMatching()

        Truth.assertThat(foundNodes).containsExactly(node1)
    }

    @Test
    fun findByTag_twoOutOfTwo_findsTwo() {
        val node1 = newNode(
            SemanticsConfiguration().apply {
                testTag = "myTestTag"
            }
        )
        var node2 = newNode(
            SemanticsConfiguration().apply {
                testTag = "myTestTag"
            }
        )

        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withSemantics(node1, node2)
        }

        val foundNodes = findByTag("myTestTag")
            .findAllMatching()

        Truth.assertThat(foundNodes).containsExactly(node1, node2)
    }

    private fun newNode(properties: SemanticsConfiguration): SemanticsTreeNode {
        return SemanticsTreeNodeStub(/* data= */ properties)
    }
}