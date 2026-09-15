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

package androidx.compose.remote.creation.compose.layout

import android.content.Context
import android.view.View
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.Custom.CustomProperty
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.util.TestProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.platform.AndroidComponentSupport
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteBaseContentTestRule
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RemoteCustomComponentTest {
    @get:Rule val remoteContentTestRule = RemoteContentTestRule()

    private val customSupport = AndroidCustomContextImpl()

    private val remoteContext: AndroidRemoteContext
        get() = checkNotNull(customSupport.getRemoteContext() as? AndroidRemoteContext)

    @Test
    fun customComponent_textReturn() {
        val capturedDoc =
            setContent(
                name = "TextReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 1) {
                        remoteContext.overrideText(value, "Returned from Custom")
                    }
                },
            ) {
                val textState = rememberNamedRemoteString("named_text_return", "Initial")
                RemoteCustomComponent(name = "TextReturnCustom") { bindReturn(1, textState) }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("TextReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(1.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.TEXT_RETURN)

        val textVarId = remoteContext.getVariableId("USER:named_text_return")
        assertThat(prop.mIntValue).isEqualTo(textVarId)
        assertThat(remoteContext.getText(textVarId)).isEqualTo("Returned from Custom")
    }

    @Test
    fun customComponent_floatReturn() {
        val capturedDoc =
            setContent(
                name = "FloatReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 2) {
                        remoteContext.overrideFloat(value, 42.5f)
                    }
                },
            ) {
                val floatState = rememberNamedRemoteFloat("named_float_return") { 0f.rf }
                RemoteCustomComponent(name = "FloatReturnCustom") { bindReturn(2, floatState) }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("FloatReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(2.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.FLOAT_RETURN)

        val floatVarId = remoteContext.getVariableId("USER:named_float_return")
        assertThat(Utils.idFromNan(prop.mFloatValue)).isEqualTo(floatVarId)
        assertThat(remoteContext.getFloat(floatVarId)).isEqualTo(42.5f)
    }

    @Test
    fun customComponent_booleanReturn() {
        val capturedDoc =
            setContent(
                name = "BooleanReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 3) {
                        remoteContext.overrideInteger(value, 1)
                    }
                },
            ) {
                val boolState = rememberNamedRemoteBoolean("named_bool_return", false)
                RemoteCustomComponent(name = "BooleanReturnCustom") { bindReturn(3, boolState) }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("BooleanReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(3.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.INT_RETURN)

        val boolVarId = remoteContext.getVariableId("USER:named_bool_return")
        assertThat(prop.mIntValue).isEqualTo(boolVarId)
        assertThat(remoteContext.getInteger(boolVarId)).isEqualTo(1)
    }

    @Test
    fun customComponent_mutableBooleanReturn() {
        var returnVarId = -1
        val capturedDoc =
            setContent(
                name = "MutableBooleanReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 3) {
                        returnVarId = value
                        remoteContext.overrideInteger(value, 12345)
                    }
                },
            ) {
                val boolState = rememberMutableRemoteBoolean(false)
                RemoteCustomComponent(name = "MutableBooleanReturnCustom") {
                    bindReturn(3, boolState)
                }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("MutableBooleanReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(3.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.INT_RETURN)
        val expectedBoolId = prop.mIntValue

        assertThat(returnVarId).isEqualTo(expectedBoolId)
        assertThat(remoteContext.getInteger(expectedBoolId)).isEqualTo(12345)
    }

    @Test
    fun customComponent_colorReturn() {
        val capturedDoc =
            setContent(
                name = "ColorReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 4) {
                        remoteContext.mRemoteComposeState.overrideColor(value, 0xFF00FF00.toInt())
                    }
                },
            ) {
                val colorState = rememberNamedRemoteColor("named_color_return", Color.Red)
                RemoteCustomComponent(name = "ColorReturnCustom") { bindReturn(4, colorState) }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("ColorReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(4.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.COLOR_RETURN)

        val colorVarId = remoteContext.getVariableId("USER:named_color_return")
        assertThat(prop.mIntValue).isEqualTo(colorVarId)
        assertThat(remoteContext.getColor(colorVarId)).isEqualTo(0xFF00FF00.toInt())
    }

    private fun setContent(
        name: String,
        configureString: ((view: View, type: Int, value: String) -> Unit)? = null,
        configureInt: ((view: View, type: Int, value: Int) -> Unit)? = null,
        configureFloat: ((view: View, type: Int, value: Float) -> Unit)? = null,
        content: @RemoteComposable @Composable () -> Unit,
    ): CoreDocument {
        val delegate =
            object : AndroidComponentSupport {
                override fun createView(context: Context): View = View(context)

                override fun configure(view: View, type: Int, value: String) {
                    configureString?.invoke(view, type, value)
                }

                override fun configure(view: View, type: Int, value: Int) {
                    configureInt?.invoke(view, type, value)
                }

                override fun configure(view: View, type: Int, value: Float) {
                    configureFloat?.invoke(view, type, value)
                }
            }
        customSupport.registerDelegate(name, delegate)

        val creationDisplayInfo = RemoteCreationDisplayInfo(200, 200, 160, 1.0f)
        val profile = TestProfiles.androidXExperimental

        var capturedDoc: CoreDocument? = null
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            profile = profile,
            onCoreDocumentCreated = { capturedDoc = it },
            player =
                object : RemoteBaseContentTestRule.Player {
                    @Composable
                    override fun Play(coreDocument: CoreDocument, size: Size) {
                        RemoteDocumentPlayer(
                            document = coreDocument,
                            documentWidth = size.width.toInt(),
                            documentHeight = size.height.toInt(),
                            customSupport = customSupport,
                        )
                    }
                },
            composable = content,
        )
        assertThat(capturedDoc).isNotNull()
        return capturedDoc!!
    }

    private fun CoreDocument.findCustomOperations(): List<Custom> {
        val result = mutableListOf<Custom>()
        fun traverse(ops: List<Operation>) {
            for (op in ops) {
                if (op is Custom) {
                    result.add(op)
                }
                if (op is Container) {
                    traverse(op.list)
                }
            }
        }
        traverse(operations)
        return result
    }

    private val Custom.configId: Int
        get() = parseFromWireBuffer().first

    private val Custom.properties: List<CustomProperty>
        get() = parseFromWireBuffer().second

    private fun Custom.parseFromWireBuffer(): Pair<Int, List<CustomProperty>> {
        val buffer = WireBuffer()
        write(buffer)
        buffer.index = 0
        buffer.readByte() // Operations.LAYOUT_CUSTOM
        buffer.readInt() // componentId
        buffer.readInt() // animationId
        val configId = buffer.readId()
        val propCount = buffer.readInt()
        val properties = buildList {
            for (i in 0 until propCount) {
                val type = buffer.readShort().toShort()
                val dataType = buffer.readShort().toShort()
                if (
                    dataType == CustomProperty.FLOAT_PROP || dataType == CustomProperty.FLOAT_RETURN
                ) {
                    add(CustomProperty(type, dataType, buffer.readFloat()))
                } else {
                    add(CustomProperty(type, dataType, buffer.readInt()))
                }
            }
        }
        return configId to properties
    }
}
