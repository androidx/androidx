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

package androidx.compose.remote.core.operations.layout.modifiers

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.LayoutManager
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertIs

class HostNamedActionOperationTest {

    @Test
    fun `clicking Box dispatches null value when valueId is -1`() {
        // Arrange.
        val context = TestRemoteContext()
        val document =
            createDocument(context) {
                root {
                    box(
                        RecordingModifier().size(100, 100).onClick(HostAction("submit")),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }

        // Act.
        document.onClick(context, 50f, 50f)

        // Assert.
        assertThat(context.namedActions).containsExactly("submit" to null)
    }

    @Test
    fun `clicking Box dispatches integer value`() {
        // Arrange.
        val context = TestRemoteContext()
        val document =
            createDocument(context) {
                val intId = addInteger(123).toInt()
                root {
                    box(
                        RecordingModifier()
                            .size(100, 100)
                            .onClick(
                                HostAction("submit", HostNamedActionOperation.INT_TYPE, intId)
                            ),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }

        // Act.
        document.onClick(context, 50f, 50f)

        // Assert.
        assertThat(context.namedActions).containsExactly("submit" to 123)
    }

    @Test
    fun `clicking Box dispatches float value`() {
        // Arrange.
        val context = TestRemoteContext()
        val document =
            createDocument(context) {
                val floatId = Utils.idFromNan(addFloatConstant(3.5f))
                root {
                    box(
                        RecordingModifier()
                            .size(100, 100)
                            .onClick(
                                HostAction("submit", HostNamedActionOperation.FLOAT_TYPE, floatId)
                            ),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }

        // Act.
        document.onClick(context, 50f, 50f)

        // Assert.
        assertThat(context.namedActions).containsExactly("submit" to 3.5f)
    }

    @Test
    fun `clicking Box dispatches string value`() {
        // Arrange.
        val context = TestRemoteContext()
        val document =
            createDocument(context) {
                val textId = addText("payload")
                root {
                    box(
                        RecordingModifier()
                            .size(100, 100)
                            .onClick(
                                HostAction("submit", HostNamedActionOperation.STRING_TYPE, textId)
                            ),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }

        // Act.
        document.onClick(context, 50f, 50f)

        // Assert.
        assertThat(context.namedActions).containsExactly("submit" to "payload")
    }

    @Test
    fun `clicking Box dispatches float array value`() {
        // Arrange.
        val context = TestRemoteContext()
        val document =
            createDocument(context) {
                val arrayId = Utils.idFromNan(addFloatArray(floatArrayOf(1f, 2f, 3f)))
                root {
                    box(
                        RecordingModifier()
                            .size(100, 100)
                            .onClick(
                                HostAction(
                                    "submit",
                                    HostNamedActionOperation.FLOAT_ARRAY_TYPE,
                                    arrayId,
                                )
                            ),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }

        // Act.
        document.onClick(context, 50f, 50f)

        // Assert.
        assertThat(context.namedActions).hasSize(1)
        assertThat(context.namedActions[0].first).isEqualTo("submit")
        val array = assertIs<FloatArray>(context.namedActions[0].second)
        assertThat(array).usingExactEquality().containsExactly(1f, 2f, 3f).inOrder()
    }

    private fun createDocument(
        context: RemoteContext,
        buildDocument: RemoteComposeWriter.() -> Unit,
    ): CoreDocument {
        val writer =
            RemoteComposeWriter(
                RcPlatformServices.None,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 200),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        writer.buildDocument()
        val document =
            CoreDocument().apply {
                initFromBuffer(
                    RemoteComposeBuffer.fromInputStream(
                        ByteArrayInputStream(writer.encodeToByteArray())
                    )
                )
            }
        context.setDocument(document)
        context.touchVersion = LayoutManager.FIX_TOUCH_EVENT
        context.paintContext = SoftwareRasterPaintContext(context, 200, 200)
        context.mWidth = 200f
        context.mHeight = 200f
        document.initializeContext(context)
        document.paint(context, Theme.UNSPECIFIED)
        return document
    }

    private class TestRemoteContext : HeadlessRemoteContext() {
        val namedActions = mutableListOf<Pair<String?, Any?>>()

        override fun runNamedAction(textId: Int, value: Any?) {
            namedActions.add(getText(textId) to value)
        }
    }
}
