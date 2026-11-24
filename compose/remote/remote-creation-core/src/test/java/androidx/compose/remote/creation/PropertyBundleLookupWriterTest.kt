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

package androidx.compose.remote.creation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.PropertyBundleLookupOperation
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PropertyBundleLookupWriterTest {

    private val displayInfo = CreationDisplayInfo(450, 450, 320)
    private val experimentalMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL

    @Test
    fun addPropertyBundleLookup_currentComponent_resolvesPropertyAndFeedsIntegerExpression() {
        val writer = createExperimentalWriter()
        var presentId = 0L
        var missingId = 0L
        var sumExprId = 0L

        writer.root {
            writer.startBox(
                RecordingModifier()
                    .propertyBundle(
                        /* bundleId = */ 5,
                        /* parentBundleId = */ 0,
                        /* propertyIds = */ intArrayOf(15, 20),
                        /* values = */ intArrayOf(30, 12),
                    )
            )
            presentId = writer.addPropertyBundleLookup(/* propertyId= */ 15, /* defaultValue= */ 0)
            missingId = writer.addPropertyBundleLookup(/* propertyId= */ 99, /* defaultValue= */ -5)
            sumExprId = writer.integerExpression(presentId, 12, Rc.IntegerExpression.L_ADD)
            writer.endBox()
        }

        val doc = parseDocument(writer)
        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val box = root.list.filterIsInstance<BoxLayout>().single()
        val lookups = box.list.filterIsInstance<PropertyBundleLookupOperation>()
        assertThat(lookups).hasSize(2)
        assertThat(lookups[0].componentId).isEqualTo(box.componentId)
        assertThat(lookups[1].componentId).isEqualTo(box.componentId)

        val context = MinimalTestRemoteContext()
        doc.initializeContext(context)

        assertThat(context.getInteger((presentId - 0x100000000L).toInt())).isEqualTo(30)
        assertThat(context.getInteger((missingId - 0x100000000L).toInt())).isEqualTo(-5)
        assertThat(context.getInteger((sumExprId - 0x100000000L).toInt())).isEqualTo(42)
    }

    @Test
    fun addPropertyBundleLookup_explicitComponentId_resolvesTargetComponentProperty() {
        val writer = createExperimentalWriter()
        val parentComponentId = 101
        var parentLookupId = 0L
        val explicitValueId = 200

        writer.root {
            writer.startColumn(
                RecordingModifier()
                    .componentId(parentComponentId)
                    .propertyBundle(
                        /* bundleId = */ 1,
                        /* parentBundleId = */ 0,
                        /* propertyIds = */ intArrayOf(77),
                        /* values = */ intArrayOf(555),
                    ),
                ColumnLayout.START,
                ColumnLayout.TOP,
            )
            writer.startBox(
                RecordingModifier()
                    .propertyBundle(
                        /* bundleId = */ 2,
                        /* parentBundleId = */ 1,
                        /* propertyIds = */ intArrayOf(77),
                        /* values = */ intArrayOf(111),
                    )
            )
            parentLookupId =
                writer.addPropertyBundleLookup(
                    /* componentId = */ parentComponentId,
                    /* propertyId = */ 77,
                    /* defaultValue = */ -1,
                )
            writer.addPropertyBundleLookupOperation(
                /* valueId = */ explicitValueId,
                /* componentId = */ parentComponentId,
                /* propertyId = */ 888,
                /* defaultValue = */ -42,
            )
            writer.endBox()
            writer.endColumn()
        }

        val doc = parseDocument(writer)
        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val column = root.list.filterIsInstance<ColumnLayout>().single()
        val childBox = column.list.filterIsInstance<BoxLayout>().single()
        val lookups = childBox.list.filterIsInstance<PropertyBundleLookupOperation>()
        assertThat(lookups).hasSize(2)
        assertThat(lookups[0].componentId).isEqualTo(parentComponentId)
        assertThat(lookups[1].componentId).isEqualTo(parentComponentId)

        val context = MinimalTestRemoteContext()
        doc.initializeContext(context)

        assertThat(context.getInteger((parentLookupId - 0x100000000L).toInt())).isEqualTo(555)
        assertThat(context.getInteger(explicitValueId)).isEqualTo(-42)
    }

    @Test
    fun addPropertyBundleLookupOperation_preAllocatedId_drivesEnclosingModifiersAndPostChildLookup() {
        val writer = createExperimentalWriter()
        val selfVar = writer.nextId()
        val visibilityVar = writer.nextId()
        var postChildLookupId = 0L

        writer.root {
            writer.startColumn(
                RecordingModifier()
                    .propertyBundle(
                        /* bundleId = */ 10,
                        /* parentBundleId = */ 0,
                        /* propertyIds = */ intArrayOf(1),
                        /* values = */ intArrayOf(320),
                    ),
                ColumnLayout.START,
                ColumnLayout.TOP,
            )
            writer.startBox(
                RecordingModifier()
                    .propertyBundle(
                        /* bundleId = */ 11,
                        /* parentBundleId = */ 10,
                        /* propertyIds = */ intArrayOf(2, 3),
                        /* values = */ intArrayOf(180, Component.Visibility.GONE),
                    )
                    .width(writer.asFloatId(selfVar.toLong()))
                    .visibility(visibilityVar)
            )
            writer.addPropertyBundleLookupOperation(
                /* valueId = */ selfVar,
                /* propertyId = */ 2,
                /* defaultValue = */ 50,
            )
            writer.addPropertyBundleLookupOperation(
                /* valueId = */ visibilityVar,
                /* propertyId = */ 3,
                /* defaultValue = */ Component.Visibility.VISIBLE,
            )
            writer.endBox()

            // Emitted after childBox has modified mBuffer.lastComponentId; should still bind to
            // the enclosing column.
            postChildLookupId =
                writer.addPropertyBundleLookup(/* propertyId= */ 1, /* defaultValue= */ -1)
            writer.endColumn()
        }

        val doc = parseDocument(writer)
        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val column = root.list.filterIsInstance<ColumnLayout>().single()
        val childBox = column.list.filterIsInstance<BoxLayout>().single()

        val context = MinimalTestRemoteContext()
        doc.initializeContext(context)
        context.updateOps()

        assertThat(context.getInteger(selfVar)).isEqualTo(180)
        assertThat(context.getFloat(selfVar)).isEqualTo(180f)
        assertThat(context.getInteger(visibilityVar)).isEqualTo(Component.Visibility.GONE)
        assertThat(childBox.getMeasureVisibility(context)).isEqualTo(Component.Visibility.GONE)
        assertThat(context.getInteger((postChildLookupId - 0x100000000L).toInt())).isEqualTo(320)
    }

    private fun createExperimentalWriter(): RemoteComposeWriter {
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, experimentalMask, RcPlatformServices.None) {
                info,
                prof,
                _ ->
                RemoteComposeWriter(
                    prof,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, info.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, info.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, experimentalMask),
                )
            }
        return profile.create(displayInfo, "test")
    }

    private fun parseDocument(writer: RemoteComposeWriter): CoreDocument {
        val bytes = writer.encodeToByteArray()
        val readBuffer = RemoteComposeBuffer()
        readBuffer.buffer.setSystemInfo(SystemInfo(writer.apiLevel, experimentalMask))
        readBuffer.buffer.reset(bytes.size)
        for (b in bytes) {
            readBuffer.buffer.writeByte(b.toInt())
        }
        readBuffer.buffer.setIndex(0)
        return CoreDocument().apply { initFromBuffer(readBuffer) }
    }

    private class MinimalTestRemoteContext : RemoteContext() {
        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {}

        override fun getPathData(instanceId: Int): FloatArray? = null

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {}

        override fun loadColor(id: Int, color: Int) {
            mRemoteComposeState.updateColor(id, color)
        }

        override fun setNamedColorOverride(colorName: String, color: Int) {}

        override fun setNamedStringOverride(stringName: String, value: String) {}

        override fun clearNamedStringOverride(stringName: String) {}

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {}

        override fun clearNamedBooleanOverride(booleanName: String) {}

        override fun setNamedIntegerOverride(integerName: String, value: Int) {}

        override fun clearNamedIntegerOverride(integerName: String) {}

        override fun setNamedFloatOverride(floatName: String, value: Float) {}

        override fun clearNamedFloatOverride(floatName: String) {}

        override fun setNamedLong(name: String, value: Long) {}

        override fun setNamedDataOverride(dataName: String, value: Any) {}

        override fun clearNamedDataOverride(dataName: String) {}

        override fun addCollection(id: Int, collection: ArrayAccess) {
            mRemoteComposeState.addCollection(id, collection)
        }

        override fun putDataMap(id: Int, map: DataMap) {
            mRemoteComposeState.putDataMap(id, map)
        }

        override fun getDataMap(id: Int): DataMap? = mRemoteComposeState.getDataMap(id)

        override fun runAction(id: Int, metadata: String) {}

        override fun runNamedAction(id: Int, value: Any?) {}

        override fun putObject(id: Int, value: Any) {
            mRemoteComposeState.updateObject(id, value)
        }

        override fun getObject(id: Int): Any? = mRemoteComposeState.getObject(id)

        override fun hapticEffect(type: Int) {}

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {}

        override fun loadText(id: Int, text: String) {
            mRemoteComposeState.cacheData(id, text)
        }

        override fun getText(id: Int): String? = mRemoteComposeState.getFromId(id) as? String

        override fun loadFloat(id: Int, value: Float) {
            mRemoteComposeState.updateFloat(id, value)
        }

        override fun overrideFloat(id: Int, value: Float) {
            mRemoteComposeState.overrideFloat(id, value)
        }

        override fun loadInteger(id: Int, value: Int) {
            mRemoteComposeState.updateInteger(id, value)
        }

        override fun overrideInteger(id: Int, value: Int) {
            mRemoteComposeState.overrideInteger(id, value)
        }

        override fun overrideText(id: Int, valueId: Int) {}

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
            mRemoteComposeState.cacheData(id, animatedFloat)
        }

        override fun loadShader(id: Int, value: ShaderData) {
            mRemoteComposeState.cacheData(id, value)
        }

        override fun getFloat(id: Int): Float = mRemoteComposeState.getFloat(id)

        override fun getInteger(id: Int): Int = mRemoteComposeState.getInteger(id)

        override fun getLong(id: Int): Long = 0L

        override fun getColor(id: Int): Int = mRemoteComposeState.getColor(id)

        override fun listensTo(id: Int, variableSupport: VariableSupport) {
            mRemoteComposeState.listenToVar(id, variableSupport)
        }

        override fun updateOps(): Int = mRemoteComposeState.getOpsToUpdate(this, currentTime)

        override fun getShader(id: Int): ShaderData? =
            mRemoteComposeState.getFromId(id) as? ShaderData

        override fun addClickArea(
            id: Int,
            contentDescriptionId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {}
    }
}
