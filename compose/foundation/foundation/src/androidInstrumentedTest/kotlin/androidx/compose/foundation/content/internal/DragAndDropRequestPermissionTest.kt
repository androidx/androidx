/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.content.internal

import android.net.Uri
import android.view.DragEvent
import androidx.compose.foundation.TestActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.input.internal.DragAndDropTestUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

@SmallTest
class DragAndDropRequestPermissionTest {

    @Suppress("DEPRECATION") @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private lateinit var testNode: TestNode

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun asksPermission_ifAllRequirementsAreMet() {
        // setup
        rule.setContent { Box(Modifier.then(TestElement { testNode = it })) }
        val event =
            DragAndDropEvent(
                DragAndDropTestUtils.makeImageDragEvent(
                    DragEvent.ACTION_DROP,
                    Uri.parse("content://com.example/content.png")
                )
            )

        // act
        testNode.dragAndDropRequestPermission(event)

        // assert
        Truth.assertThat(rule.activity.requestedDragAndDropPermissions).isNotEmpty()
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun doesNotAskPermission_ifNoContentUri() {
        // setup
        rule.setContent { Box(Modifier.then(TestElement { testNode = it })) }
        val event =
            DragAndDropEvent(
                DragAndDropTestUtils.makeImageDragEvent(
                    DragEvent.ACTION_DROP,
                    Uri.parse("file://com.example/content.png")
                )
            )

        // act
        testNode.dragAndDropRequestPermission(event)

        // assert
        Truth.assertThat(rule.activity.requestedDragAndDropPermissions).isEmpty()
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun doesNotAskPermission_ifNodeIsDetached() {
        // setup
        var toggle by mutableStateOf(true)
        rule.setContent {
            if (toggle) {
                Box(Modifier.then(TestElement { testNode = it }))
            }
        }
        val event =
            DragAndDropEvent(
                DragAndDropTestUtils.makeImageDragEvent(
                    DragEvent.ACTION_DROP,
                    Uri.parse("file://com.example/content.png")
                )
            )

        toggle = false

        // act
        testNode.dragAndDropRequestPermission(event)

        // assert
        Truth.assertThat(rule.activity.requestedDragAndDropPermissions).isEmpty()
    }

    private data class TestElement(val onNode: (TestNode) -> Unit) :
        ModifierNodeElement<TestNode>() {
        override fun create(): TestNode = TestNode(onNode)

        override fun update(node: TestNode) {
            node.onNode = onNode
        }
    }

    private class TestNode(var onNode: (TestNode) -> Unit) :
        Modifier.Node(), CompositionLocalConsumerModifierNode {

        override fun onAttach() {
            onNode(this)
        }
    }
}
