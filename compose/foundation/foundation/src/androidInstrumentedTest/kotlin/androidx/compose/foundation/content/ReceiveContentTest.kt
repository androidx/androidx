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

package androidx.compose.foundation.content

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TestActivity
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.content.internal.getReceiveContentConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.firstUriOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
class ReceiveContentTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun receiveContentConfiguration_isMergedBottomToTop() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = null
        val listenerCalls = mutableListOf<Int>()
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(setOf(MediaType.Video)) { listenerCalls += 3; it }
                .receiveContent(setOf(MediaType.Audio)) { listenerCalls += 2; it }
                .receiveContent(setOf(MediaType.Text)) { listenerCalls += 1; it }
                .then(TestElement {
                    calculatedReceiveContent = it.getReceiveContentConfiguration()
                    calculatedReceiveContent
                        ?.receiveContentListener
                        ?.onReceive(TransferableContent(createClipData()))
                })
            )
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent?.hintMediaTypes).isNotNull()
            assertThat(calculatedReceiveContent?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Audio, MediaType.Text)
            )
            assertThat(listenerCalls).isEqualTo(listOf(1, 2, 3))
        }
    }

    @Test
    fun onReceiveCallbacks_passTheReturnedValue_toParentNode() {
        var videoReceived: TransferableContent? = null
        var audioReceived: TransferableContent? = null
        var textReceived: TransferableContent? = null
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(setOf(MediaType.Video)) {
                    videoReceived = it
                    val t = it.consumeEach {
                        it.uri
                            ?.toString()
                            ?.contains("video") ?: false
                    }
                    t
                }
                .receiveContent(setOf(MediaType.Audio)) {
                    audioReceived = it
                    val t = it.consumeEach {
                        it.uri
                            ?.toString()
                            ?.contains("audio") ?: false
                    }
                    t
                }
                .receiveContent(setOf(MediaType.Text)) {
                    textReceived = it
                    val t = it.consumeEach { it.text != null }
                    t
                }
                .then(TestElement {
                    it.getReceiveContentConfiguration()
                        ?.receiveContentListener
                        ?.onReceive(TransferableContent(createClipData {
                            addText()
                            addUri(Uri.parse("content://video"), "video/mp4")
                            addUri(Uri.parse("content://audio"), "audio/ogg")
                        }))
                })
            )
        }

        rule.runOnIdle {
            assertClipData(videoReceived!!.clipEntry.clipData).isEqualToClipData(createClipData {
                addUri(Uri.parse("content://video"), "video/mp4")
            }, ignoreClipDescription = true)
            assertClipData(audioReceived!!.clipEntry.clipData).isEqualToClipData(createClipData {
                addUri(Uri.parse("content://video"), "video/mp4")
                addUri(Uri.parse("content://audio"), "audio/ogg")
            }, ignoreClipDescription = true)
            assertClipData(textReceived!!.clipEntry.clipData).isEqualToClipData(createClipData {
                addText()
                addUri(Uri.parse("content://video"), "video/mp4")
                addUri(Uri.parse("content://audio"), "audio/ogg")
            }, ignoreClipDescription = true)
        }
    }

    @Test
    fun receiveContentConfiguration_returnsNullIfNotDefined() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = ReceiveContentConfiguration(
            emptySet(),
            ReceiveContentListener { null }
        )
        rule.setContent {
            Box(
                modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.getReceiveContentConfiguration()
                })
            )
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent).isNull()
        }
    }

    @Test
    fun receiveContentConfiguration_returnsNullIfDefined_atSiblingNode() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = ReceiveContentConfiguration(
            emptySet(),
            ReceiveContentListener { null }
        )
        rule.setContent {
            Box {
                Box(modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.getReceiveContentConfiguration()
                }))
                Box(modifier = Modifier.receiveContent(setOf(MediaType.Text)) { it })
            }
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent).isNull()
        }
    }

    @Test
    fun receiveContentConfiguration_returnsNullIfDefined_atChildNode() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = ReceiveContentConfiguration(
            emptySet(),
            ReceiveContentListener { null }
        )
        rule.setContent {
            Box(
                modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.getReceiveContentConfiguration()
                })
            ) {
                Box(modifier = Modifier.receiveContent(setOf(MediaType.Text)) { it })
            }
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent).isNull()
        }
    }

    @Test
    fun receiveContentConfiguration_emptyMediaTypeSet_returnsMediaTypeAll() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = ReceiveContentConfiguration(
            emptySet(),
            ReceiveContentListener { null }
        )
        rule.setContent {
            Box(modifier = Modifier.receiveContent(emptySet()) { it }) {
                Box(modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.getReceiveContentConfiguration()
                }))
            }
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent).isNotNull()
            assertThat(calculatedReceiveContent?.hintMediaTypes).isEqualTo(setOf(MediaType.All))
        }
    }

    @Test
    fun detachedReceiveContent_disappearsFromMergedConfiguration() {
        var getReceiveContentConfiguration: (() -> ReceiveContentConfiguration?)? = null
        var attached by mutableStateOf(true)
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(setOf(MediaType.Video)) { it }
                .then(if (attached) {
                    Modifier.receiveContent(setOf(MediaType.Audio)) { it }
                } else {
                    Modifier
                })
                .receiveContent(setOf(MediaType.Text)) { it }
                .then(TestElement {
                    getReceiveContentConfiguration = {
                        it.getReceiveContentConfiguration()
                    }
                })
            )
        }

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Audio, MediaType.Text)
            )
        }

        attached = false

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Text)
            )
        }
    }

    @Test
    fun laterAttachedReceiveContent_appearsInMergedConfiguration() {
        var getReceiveContentConfiguration: (() -> ReceiveContentConfiguration?)? = null
        var attached by mutableStateOf(false)
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(setOf(MediaType.Video)) { it }
                .then(if (attached) {
                    Modifier.receiveContent(setOf(MediaType.Audio)) { it }
                } else {
                    Modifier
                })
                .receiveContent(setOf(MediaType.Text)) { it }
                .then(TestElement {
                    getReceiveContentConfiguration = {
                        it.getReceiveContentConfiguration()
                    }
                })
            )
        }

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Text)
            )
        }

        attached = true

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Audio, MediaType.Text)
            )
        }
    }

    @Test
    fun changingParentReceiveContent_appearsInMergedConfiguration() {
        var getReceiveContentConfiguration: (() -> ReceiveContentConfiguration?)? = null
        var topMediaTypes by mutableStateOf(setOf(MediaType.Video))
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(topMediaTypes) { it }
                .receiveContent(setOf(MediaType.Text)) { it }
                .then(TestElement {
                    getReceiveContentConfiguration = {
                        it.getReceiveContentConfiguration()
                    }
                })
            )
        }

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Text)
            )
        }

        topMediaTypes = setOf(MediaType.Audio, MediaType.Video)

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Audio, MediaType.Text)
            )
        }
    }

    @Test
    fun changingCurrentReceiveContent_appearsInMergedConfiguration() {
        var getReceiveContentConfiguration: (() -> ReceiveContentConfiguration?)? = null
        var currentMediaTypes by mutableStateOf(setOf(MediaType.Video))
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(setOf(MediaType.Image)) { it }
                .receiveContent(currentMediaTypes) { it }
                .then(TestElement {
                    getReceiveContentConfiguration = {
                        it.getReceiveContentConfiguration()
                    }
                })
            )
        }

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Image)
            )
        }

        currentMediaTypes = setOf(MediaType.Text, MediaType.Video)

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.hintMediaTypes).containsExactlyElementsIn(
                setOf(MediaType.Video, MediaType.Image, MediaType.Text)
            )
        }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun dragAndDrop_dropImplicitlyRequestsPermissions_once() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box(modifier = Modifier
                .size(200.dp)
                .receiveContent(setOf(MediaType.Video)) { it }
                .size(100.dp)
                .receiveContent(setOf(MediaType.Audio)) { it }
                .size(50.dp)
                .receiveContent(setOf(MediaType.Text)) { it }
            )
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(
                Offset(25.dp.toPx(), 25.dp.toPx()),
                draggingUri
            )
            drop()
        }

        rule.runOnIdle {
            val requests = rule.activity.requestedDragAndDropPermissions
            assertThat(requests.size).isEqualTo(1)
            assertThat(requests.first().clipData.getItemAt(0).uri).isEqualTo(draggingUri)
        }
    }

    @Test
    fun dragAndDropOnSingleNodeTriggersOnReceive() {
        lateinit var view: View
        var transferableContent: TransferableContent? = null
        rule.setContent {
            view = LocalView.current
            Box(modifier = Modifier
                .size(100.dp)
                .receiveContent(setOf(MediaType.Image)) {
                    transferableContent = it
                    null // consume all
                })
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(
                Offset(50.dp.toPx(), 50.dp.toPx()),
                draggingUri
            )
            drop()
        }

        rule.runOnIdle {
            assertThat(transferableContent).isNotNull()
            assertThat(transferableContent?.clipEntry?.firstUriOrNull())
                .isEqualTo(draggingUri)
            assertThat(transferableContent?.source)
                .isEqualTo(TransferableContent.Source.DragAndDrop)
        }
    }

    @Test
    fun dragAndDropOnSingleNode_withNotIncludedHintMediaType_triggersOnReceive() {
        lateinit var view: View
        var transferableContent: TransferableContent? = null
        rule.setContent {
            view = LocalView.current
            Box(modifier = Modifier
                .size(100.dp)
                .receiveContent(setOf(MediaType.Audio)) {
                    transferableContent = it
                    null // consume all
                })
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(
                Offset(50.dp.toPx(), 50.dp.toPx()),
                draggingUri
            )
            drop()
        }

        rule.runOnIdle {
            assertThat(transferableContent).isNotNull()
            assertThat(transferableContent?.clipEntry?.firstUriOrNull())
                .isEqualTo(draggingUri)
            assertThat(transferableContent?.source)
                .isEqualTo(TransferableContent.Source.DragAndDrop)
        }
    }

    @Test
    fun dragAndDropOnNestedNode_triggersOnReceive_onAllNodes() {
        lateinit var view: View
        var childTransferableContent: TransferableContent? = null
        var parentTransferableContent: TransferableContent? = null
        rule.setContent {
            view = LocalView.current
            Box(modifier = Modifier
                .size(200.dp)
                .receiveContent(setOf(MediaType.All)) {
                    parentTransferableContent = it
                    null
                }) {
                Box(modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
                    .receiveContent(setOf(MediaType.Image)) {
                        childTransferableContent = it
                        it // don't consume anything
                    })
            }
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(
                Offset(100.dp.toPx(), 100.dp.toPx()),
                draggingUri
            )
            drop()
        }

        rule.runOnIdle {
            assertThat(parentTransferableContent).isNotNull()
            assertThat(parentTransferableContent?.clipEntry?.firstUriOrNull())
                .isEqualTo(draggingUri)
            assertThat(parentTransferableContent?.source)
                .isEqualTo(TransferableContent.Source.DragAndDrop)

            assertThat(childTransferableContent).isNotNull()
            assertThat(childTransferableContent?.clipEntry?.firstUriOrNull())
                .isEqualTo(draggingUri)
            assertThat(childTransferableContent?.source)
                .isEqualTo(TransferableContent.Source.DragAndDrop)
        }
    }

    @Test
    fun dragAndDropOnNestedNode_triggersOnReceive_onHoveringNodes() {
        lateinit var view: View
        var childTransferableContent: TransferableContent? = null
        var parentTransferableContent: TransferableContent? = null
        var grandParentTransferableContent: TransferableContent? = null
        rule.setContent {
            view = LocalView.current
            Box(modifier = Modifier
                .size(200.dp)
                .receiveContent(setOf(MediaType.All)) {
                    grandParentTransferableContent = it
                    null
                }) {
                Box(modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
                    .receiveContent(setOf(MediaType.Image)) {
                        parentTransferableContent = it
                        it // don't consume anything
                    }) {
                    Box(modifier = Modifier
                        .align(Alignment.Center)
                        .size(50.dp)
                        .receiveContent(setOf(MediaType.Text)) {
                            childTransferableContent = it
                            it // don't consume anything
                        })
                }
            }
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(
                Offset(60.dp.toPx(), 60.dp.toPx()),
                draggingUri
            )
            drop()
        }

        rule.runOnIdle {
            assertThat(grandParentTransferableContent).isNotNull()
            assertThat(parentTransferableContent).isNotNull()
            assertThat(childTransferableContent).isNull() // child was not in hover region
        }
    }

    @Test
    fun dragAndDrop_enterExitCallbacks_singleNode() {
        lateinit var view: View
        val calls = mutableListOf<String>()
        rule.setContent {
            view = LocalView.current
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                        override fun onDragEnter() {
                            calls += "enter"
                        }

                        override fun onDragExit() {
                            calls += "exit"
                        }

                        override fun onReceive(
                            transferableContent: TransferableContent
                        ): TransferableContent? {
                            calls += "receive"
                            return null
                        }
                    })
            )
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(Offset(125.dp.toPx(), 125.dp.toPx()), draggingUri)
            // enter
            drag(Offset(90.dp.toPx(), 90.dp.toPx()), draggingUri)
            // moves
            drag(Offset(50.dp.toPx(), 50.dp.toPx()), draggingUri)
            // exits
            drag(Offset(101.dp.toPx(), 50.dp.toPx()), draggingUri)
            // enters again
            drag(Offset(99.dp.toPx(), 50.dp.toPx()), draggingUri)
            drop()
        }

        rule.runOnIdle {
            assertThat(calls).isEqualTo(listOf("enter", "exit", "enter", "receive"))
        }
    }

    @Test
    fun dragAndDrop_enterExitCallbacks_nestedNodes() {
        lateinit var view: View
        val calls = mutableListOf<String>()
        rule.setContent {
            view = LocalView.current
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                        override fun onDragEnter() {
                            calls += "enter-1"
                        }

                        override fun onDragExit() {
                            calls += "exit-1"
                        }

                        override fun onReceive(
                            transferableContent: TransferableContent
                        ): TransferableContent = transferableContent
                    })
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp)
                        .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                            override fun onDragEnter() {
                                calls += "enter-2"
                            }

                            override fun onDragExit() {
                                calls += "exit-2"
                            }

                            override fun onReceive(
                                transferableContent: TransferableContent
                            ): TransferableContent = transferableContent
                        })
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                                override fun onDragEnter() {
                                    calls += "enter-3"
                                }

                                override fun onDragExit() {
                                    calls += "exit-3"
                                }

                                override fun onReceive(
                                    transferableContent: TransferableContent
                                ): TransferableContent = transferableContent
                            })
                    )
                }
            }
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(Offset(225.dp.toPx(), 225.dp.toPx()), draggingUri)
            // enter 1 and 2, skip 3
            drag(Offset(60.dp.toPx(), 60.dp.toPx()), draggingUri)
            // exits 2, stays in 1
            drag(Offset(40.dp.toPx(), 40.dp.toPx()), draggingUri)
            // enters 2 and 3
            drag(Offset(100.dp.toPx(), 100.dp.toPx()), draggingUri)
            // exits all of them at once
            drag(Offset(201.dp.toPx(), 201.dp.toPx()), draggingUri)
        }

        rule.runOnIdle {
            assertThat(calls).isEqualTo(
                listOf(
                    "enter-1",
                    "enter-2",
                    "exit-2",
                    "enter-2",
                    "enter-3",
                    "exit-1",
                    "exit-2",
                    "exit-3"
                )
            )
        }
    }

    @Test
    fun dragAndDrop_startEndCallbacks_singleNode() {
        lateinit var view: View
        val calls = mutableListOf<String>()
        rule.setContent {
            view = LocalView.current
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                        override fun onDragStart() {
                            calls += "start"
                        }

                        override fun onDragEnd() {
                            calls += "end"
                        }

                        override fun onReceive(
                            transferableContent: TransferableContent
                        ): TransferableContent? = null
                    })
            )
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(Offset(125.dp.toPx(), 125.dp.toPx()), draggingUri)
            cancelDrag()
        }

        rule.runOnIdle {
            assertThat(calls).isEqualTo(listOf("start", "end"))
        }

        calls.clear()

        testDragAndDrop(view, rule.density) {
            drag(Offset(50.dp.toPx(), 50.dp.toPx()), draggingUri)
            cancelDrag()
        }

        rule.runOnIdle {
            assertThat(calls).isEqualTo(listOf("start", "end"))
        }
    }

    @Test
    fun dragAndDrop_startEndCallbacks_nestedNodes() {
        lateinit var view: View
        val calls = mutableListOf<String>()
        rule.setContent {
            view = LocalView.current
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                        override fun onDragStart() {
                            calls += "start-1"
                        }

                        override fun onDragEnd() {
                            calls += "end-1"
                        }

                        override fun onReceive(
                            transferableContent: TransferableContent
                        ): TransferableContent = transferableContent
                    })
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp)
                        .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                            override fun onDragStart() {
                                calls += "start-2"
                            }

                            override fun onDragEnd() {
                                calls += "end-2"
                            }

                            override fun onReceive(
                                transferableContent: TransferableContent
                            ): TransferableContent = transferableContent
                        })
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .receiveContent(setOf(MediaType.All), object : ReceiveContentListener {
                                override fun onDragStart() {
                                    calls += "start-3"
                                }

                                override fun onDragEnd() {
                                    calls += "end-3"
                                }

                                override fun onReceive(
                                    transferableContent: TransferableContent
                                ): TransferableContent = transferableContent
                            })
                    )
                }
            }
        }

        val draggingUri = Uri.parse("content://com.example/content.jpg")
        testDragAndDrop(view, rule.density) {
            drag(Offset(225.dp.toPx(), 225.dp.toPx()), draggingUri)
            cancelDrag()
        }

        rule.runOnIdle {
            assertThat(calls.take(3)).containsExactlyElementsIn(
                listOf("start-1", "start-2", "start-3")
            )
            assertThat(calls.drop(3)).containsExactlyElementsIn(
                listOf("end-1", "end-2", "end-3")
            )
        }
    }

    private data class TestElement(
        val onNode: (TestNode) -> Unit
    ) : ModifierNodeElement<TestNode>() {
        override fun create(): TestNode = TestNode(onNode)
        override fun update(node: TestNode) {
            node.onNode = onNode
        }
    }

    private class TestNode(
        var onNode: (TestNode) -> Unit
    ) : Modifier.Node(), ModifierLocalModifierNode {

        override fun onAttach() {
            onNode(this)
        }
    }
}

internal fun createClipData(
    label: String = defaultLabel,
    block: (ClipDataBuilder.() -> Unit)? = null
): ClipData {
    val builder = ClipDataBuilder()
    return if (block != null) {
        builder.block()
        builder.build(label)
    } else {
        builder.apply {
            addText()
            addUri()
            addHtmlText()
            addIntent()
        }.build(label)
    }
}

/**
 * Helper scope to build ClipData objects for tests. This scope also builds a valid ClipDescription
 * object according to supplied mimeTypes.
 */
internal class ClipDataBuilder {
    private val items = mutableListOf<ClipData.Item>()
    private val mimeTypes = mutableSetOf<String>()

    fun addText(
        text: String = "plain text",
        mimeType: String = ClipDescription.MIMETYPE_TEXT_PLAIN
    ) {
        items.add(ClipData.Item(text))
        mimeTypes.add(mimeType)
    }

    fun addHtmlText(
        text: String = "Html Content",
        htmlText: String = "<p>Html Content</p>",
        mimeType: String = ClipDescription.MIMETYPE_TEXT_HTML
    ) {
        items.add(ClipData.Item(text, htmlText))
        mimeTypes.add(mimeType)
    }

    fun addUri(
        uri: Uri = defaultUri,
        mimeType: String = "image/png"
    ) {
        items.add(ClipData.Item(uri))
        mimeTypes.add(mimeType)
    }

    fun addIntent(
        intent: Intent = defaultIntent,
        mimeType: String = ClipDescription.MIMETYPE_TEXT_INTENT
    ) {
        items.add(ClipData.Item(intent))
        mimeTypes.add(mimeType)
    }

    fun build(label: String = "label"): ClipData {
        val clipDescription = ClipDescription(label, mimeTypes.toTypedArray())
        val clipData = ClipData(clipDescription, items.first())
        for (i in 1 until items.size) {
            clipData.addItem(items[i])
        }
        return clipData
    }
}

private val defaultLabel = "label"
private val defaultIntent = Intent(
    Intent.ACTION_VIEW,
    Uri.parse("https://example.com")
)
private val defaultUri = Uri.parse("content://com.example.app/image")

@OptIn(ExperimentalFoundationApi::class)
private val MediaType.Companion.Video: MediaType
    get() = MediaType("video/*")

@OptIn(ExperimentalFoundationApi::class)
private val MediaType.Companion.Audio: MediaType
    get() = MediaType("audio/*")
