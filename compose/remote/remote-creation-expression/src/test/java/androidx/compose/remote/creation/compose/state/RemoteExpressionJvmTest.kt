/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComponent
import androidx.compose.remote.creation.compose.layout.RemoteComponentToken
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests AST expression creation, evaluation, and constant folding on pure JVM without any Android
 * or Compose UI runtime dependencies.
 */
@RunWith(JUnit4::class)
class RemoteExpressionJvmTest {

    @Test
    fun testRemoteFloatConstantFolding() {
        val a = 3f.rf
        val b = 4f.rf
        val sum = a + b
        assertThat(sum.constantValue).isEqualTo(7f)

        val prod = a * b
        assertThat(prod.constantValue).isEqualTo(12f)

        val diff = b - a
        assertThat(diff.constantValue).isEqualTo(1f)

        val quot = 12f.rf / 4f.rf
        assertThat(quot.constantValue).isEqualTo(3f)
    }

    @Test
    fun testRemoteFloatComparison() {
        val a = 5f.rf
        val b = 10f.rf
        val lt = a.isLessThan(b)
        assertThat(lt.constantValue).isTrue()

        val gt = a.isGreaterThan(b)
        assertThat(gt.constantValue).isFalse()

        val eq = a.isEqualTo(5f.rf)
        assertThat(eq.constantValue).isTrue()
    }

    @Test
    fun testRemoteIntOperations() {
        val x = 10.ri
        val y = 20.ri
        val sum = x + y
        assertThat(sum.constantValue).isEqualTo(30)

        val prod = x * 2.ri
        assertThat(prod.constantValue).isEqualTo(20)

        val condition = x.isLessThan(y)
        val selected = condition.select(ifTrue = 100.ri, ifFalse = 200.ri)
        assertThat(selected.constantValue).isEqualTo(100)
    }

    @Test
    fun testRemoteBooleanLogic() {
        val t = true.rb
        val f = false.rb

        assertThat((!t).constantValue).isFalse()
        assertThat((!f).constantValue).isTrue()
        assertThat((t and f).constantValue).isFalse()
        assertThat((t or f).constantValue).isTrue()
    }

    @Test
    fun testRemoteColorFromArgb() {
        val colorInt = 0xFF112233.toInt()
        val color = RemoteColor(colorInt)
        assertThat(color.constantValue).isEqualTo(colorInt)
        assertThat(color.alpha.constantValue).isWithin(0.01f).of(1.0f)
        assertThat(color.red.constantValue).isWithin(0.01f).of(0x11 / 255f)
        assertThat(color.green.constantValue).isWithin(0.01f).of(0x22 / 255f)
        assertThat(color.blue.constantValue).isWithin(0.01f).of(0x33 / 255f)
    }

    @Test
    fun testRemoteColorRgb() {
        val color = RemoteColor.rgb(red = 1f.rf, green = 0f.rf, blue = 0f.rf, alpha = 1f.rf)
        assertThat(color.constantValue).isEqualTo(0xFFFF0000.toInt())
    }

    @Test
    fun testRemoteColorInvoke_rgbaFloats() {
        // Positional RGBA order: red, green, blue, alpha
        val colorWithAlpha = RemoteColor(1f, 0f, 0f, 0.5f)
        assertThat(colorWithAlpha.red.constantValue).isWithin(0.01f).of(1.0f)
        assertThat(colorWithAlpha.green.constantValue).isWithin(0.01f).of(0.0f)
        assertThat(colorWithAlpha.blue.constantValue).isWithin(0.01f).of(0.0f)
        assertThat(colorWithAlpha.alpha.constantValue).isWithin(0.01f).of(0.5f)

        // Default alpha = 1f
        val opaqueColor = RemoteColor(0f, 1f, 0f)
        assertThat(opaqueColor.red.constantValue).isWithin(0.01f).of(0.0f)
        assertThat(opaqueColor.green.constantValue).isWithin(0.01f).of(1.0f)
        assertThat(opaqueColor.blue.constantValue).isWithin(0.01f).of(0.0f)
        assertThat(opaqueColor.alpha.constantValue).isWithin(0.01f).of(1.0f)
        assertThat(opaqueColor.constantValue).isEqualTo(0xFF00FF00.toInt())
    }

    @Test
    fun testRemoteColorInvoke_rgbaRemoteFloats() {
        val color = RemoteColor(0f.rf, 0f.rf, 1f.rf, 0.8f.rf)
        assertThat(color.red.constantValue).isWithin(0.01f).of(0.0f)
        assertThat(color.green.constantValue).isWithin(0.01f).of(0.0f)
        assertThat(color.blue.constantValue).isWithin(0.01f).of(1.0f)
        assertThat(color.alpha.constantValue).isWithin(0.01f).of(0.8f)

        val opaqueColor = RemoteColor(1f.rf, 1f.rf, 0f.rf)
        assertThat(opaqueColor.alpha.constantValue).isWithin(0.01f).of(1.0f)
        assertThat(opaqueColor.constantValue).isEqualTo(0xFFFFFF00.toInt())
    }

    @Test
    fun testRemoteColorRgb_floats() {
        val color = RemoteColor.rgb(1f, 0f, 0f, 1f)
        assertThat(color.constantValue).isEqualTo(0xFFFF0000.toInt())

        val opaqueBlue = RemoteColor.rgb(0f, 0f, 1f)
        assertThat(opaqueBlue.constantValue).isEqualTo(0xFF0000FF.toInt())
    }

    @Test
    fun testRemoteDpOperations() {
        val dp1 = RemoteDp(16f.rf)
        val dp2 = RemoteDp(8f.rf)
        val sum = dp1 + dp2
        assertThat(sum.constantValue).isEqualTo(24f)

        val scaled = dp1 * 2f
        assertThat(scaled.constantValue).isEqualTo(32f)
    }

    @Test
    fun testRemoteStringOperations() {
        val hello = "Hello".rs
        val world = "World".rs
        val message = hello + " ".rs + world
        assertThat(message.constantValue).isEqualTo("Hello World")
    }

    @Test
    fun testRemoteTextUnitSp() {
        val sp = 14f.rsp
        assertThat(sp.type).isEqualTo(RemoteTextUnitType.Sp)
        assertThat(sp.value.constantValue).isEqualTo(14f)
    }

    @Test
    fun testMutableRemoteImageBitmap() {
        val mutableBitmap = MutableRemoteImageBitmap.createMutableForId(42)
        val creationState = RemoteComposeCreationState()
        assertThat(mutableBitmap.writeToDocument(creationState)).isEqualTo(42)
        assertThat(mutableBitmap.cacheKey).isEqualTo(RemoteStateIdKey(42))
    }

    enum class TestStatus {
        Active,
        Inactive,
    }

    @Test
    fun testRemoteEnum_computeRequiredCodePointSet_allConstants() {
        val dynamicInt = RemoteInt.createNamedRemoteInt("status", 0)
        val enumState = RemoteEnum(dynamicInt, TestStatus.entries)
        val remoteStr = enumState.toRemoteString()
        val creationState = RemoteComposeCreationState()
        val codePoints = remoteStr.computeRequiredCodePointSet(creationState)
        assertThat(codePoints as Any?).isNotNull()
        assertThat(codePoints).containsAtLeast("A", "c", "t", "i", "v", "e", "I", "n")
    }

    @Test
    fun testRemoteEnum_computeRequiredCodePointSet_unknownReturnsNull() {
        val dynamicInt = RemoteInt.createNamedRemoteInt("status", 0)
        val enumState = RemoteEnum(dynamicInt, TestStatus.entries)
        val remoteStr = enumState.toRemoteString {
            if (it == TestStatus.Active) {
                RemoteString.createNamedRemoteString("dyn", "Active")
            } else {
                it.name.rs
            }
        }
        val creationState = RemoteComposeCreationState()
        val codePoints = remoteStr.computeRequiredCodePointSet(creationState)
        assertThat(codePoints as Any?).isNull()
    }

    @Test
    fun testRemoteStringArray_computeRequiredCodePointSet_unknownReturnsNull() {
        val dynamicString = RemoteString.createNamedRemoteString("dynamic", "fallback")
        val stringArray = RemoteStringArray(listOf("Constant".rs, dynamicString))
        val deref = stringArray[RemoteInt.createNamedRemoteInt("idx", 0)]
        val creationState = RemoteComposeCreationState()
        val codePoints = deref.computeRequiredCodePointSet(creationState)
        assertThat(codePoints as Any?).isNull()
    }

    @Test
    fun testRemoteStringArray_computeRequiredCodePointSet_allConstants() {
        val stringArray = RemoteStringArray(listOf("AB".rs, "BC".rs))
        val deref = stringArray[RemoteInt.createNamedRemoteInt("idx", 0)]
        val creationState = RemoteComposeCreationState()
        val codePoints = deref.computeRequiredCodePointSet(creationState)
        assertThat(codePoints as Any?).isNotNull()
        assertThat(codePoints).containsExactly("A", "B", "C")
    }

    @Test
    fun testCustomVisitor_collectsNamedVariables() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 2f)
        val c = RemoteFloat.createNamedRemoteFloat("c", 3f)
        val expr = (a + b) * c

        val names = expr.accept { key, _, visitedArgs ->
            val fromArgs = visitedArgs.flatten().toSet()
            if (key is RemoteNamedCacheKey) fromArgs + key.name else fromArgs
        }
        assertThat(names).containsExactly("a", "b", "c")
    }

    @Test
    fun testRemoteComponent_tokenPropertiesAndEquality() {
        val token = RemoteComponentToken()
        val component = RemoteComponent(token)

        assertThat(token.isRequested(RemoteComponentToken.PROP_WIDTH)).isFalse()
        assertThat(component.width.hasConstantValue).isFalse()
        assertThat(token.isRequested(RemoteComponentToken.PROP_WIDTH)).isTrue()

        assertThat(token.isRequested(RemoteComponentToken.PROP_HEIGHT)).isFalse()
        assertThat(component.height.hasConstantValue).isFalse()
        assertThat(token.isRequested(RemoteComponentToken.PROP_HEIGHT)).isTrue()

        val centerToken = RemoteComponentToken()
        val centerComponent = RemoteComponent(centerToken)
        assertThat(centerComponent.centerX.hasConstantValue).isFalse()
        assertThat(centerToken.isRequested(RemoteComponentToken.PROP_WIDTH)).isTrue()
        assertThat(centerComponent.centerY.hasConstantValue).isFalse()
        assertThat(centerToken.isRequested(RemoteComponentToken.PROP_HEIGHT)).isTrue()

        assertThat(component.x.hasConstantValue).isFalse()
        assertThat(token.isRequested(RemoteComponentToken.PROP_X)).isTrue()
        assertThat(component.y.hasConstantValue).isFalse()
        assertThat(token.isRequested(RemoteComponentToken.PROP_Y)).isTrue()
        assertThat(component.rootX.hasConstantValue).isFalse()
        assertThat(token.isRequested(RemoteComponentToken.PROP_ROOT_X)).isTrue()
        assertThat(component.rootY.hasConstantValue).isFalse()
        assertThat(token.isRequested(RemoteComponentToken.PROP_ROOT_Y)).isTrue()

        assertThat(component).isEqualTo(RemoteComponent(token))
        assertThat(component.hashCode()).isEqualTo(RemoteComponent(token).hashCode())
        assertThat(component).isNotEqualTo(centerComponent)
    }

    @Test
    fun testCreateNamedRemoteColor_constantValueParity() {
        val creationState =
            RemoteComposeCreationState().apply {
                document = RemoteComposeWriter(RcPlatformServices.None)
            }
        val constantColor = 0xFFFF0000.toInt().rc
        val namedColor = RemoteColor.createNamedRemoteColor("primary", constantColor)
        val id = namedColor.getIdForCreationState(creationState)

        val directNamedColor = RemoteColor.createNamedRemoteColor("primary_int", 0xFFFF0000.toInt())
        val directId = directNamedColor.getIdForCreationState(creationState)

        assertThat(namedColor.constantValueOrNull).isNull()
        assertThat(directNamedColor.constantValueOrNull).isNull()

        val buffer = creationState.document.buffer
        buffer.buffer.index = 0
        val operations = ArrayList<Operation>()
        buffer.inflateFromBuffer(operations)

        val namedVars = operations.filterIsInstance<NamedVariable>()
        assertThat(namedVars.any { it.mVarId == id && it.mVarName == "USER:primary" }).isTrue()
        assertThat(namedVars.any { it.mVarId == directId && it.mVarName == "USER:primary_int" })
            .isTrue()
    }

    @Test
    fun testCreateNamedRemoteColor_dynamicExpressionUniqueness() {
        val creationState =
            RemoteComposeCreationState().apply {
                document = RemoteComposeWriter(RcPlatformServices.None)
            }
        val dynamicExpr =
            RemoteColor.rgb(
                red = RemoteFloat.createNamedRemoteFloat("r", 0.5f),
                green = 0.5f.rf,
                blue = 0.5f.rf,
            )
        val namedColor1 = RemoteColor.createNamedRemoteColor("color1", dynamicExpr)
        val namedColor2 = RemoteColor.createNamedRemoteColor("color2", dynamicExpr)

        val id1 = namedColor1.getIdForCreationState(creationState)
        val id2 = namedColor2.getIdForCreationState(creationState)

        assertThat(id1).isNotEqualTo(id2)

        val buffer = creationState.document.buffer
        buffer.buffer.index = 0
        val operations = ArrayList<Operation>()
        buffer.inflateFromBuffer(operations)

        val colorExpressions = operations.filterIsInstance<ColorExpression>()
        assertThat(colorExpressions.any { it.mId == id1 }).isTrue()
        assertThat(colorExpressions.any { it.mId == id2 }).isTrue()

        val namedVars = operations.filterIsInstance<NamedVariable>()
        assertThat(namedVars.any { it.mVarId == id1 && it.mVarName == "USER:color1" }).isTrue()
        assertThat(namedVars.any { it.mVarId == id2 && it.mVarName == "USER:color2" }).isTrue()
    }
}
