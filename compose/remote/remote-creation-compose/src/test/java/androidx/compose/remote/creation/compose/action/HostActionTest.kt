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

package androidx.compose.remote.creation.compose.action

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createProfile
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.serialization.yaml.YAMLSerializer
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.remember
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class HostActionTest {

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun hostActionWithNoValue_actionTypeIsNone() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.clickable(hostAction("foobar".rs)))
            }
        val ops = doc.findOperations<HostNamedActionOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().getActionType()).isEqualTo("NONE_TYPE")
        assertThat(doc.displayHierarchy()).contains("HOST_NAMED_ACTION")
    }

    @Test
    fun hostActionWithFloatValue_actionTypeIsFloat() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.clickable(hostAction("foobar".rs, 1.5f.rf)))
            }
        val ops = doc.findOperations<HostNamedActionOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().getActionType()).isEqualTo("FLOAT_TYPE")
    }

    @Test
    fun hostActionWithIntValue_actionTypeIsInt() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.clickable(hostAction("foobar".rs, 42.ri)))
            }
        val ops = doc.findOperations<HostNamedActionOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().getActionType()).isEqualTo("INT_TYPE")
    }

    @Test
    fun hostActionWithStringValue_actionTypeIsString() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.clickable(hostAction("foobar".rs, "test".rs)))
            }
        val ops = doc.findOperations<HostNamedActionOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().getActionType()).isEqualTo("STRING_TYPE")
    }

    @Test
    fun hostActionWithVariableRemoteString_createsHostNamedActionOperation() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                val variableName = remember { createNamedRemoteString("action_name", "my_action") }
                RemoteBox(modifier = RemoteModifier.clickable(hostAction(variableName)))
            }
        assertThat(doc.findOperations<HostActionOperation>()).isEmpty()
        val namedOps = doc.findOperations<HostNamedActionOperation>()
        assertThat(namedOps).hasSize(1)
        assertThat(namedOps.first().getActionType()).isEqualTo("NONE_TYPE")
    }

    @Test
    fun hostActionWithVariableRemoteStringAndValue_keepsTypedValue() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                val variableName = remember { createNamedRemoteString("action_name", "my_action") }
                RemoteBox(modifier = RemoteModifier.clickable(hostAction(variableName, 1.5f.rf)))
            }
        assertThat(doc.findOperations<HostActionMetadataOperation>()).isEmpty()
        val namedOps = doc.findOperations<HostNamedActionOperation>()
        assertThat(namedOps).hasSize(1)
        assertThat(namedOps.first().getActionType()).isEqualTo("FLOAT_TYPE")
    }

    @Test
    fun pendingIntentAction_defaultProfile_writesNamedAction() = runTest {
        assertWritesNamedAction(RcPlatformProfiles.ANDROIDX)
    }

    @Test
    fun pendingIntentAction_wearWidgetsProfile_writesNamedAction() = runTest {
        // Wear hosts dispatch this named action, so its encoding must not change.
        assertWritesNamedAction(createProfile(profileFlags = RcProfiles.PROFILE_WEAR_WIDGETS))
    }

    @Test
    fun pendingIntentAction_launcherWidgetsV6_writesIdAction() = runTest {
        assertWritesIdAction(LauncherWidgetTestProfiles.WIDGETS_V6)
    }

    @Test
    fun pendingIntentAction_launcherWidgetsV7_writesIdAction() = runTest {
        assertWritesIdAction(LauncherWidgetTestProfiles.WIDGETS_V7)
    }

    private suspend fun assertWritesNamedAction(profile: Profile) {
        val pendingIntent = testPendingIntent()
        val captured =
            captureSingleRemoteDocument(context = context, profile = profile) {
                RemoteBox(
                    modifier = RemoteModifier.clickable(pendingIntentAction { pendingIntent })
                )
            }
        val doc = captured.toCoreDocument()
        assertThat(doc.findOperations<HostActionOperation>()).isEmpty()
        val namedOps = doc.findOperations<HostNamedActionOperation>()
        assertThat(namedOps).hasSize(1)
        assertThat(namedOps.first().getActionType()).isEqualTo("INT_TYPE")
        assertThat(captured.pendingIntents[0]).isSameInstanceAs(pendingIntent)
    }

    private suspend fun assertWritesIdAction(profile: Profile) {
        val pendingIntent = testPendingIntent()
        val captured =
            captureSingleRemoteDocument(context = context, profile = profile) {
                RemoteBox(
                    modifier = RemoteModifier.clickable(pendingIntentAction { pendingIntent })
                )
            }
        val doc = captured.toCoreDocument()
        assertThat(doc.findOperations<HostNamedActionOperation>()).isEmpty()
        val ops = doc.findOperations<HostActionOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().actionId).isEqualTo(PendingIntentAction.FIRST_ACTION_ID)
        assertThat(captured.pendingIntents[0]).isSameInstanceAs(pendingIntent)
    }

    @Test
    fun hostActionWithBooleanValue_actionTypeIsInt() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.clickable(hostAction("foobar".rs, true.rb)))
            }
        val ops = doc.findOperations<HostNamedActionOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().getActionType()).isEqualTo("INT_TYPE")
    }

    @Test
    fun hostActionWithId_createsHostActionOperationWithStableId() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.clickable(hostAction(id = 42)))
            }
        val ops = doc.findOperations<HostActionOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().actionId).isEqualTo(42)
    }

    @Test
    fun hostActionWithIdAndMetadata_createsHostActionMetadataOperation() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(
                    modifier =
                        RemoteModifier.clickable(
                            hostAction(id = 42, metadata = "a=1&b=".rs + 2.ri.toRemoteString())
                        )
                )
            }
        val ops = doc.findOperations<HostActionMetadataOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().actionId).isEqualTo(42)
    }

    @Test
    fun hostActionWithDefaultComponentId_throws() {
        assertThrows(IllegalArgumentException::class.java) { hostAction(id = -1) }
    }

    @Test
    fun pendingIntentAction_withDynamicExtras_writesMetadata() = runTest {
        val pendingIntent = testPendingIntent()
        val captured =
            captureSingleRemoteDocument(context = context) {
                val taps = rememberMutableRemoteInt(3)
                RemoteBox(
                    modifier =
                        RemoteModifier.clickable(
                            pendingIntentAction(
                                "taps" to taps.toRemoteString(),
                                "screen" to "home".rs,
                            ) {
                                pendingIntent
                            }
                        )
                )
            }
        val ops = captured.toCoreDocument().findOperations<HostActionMetadataOperation>()
        assertThat(ops).hasSize(1)
        assertThat(ops.first().actionId).isEqualTo(PendingIntentAction.FIRST_ACTION_ID)
    }

    @Test
    fun pendingIntentAction_withOnlyConstantExtras_writesNoMetadata() = runTest {
        val pendingIntent = testPendingIntent()
        val captured =
            captureSingleRemoteDocument(context = context) {
                RemoteBox(
                    modifier =
                        RemoteModifier.clickable(
                            pendingIntentAction("screen" to "home".rs) { pendingIntent }
                        )
                )
            }
        val doc = captured.toCoreDocument()
        // Without dynamic extras, the default named action is written.
        assertThat(doc.findOperations<HostActionMetadataOperation>()).isEmpty()
        assertThat(doc.findOperations<HostNamedActionOperation>()).hasSize(1)
    }

    @Test
    fun pendingIntentAction_wearWidgetsProfile_ignoresExtras() = runTest {
        val pendingIntent = testPendingIntent()
        val captured =
            captureSingleRemoteDocument(
                context = context,
                profile = createProfile(profileFlags = RcProfiles.PROFILE_WEAR_WIDGETS),
            ) {
                val taps = rememberMutableRemoteInt(3)
                RemoteBox(
                    modifier =
                        RemoteModifier.clickable(
                            pendingIntentAction("taps" to taps.toRemoteString()) { pendingIntent }
                        )
                )
            }
        val doc = captured.toCoreDocument()
        assertThat(doc.findOperations<HostActionMetadataOperation>()).isEmpty()
        assertThat(doc.findOperations<HostNamedActionOperation>()).hasSize(1)
    }

    @Test
    fun decodeExtras_keysOnlyComeFromTheHeader() {
        val us = '\u001F'
        val gs = '\u001D'
        val rs = '\u001E'
        assertThat(PendingIntentAction.decodeExtras("a${us}b${gs}1${rs}2"))
            .containsExactly("a", "1", "b", "2")
            .inOrder()
        // A value containing separators cannot add a key; the last key takes the remainder.
        assertThat(PendingIntentAction.decodeExtras("a${us}b${gs}1${rs}x${us}evil${rs}2"))
            .containsExactly("a", "1", "b", "x${us}evil${rs}2")
            .inOrder()
        assertThat(PendingIntentAction.decodeExtras("a${gs}x${gs}evil"))
            .containsExactly("a", "x${gs}evil")
        // Malformed metadata decodes to fewer entries without throwing.
        assertThat(PendingIntentAction.decodeExtras("a${us}b${gs}1")).containsExactly("a", "1")
        assertThat(PendingIntentAction.decodeExtras("no header")).isEmpty()
        assertThat(PendingIntentAction.decodeExtras(null)).isEmpty()
    }

    private fun testPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent("test").setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private fun CapturedDocument.toCoreDocument(): CoreDocument =
        CoreDocument().apply {
            initFromBuffer(RemoteComposeBuffer.fromInputStream(bytes.inputStream()))
        }

    private fun <T : Operation> CoreDocument.findOperations(clazz: Class<T>): List<T> {
        val result = mutableListOf<T>()
        fun collect(ops: List<Operation>) {
            for (op in ops) {
                if (clazz.isInstance(op)) {
                    result.add(clazz.cast(op)!!)
                }
                if (op is Container) {
                    collect(op.list)
                }
                if (op is LayoutComponent) {
                    collect(op.componentModifiers.list)
                }
            }
        }
        collect(operations)
        return result
    }

    private inline fun <reified T : Operation> CoreDocument.findOperations(): List<T> =
        findOperations(T::class.java)

    private fun HostNamedActionOperation.getActionType(): String? {
        val serializer = YAMLSerializer()
        serialize(serializer.serializeMap())
        val map = serializer.toObject() as? Map<*, *>
        return map?.get("actionType") as? String
    }
}
