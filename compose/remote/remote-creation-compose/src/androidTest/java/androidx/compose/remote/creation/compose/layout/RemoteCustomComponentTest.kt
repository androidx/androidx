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
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteString
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.util.TestProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.platform.AndroidComponentSupport
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteBaseContentTestRule
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    fun customComponent_mutableTextReturn() {
        var returnVarId = -1
        val capturedDoc =
            setContent(
                name = "MutableTextReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 1) {
                        returnVarId = value
                        remoteContext.overrideText(value, "Returned from Mutable Custom")
                    }
                },
            ) {
                val textState = rememberMutableRemoteString("Initial")
                RemoteCustomComponent(name = "MutableTextReturnCustom") { bindReturn(1, textState) }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("MutableTextReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(1.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.TEXT_RETURN)
        val expectedTextId = prop.mIntValue

        assertThat(returnVarId).isEqualTo(expectedTextId)
        assertThat(remoteContext.getText(expectedTextId)).isEqualTo("Returned from Mutable Custom")
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
    fun customComponent_mutableFloatReturn() {
        var returnVarId = -1
        val capturedDoc =
            setContent(
                name = "MutableFloatReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 2) {
                        returnVarId = value
                        remoteContext.overrideFloat(value, 98.6f)
                    }
                },
            ) {
                val floatState = rememberMutableRemoteFloat(0f)
                RemoteCustomComponent(name = "MutableFloatReturnCustom") {
                    bindReturn(2, floatState)
                }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("MutableFloatReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(2.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.FLOAT_RETURN)
        val expectedFloatId = Utils.idFromNan(prop.mFloatValue)

        assertThat(returnVarId).isEqualTo(expectedFloatId)
        assertThat(remoteContext.getFloat(expectedFloatId)).isEqualTo(98.6f)
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
    fun customComponent_intReturn() {
        val capturedDoc =
            setContent(
                name = "IntReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 5) {
                        remoteContext.overrideInteger(value, 99)
                    }
                },
            ) {
                val intState = rememberNamedRemoteInt("named_int_return", 0)
                RemoteCustomComponent(name = "IntReturnCustom") { bindReturn(5, intState) }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("IntReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(5.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.INT_RETURN)

        val intVarId = remoteContext.getVariableId("USER:named_int_return")
        assertThat(prop.mIntValue).isEqualTo(intVarId)
        assertThat(remoteContext.getInteger(intVarId)).isEqualTo(99)
    }

    @Test
    fun customComponent_mutableIntReturn() {
        var returnVarId = -1
        val capturedDoc =
            setContent(
                name = "MutableIntReturnCustom",
                configureInt = { _, type, value ->
                    if (type == 5) {
                        returnVarId = value
                        remoteContext.overrideInteger(value, 777)
                    }
                },
            ) {
                val intState = rememberMutableRemoteInt(0)
                RemoteCustomComponent(name = "MutableIntReturnCustom") { bindReturn(5, intState) }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        assertThat(capturedDoc.getText(customOp.configId)).isEqualTo("MutableIntReturnCustom")
        val prop = customOp.properties.single()
        assertThat(prop.mType).isEqualTo(5.toShort())
        assertThat(prop.mDataType).isEqualTo(CustomProperty.INT_RETURN)
        val expectedIntId = prop.mIntValue

        assertThat(returnVarId).isEqualTo(expectedIntId)
        assertThat(remoteContext.getInteger(expectedIntId)).isEqualTo(777)
    }

    @Test
    fun customComponent_intAndRemoteIntProperties() {
        val configuredInts = mutableMapOf<Int, Int>()
        val capturedDoc =
            setContent(
                name = "IntPropertiesCustom",
                configureInt = { _, type, value -> configuredInts[type] = value },
            ) {
                val namedInt = rememberNamedRemoteInt("named_int_prop", 200)
                RemoteCustomComponent(name = "IntPropertiesCustom") {
                    property(1, 42)
                    property(2, 100.ri)
                    property(3, namedInt)
                }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        val props = customOp.properties
        assertThat(props).hasSize(3)

        assertThat(props[0].mType).isEqualTo(1.toShort())
        assertThat(props[0].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(props[0].mIntValue).isEqualTo(42)

        assertThat(props[1].mType).isEqualTo(2.toShort())
        assertThat(props[1].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(props[1].mIntValue).isEqualTo(100)

        assertThat(props[2].mType).isEqualTo(3.toShort())
        assertThat(props[2].mDataType).isEqualTo(CustomProperty.INT_ID_PROP)
        assertThat(props[2].mIntValue).isEqualTo(remoteContext.getVariableId("USER:named_int_prop"))

        assertThat(configuredInts[1]).isEqualTo(42)
        assertThat(configuredInts[2]).isEqualTo(100)
        assertThat(configuredInts[3]).isEqualTo(200)
    }

    @Test
    fun customComponent_colorAndRemoteColorProperties() {
        val configuredInts = mutableMapOf<Int, Int>()
        val capturedDoc =
            setContent(
                name = "ColorPropertiesCustom",
                configureInt = { _, type, value -> configuredInts[type] = value },
            ) {
                val namedColor = rememberNamedRemoteColor("named_color_prop", Color.Blue)
                RemoteCustomComponent(name = "ColorPropertiesCustom") {
                    property(1, Color.Red)
                    property(2, Color.Green.rc)
                    property(3, namedColor)
                }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        val props = customOp.properties
        assertThat(props).hasSize(3)

        assertThat(props[0].mType).isEqualTo(1.toShort())
        assertThat(props[0].mDataType).isEqualTo(CustomProperty.COLOR_PROP)
        assertThat(props[0].mIntValue).isEqualTo(Color.Red.toArgb())

        assertThat(props[1].mType).isEqualTo(2.toShort())
        assertThat(props[1].mDataType).isEqualTo(CustomProperty.COLOR_PROP)
        assertThat(props[1].mIntValue).isEqualTo(Color.Green.toArgb())

        assertThat(props[2].mType).isEqualTo(3.toShort())
        assertThat(props[2].mDataType).isEqualTo(CustomProperty.COLOR_ID_PROP)
        assertThat(props[2].mIntValue)
            .isEqualTo(remoteContext.getVariableId("USER:named_color_prop"))

        assertThat(configuredInts[1]).isEqualTo(Color.Red.toArgb())
        assertThat(configuredInts[2]).isEqualTo(Color.Green.toArgb())
        assertThat(configuredInts[3]).isEqualTo(Color.Blue.toArgb())
    }

    @Test
    fun customComponent_floatRemoteFloatAndDpProperties() {
        val configuredFloats = mutableMapOf<Int, Float>()
        val capturedDoc =
            setContent(
                name = "FloatPropertiesCustom",
                configureFloat = { _, type, value -> configuredFloats[type] = value },
            ) {
                val namedFloat = rememberNamedRemoteFloat("named_float_prop") { 1.41f.rf }
                RemoteCustomComponent(name = "FloatPropertiesCustom") {
                    property(1, 3.14f)
                    property(2, 2.71f.rf)
                    property(3, namedFloat)
                    property(4, 16.rdp)
                }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        val props = customOp.properties
        assertThat(props).hasSize(4)

        assertThat(props[0].mType).isEqualTo(1.toShort())
        assertThat(props[0].mDataType).isEqualTo(CustomProperty.FLOAT_PROP)
        assertThat(props[0].mFloatValue).isEqualTo(3.14f)

        assertThat(props[1].mType).isEqualTo(2.toShort())
        assertThat(props[1].mDataType).isEqualTo(CustomProperty.FLOAT_PROP)
        assertThat(props[1].mFloatValue).isEqualTo(2.71f)

        assertThat(props[2].mType).isEqualTo(3.toShort())
        assertThat(props[2].mDataType).isEqualTo(CustomProperty.FLOAT_PROP)
        val floatVarId = remoteContext.getVariableId("USER:named_float_prop")
        assertThat(Utils.idFromNan(props[2].mFloatValue)).isEqualTo(floatVarId)

        assertThat(props[3].mType).isEqualTo(4.toShort())
        assertThat(props[3].mDataType).isEqualTo(CustomProperty.FLOAT_PROP)

        assertThat(configuredFloats[1]).isEqualTo(3.14f)
        assertThat(configuredFloats[2]).isEqualTo(2.71f)
        assertThat(configuredFloats[3]).isEqualTo(1.41f)
        assertThat(configuredFloats[4]).isEqualTo(16f)
    }

    @Test
    fun customComponent_stringAndRemoteStringProperties() {
        val configuredStrings = mutableMapOf<Int, String>()
        val capturedDoc =
            setContent(
                name = "StringPropertiesCustom",
                configureString = { _, type, value -> configuredStrings[type] = value },
            ) {
                val namedString = rememberNamedRemoteString("named_string_prop", "NamedRemote")
                RemoteCustomComponent(name = "StringPropertiesCustom") {
                    property(1, "Hello")
                    property(2, "ConstantRemote".rs)
                    property(3, namedString)
                }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        val props = customOp.properties
        assertThat(props).hasSize(3)

        assertThat(props[0].mType).isEqualTo(1.toShort())
        assertThat(props[0].mDataType).isEqualTo(CustomProperty.STRING_PROP)
        assertThat(capturedDoc.getText(props[0].mIntValue)).isEqualTo("Hello")

        assertThat(props[1].mType).isEqualTo(2.toShort())
        assertThat(props[1].mDataType).isEqualTo(CustomProperty.STRING_PROP)
        assertThat(capturedDoc.getText(props[1].mIntValue)).isEqualTo("ConstantRemote")

        assertThat(props[2].mType).isEqualTo(3.toShort())
        assertThat(props[2].mDataType).isEqualTo(CustomProperty.STRING_PROP)
        assertThat(props[2].mIntValue)
            .isEqualTo(remoteContext.getVariableId("USER:named_string_prop"))

        assertThat(configuredStrings[1]).isEqualTo("Hello")
        assertThat(configuredStrings[2]).isEqualTo("ConstantRemote")
        assertThat(configuredStrings[3]).isEqualTo("NamedRemote")
    }

    @Test
    fun customComponent_booleanAndRemoteBooleanProperties() {
        val configuredInts = mutableMapOf<Int, Int>()
        val capturedDoc =
            setContent(
                name = "BooleanPropertiesCustom",
                configureInt = { _, type, value -> configuredInts[type] = value },
            ) {
                val namedBoolTrue = rememberNamedRemoteBoolean("named_bool_true", true)
                val namedBoolFalse = rememberNamedRemoteBoolean("named_bool_false", false)
                RemoteCustomComponent(name = "BooleanPropertiesCustom") {
                    property(1, true)
                    property(2, false)
                    property(3, true.rb)
                    property(4, false.rb)
                    property(5, namedBoolTrue)
                    property(6, namedBoolFalse)
                }
            }

        val customOp = capturedDoc.findCustomOperations().single()
        val props = customOp.properties
        assertThat(props).hasSize(6)

        assertThat(props[0].mType).isEqualTo(1.toShort())
        assertThat(props[0].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(props[0].mIntValue).isEqualTo(1)

        assertThat(props[1].mType).isEqualTo(2.toShort())
        assertThat(props[1].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(props[1].mIntValue).isEqualTo(0)

        assertThat(props[2].mType).isEqualTo(3.toShort())
        assertThat(props[2].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(props[2].mIntValue).isEqualTo(1)

        assertThat(props[3].mType).isEqualTo(4.toShort())
        assertThat(props[3].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(props[3].mIntValue).isEqualTo(0)

        assertThat(props[4].mType).isEqualTo(5.toShort())
        assertThat(props[4].mDataType).isEqualTo(CustomProperty.INT_ID_PROP)
        assertThat(props[4].mIntValue)
            .isEqualTo(remoteContext.getVariableId("USER:named_bool_true"))

        assertThat(props[5].mType).isEqualTo(6.toShort())
        assertThat(props[5].mDataType).isEqualTo(CustomProperty.INT_ID_PROP)
        assertThat(props[5].mIntValue)
            .isEqualTo(remoteContext.getVariableId("USER:named_bool_false"))

        assertThat(configuredInts[1]).isEqualTo(1)
        assertThat(configuredInts[2]).isEqualTo(0)
        assertThat(configuredInts[3]).isEqualTo(1)
        assertThat(configuredInts[4]).isEqualTo(0)
        assertThat(configuredInts[5]).isEqualTo(1)
        assertThat(configuredInts[6]).isEqualTo(0)
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
