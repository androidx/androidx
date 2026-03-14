/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.v2.captureSingleRemoteDocumentV2
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.semantics.Role
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SemanticsModifierTest {
    private val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun semanticsModifier_recordsCorrectProperties() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val captured =
            captureSingleRemoteDocumentV2(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics {
                            contentDescription = "test description".rs
                            text = "test text".rs
                            stateDescription = "test state".rs
                            role = Role.Button
                            enabled = false
                        }
                ) {}
            }

        val coreDoc = makeAndUpdateCoreDocument(captured)

        val boxLayout = coreDoc.getComponent(-3) as BoxLayout
        val semantics = boxLayout.componentModifiers.list.filterIsInstance<CoreSemantics>().first()

        assertThat(semantics).isNotNull()
        assertThat(context.getText(semantics.contentDescriptionId)).isEqualTo("test description")
        assertThat(context.getText(semantics.textId!!)).isEqualTo("test text")
        assertThat(context.getText(semantics.stateDescriptionId!!)).isEqualTo("test state")
        assertThat(semantics.mEnabled).isEqualTo(false)
        assertThat(semantics.role).isEqualTo(AccessibleComponent.Role.BUTTON)
        assertThat(semantics.mode).isEqualTo(AccessibleComponent.Mode.SET)
    }

    @Test
    fun clearAndSetSemantics_recordsCorrectMode() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val captured =
            captureSingleRemoteDocumentV2(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                RemoteBox(modifier = RemoteModifier.clearAndSetSemantics { text = "item".rs }) {}
            }

        val coreDoc = makeAndUpdateCoreDocument(captured)

        val boxLayout = coreDoc.getComponent(-3) as BoxLayout
        val semantics = boxLayout.componentModifiers.list.filterIsInstance<CoreSemantics>().first()

        assertThat(semantics).isNotNull()
        assertThat(semantics.mode).isEqualTo(AccessibleComponent.Mode.CLEAR_AND_SET)
        assertThat(context.getText(semantics.textId!!)).isEqualTo("item")
    }

    private fun makeAndUpdateCoreDocument(captured: CapturedDocument) =
        CoreDocument().apply {
            val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(captured.bytes))
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)

            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }
}
