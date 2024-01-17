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

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.content.internal.mergeReceiveContentConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
class ReceiveContentConfigurationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun receiveContentConfiguration_isMergedBottomToTop() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = null
        val listenerCalls = mutableListOf<Int>()
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(MediaType.Video) { listenerCalls += 3; it }
                .receiveContent(MediaType.Audio) { listenerCalls += 2; it }
                .receiveContent(MediaType.Text) { listenerCalls += 1; it }
                .then(TestElement {
                    calculatedReceiveContent = it.mergeReceiveContentConfiguration()
                    calculatedReceiveContent
                        ?.onReceive
                        ?.invoke(TransferableContent(createClipData()))
                })
            )
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent?.acceptedMimeTypes).isNotNull()
            assertThat(calculatedReceiveContent?.acceptedMimeTypes).containsExactlyElementsIn(
                listOf(
                    MediaType.Video.representation,
                    MediaType.Audio.representation,
                    MediaType.Text.representation
                )
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
                .receiveContent(MediaType.Video) {
                    videoReceived = it
                    val t = it.consumeEach { it.uri?.toString()?.contains("video") ?: false }
                    t
                }
                .receiveContent(MediaType.Audio) {
                    audioReceived = it
                    val t = it.consumeEach { it.uri?.toString()?.contains("audio") ?: false }
                    t
                }
                .receiveContent(MediaType.Text) {
                    textReceived = it
                    val t = it.consumeEach { it.text != null }
                    t
                }
                .then(TestElement {
                    it.mergeReceiveContentConfiguration()
                        ?.onReceive
                        ?.invoke(TransferableContent(createClipData {
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
            emptySet()
        ) { null }
        rule.setContent {
            Box(
                modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.mergeReceiveContentConfiguration()
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
            emptySet()
        ) { null }
        rule.setContent {
            Box {
                Box(modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.mergeReceiveContentConfiguration()
                }))
                Box(modifier = Modifier.receiveContent(MediaType.Text) { it })
            }
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent).isNull()
        }
    }

    @Test
    fun receiveContentConfiguration_returnsNullIfDefined_atChildNode() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = ReceiveContentConfiguration(
            emptySet()
        ) { null }
        rule.setContent {
            Box(
                modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.mergeReceiveContentConfiguration()
                })
            ) {
                Box(modifier = Modifier.receiveContent(MediaType.Text) { it })
            }
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent).isNull()
        }
    }

    @Test
    fun receiveContentConfiguration_noMimeType_returnsNull() {
        var calculatedReceiveContent: ReceiveContentConfiguration? = ReceiveContentConfiguration(
            emptySet()
        ) { null }
        rule.setContent {
            Box(modifier = Modifier.receiveContent { it }) {
                Box(modifier = Modifier.then(TestElement {
                    calculatedReceiveContent = it.mergeReceiveContentConfiguration()
                }))
            }
        }

        rule.runOnIdle {
            assertThat(calculatedReceiveContent).isNull()
        }
    }

    @Test
    fun detachedReceiveContent_disappearsFromMergedConfiguration() {
        var getReceiveContentConfiguration: (() -> ReceiveContentConfiguration?)? = null
        var attached by mutableStateOf(true)
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(MediaType.Video) { it }
                .then(if (attached) Modifier.receiveContent(MediaType.Audio) { it } else Modifier)
                .receiveContent(MediaType.Text) { it }
                .then(TestElement {
                    getReceiveContentConfiguration = {
                        it.mergeReceiveContentConfiguration()
                    }
                })
            )
        }

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.acceptedMimeTypes).containsExactlyElementsIn(
                listOf(
                    MediaType.Video.representation,
                    MediaType.Audio.representation,
                    MediaType.Text.representation
                )
            )
        }

        attached = false

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.acceptedMimeTypes).containsExactlyElementsIn(
                listOf(
                    MediaType.Video.representation,
                    MediaType.Text.representation
                )
            )
        }
    }

    @Test
    fun laterAttachedReceiveContent_appearsInMergedConfiguration() {
        var getReceiveContentConfiguration: (() -> ReceiveContentConfiguration?)? = null
        var attached by mutableStateOf(false)
        rule.setContent {
            Box(modifier = Modifier
                .receiveContent(MediaType.Video) { it }
                .then(if (attached) Modifier.receiveContent(MediaType.Audio) { it } else Modifier)
                .receiveContent(MediaType.Text) { it }
                .then(TestElement {
                    getReceiveContentConfiguration = {
                        it.mergeReceiveContentConfiguration()
                    }
                })
            )
        }

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.acceptedMimeTypes).containsExactlyElementsIn(
                listOf(
                    MediaType.Video.representation,
                    MediaType.Text.representation
                )
            )
        }

        attached = true

        rule.runOnIdle {
            val receiveContentConfiguration = getReceiveContentConfiguration?.invoke()
            assertThat(receiveContentConfiguration).isNotNull()
            assertThat(receiveContentConfiguration?.acceptedMimeTypes).containsExactlyElementsIn(
                listOf(
                    MediaType.Video.representation,
                    MediaType.Audio.representation,
                    MediaType.Text.representation
                )
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
    ) : Modifier.Node(), PlatformTextInputModifierNode {

        override fun onAttach() {
            onNode(this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val MediaType.Companion.Video: MediaType
    get() = MediaType("video/*")

@OptIn(ExperimentalFoundationApi::class)
private val MediaType.Companion.Audio: MediaType
    get() = MediaType("audio/*")
