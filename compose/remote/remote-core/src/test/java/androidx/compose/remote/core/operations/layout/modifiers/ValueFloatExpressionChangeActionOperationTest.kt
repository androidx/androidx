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
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.Test

class ValueFloatExpressionChangeActionOperationTest {

    @Test
    fun `clicking Box evaluates expression and updates target`() {
        // Arrange.
        var targetId = 0
        val context = HeadlessRemoteContext()
        val document =
            createDocument(context) {
                val targetNan = addFloatConstant(4.0f)
                targetId = Utils.idFromNan(targetNan)
                val exprId =
                    Utils.idFromNan(floatExpression(targetNan, 2.5f, AnimatedFloatExpression.ADD))
                root {
                    box(
                        RecordingModifier()
                            .size(100, 100)
                            .onClick(ValueFloatExpressionChange(targetId, exprId)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }

        // Act.
        document.onClick(context, 50f, 50f)
        document.onClick(context, 50f, 50f)

        // Assert.
        assertThat(context.getFloat(targetId)).isWithin(1e-5f).of(9.0f)
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
}
