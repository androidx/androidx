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
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.Test

class RunActionOperationTest {

    @Test
    fun `painting Box with RunAction runs child ActionOperations`() {
        // Arrange.
        var intId = 0
        var floatId = 0
        val context = TestRemoteContext()
        val document =
            createDocument(context) {
                intId = addInteger(0).toInt()
                floatId = Utils.idFromNan(addFloatConstant(0f))
                root {
                    box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {
                        buffer.addRunActionsStart()
                        ValueIntegerChange(intId, 99).write(this)
                        ValueFloatChange(floatId, 4.25f).write(this)
                        addContainerEnd()
                    }
                }
            }

        // Act.
        document.paint(context, Theme.UNSPECIFIED)

        // Assert.
        assertThat(context.getInteger(intId)).isEqualTo(99)
        assertThat(context.getFloat(floatId)).isWithin(1e-5f).of(4.25f)
    }

    @Test
    fun `painting Box with RunAction ignores non ActionOperation children`() {
        // Arrange.
        var intId = 0
        val context = TestRemoteContext()
        val document =
            createDocument(context) {
                intId = addInteger(0).toInt()
                root {
                    box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {
                        buffer.addRunActionsStart()
                        addInteger(123)
                        ValueIntegerChange(intId, 42).write(this)
                        addContainerEnd()
                    }
                }
            }

        // Act.
        document.paint(context, Theme.UNSPECIFIED)

        // Assert.
        assertThat(context.getInteger(intId)).isEqualTo(42)
    }

    @Test
    fun `paint is a no-op when document is null`() {
        // Arrange.
        val context = TestRemoteContext().apply { returnNullDocument = true }
        context.mLastComponent = Component(1, 0f, 0f, 100f, 100f, null)
        context.loadInteger(10, 1)
        val paintContext = SoftwareRasterPaintContext(context, 64, 64)
        val runAction =
            RunActionOperation().apply { list.add(ValueIntegerChangeActionOperation(10, 99)) }

        // Act.
        runAction.paint(paintContext)

        // Assert.
        assertThat(context.getInteger(10)).isEqualTo(1)
    }

    @Test
    fun `painting Box with RunAction skips host actions when ENABLE_RUN_ACTION_HOST_ACTIONS is false`() {
        val originalFlag = Limits.ENABLE_RUN_ACTION_HOST_ACTIONS
        try {
            // Arrange.
            var intId = 0
            val context = TestRemoteContext()
            val document =
                createDocument(context) {
                    val metadataId = addText("meta")
                    intId = addInteger(0).toInt()
                    root {
                        box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {
                            buffer.addRunActionsStart()
                            HostAction(1).write(this)
                            HostAction(2, metadataId).write(this)
                            HostAction("named").write(this)
                            ValueIntegerChange(intId, 55).write(this)
                            addContainerEnd()
                        }
                    }
                }
            Limits.ENABLE_RUN_ACTION_HOST_ACTIONS = false

            // Act.
            document.paint(context, Theme.UNSPECIFIED)

            // Assert.
            assertThat(context.actions).isEmpty()
            assertThat(context.namedActions).isEmpty()
            assertThat(context.getInteger(intId)).isEqualTo(55)
        } finally {
            Limits.ENABLE_RUN_ACTION_HOST_ACTIONS = originalFlag
        }
    }

    @Test
    fun `painting Box with RunAction runs host actions when ENABLE_RUN_ACTION_HOST_ACTIONS is true`() {
        val originalFlag = Limits.ENABLE_RUN_ACTION_HOST_ACTIONS
        try {
            // Arrange.
            val context = TestRemoteContext()
            val document =
                createDocument(context) {
                    val metadataId = addText("meta")
                    root {
                        box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {
                            buffer.addRunActionsStart()
                            HostAction(1).write(this)
                            HostAction(2, metadataId).write(this)
                            HostAction("named").write(this)
                            addContainerEnd()
                        }
                    }
                }
            Limits.ENABLE_RUN_ACTION_HOST_ACTIONS = true

            // Act.
            document.paint(context, Theme.UNSPECIFIED)

            // Assert.
            assertThat(context.actions).containsExactly(1 to "", 2 to "meta").inOrder()
            assertThat(context.namedActions).containsExactly("named" to null)
        } finally {
            Limits.ENABLE_RUN_ACTION_HOST_ACTIONS = originalFlag
        }
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
        context.paintContext = SoftwareRasterPaintContext(context, 200, 200)
        context.mWidth = 200f
        context.mHeight = 200f
        document.initializeContext(context)
        return document
    }

    private class TestRemoteContext : HeadlessRemoteContext() {
        val actions = mutableListOf<Pair<Int, String>>()
        val namedActions = mutableListOf<Pair<String?, Any?>>()
        var returnNullDocument = false

        override fun getDocument(): CoreDocument? =
            if (returnNullDocument) null else super.getDocument()

        override fun runAction(id: Int, metadata: String) {
            actions.add(id to metadata)
        }

        override fun runNamedAction(textId: Int, value: Any?) {
            namedActions.add(getText(textId) to value)
        }
    }
}
