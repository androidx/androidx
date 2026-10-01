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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.util.RemoteDocumentTestRule
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Config.TARGET_SDK],
    qualifiers = "xhdpi",
)
class RemoteTextUnitTest {

    @get:Rule val remoteComposeTestRule = RemoteDocumentTestRule()

    private val context: RemoteContext
        get() = remoteComposeTestRule.context

    @Test
    fun constructor_createsCorrectly() {
        val floatValue = 16f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteTextUnit = RemoteTextUnit(remoteFloat, TextUnitType.Sp)

        assertThat(remoteTextUnit.value).isEqualTo(remoteFloat)
        assertThat(remoteTextUnit.type).isEqualTo(RemoteTextUnitType.Sp)
        assertThat(remoteTextUnit.asConstantTextUnitOrNull()).isEqualTo(floatValue.sp)
        assertThat(remoteTextUnit.asConstantTextUnit()).isEqualTo(floatValue.sp)
    }

    @Test
    fun constructors_createCorrectly() {
        val floatValue = 16f
        val remoteFloat = RemoteFloat(floatValue)

        val fromRemoteFloatDefault = RemoteTextUnit(remoteFloat)
        assertThat(fromRemoteFloatDefault.value).isEqualTo(remoteFloat)
        assertThat(fromRemoteFloatDefault.type).isEqualTo(RemoteTextUnitType.Sp)

        val fromRemoteFloatExplicit = RemoteTextUnit(remoteFloat, RemoteTextUnitType.Sp)
        assertThat(fromRemoteFloatExplicit.value).isEqualTo(remoteFloat)
        assertThat(fromRemoteFloatExplicit.type).isEqualTo(RemoteTextUnitType.Sp)

        val fromFloatDefault = RemoteTextUnit(floatValue)
        assertThat(fromFloatDefault.value.constantValue).isEqualTo(floatValue)
        assertThat(fromFloatDefault.type).isEqualTo(RemoteTextUnitType.Sp)

        val fromFloatExplicit = RemoteTextUnit(floatValue, RemoteTextUnitType.Sp)
        assertThat(fromFloatExplicit.value.constantValue).isEqualTo(floatValue)
        assertThat(fromFloatExplicit.type).isEqualTo(RemoteTextUnitType.Sp)
    }

    @Test
    fun extensionProperties_createCorrectly() {
        assertThat(16.rsp.value.constantValue).isEqualTo(16f)
        assertThat(16.rsp.type).isEqualTo(RemoteTextUnitType.Sp)
        assertThat(16.5f.rsp.value.constantValue).isEqualTo(16.5f)
        assertThat(16.5f.rsp.type).isEqualTo(RemoteTextUnitType.Sp)
        assertThat(18f.rf.rsp.value.constantValue).isEqualTo(18f)
        assertThat(18f.rf.rsp.type).isEqualTo(RemoteTextUnitType.Sp)
        assertThat(16.sp.asRemoteTextUnit().value.constantValue).isEqualTo(16f)
        assertThat(16.sp.asRemoteTextUnit().type).isEqualTo(RemoteTextUnitType.Sp)
    }

    @Test
    @Config(fontScale = 1.5f)
    fun remoteFloatRsp_dynamicExpression_calculatesToPxCorrectly() {
        val (dynamicPxId, staticPxId) =
            remoteComposeTestRule.initialise {
                val baseSize = RemoteFloat.createNamedRemoteFloat("baseSize", 14f)
                val dynamicTextUnit = (baseSize * 2f).rsp
                val staticTextUnit = 28.rsp
                Pair(
                    dynamicTextUnit.toPx().getIdForCreationState(it),
                    staticTextUnit.toPx().getIdForCreationState(it),
                )
            }

        assertThat(context.getFloat(dynamicPxId)).isEqualTo(context.getFloat(staticPxId))
    }

    @Test
    fun toPx_sp_calculatesCorrectly() {
        val intValue = 16
        // Default xhdpi density is 2.0
        val density = 2f
        // Default font scale is 1.0 from RemoteDensity.HOST or similar
        val fontScale = 1.0f

        val (resultId, resultPxId) =
            remoteComposeTestRule.initialise {
                val remoteTextUnit = intValue.rsp
                val remoteFloatPx = remoteTextUnit.toPx()

                val resultId = remoteTextUnit.value.getIdForCreationState(it)
                val resultPxId = remoteFloatPx.getIdForCreationState(it)
                Pair(resultId, resultPxId)
            }

        assertThat(context.getFloat(resultId)).isEqualTo(intValue)
        assertThat(context.getFloat(resultPxId)).isEqualTo(intValue * density * fontScale)
    }

    @Test
    fun constructor_unsupportedTextUnitType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteTextUnit(1.5f.rf, TextUnitType.Em)
        }
    }

    @Test
    @Config(fontScale = 2.5f)
    fun toRsp_isNonScalableWithFontSize() {
        val dpValue = 12.dp

        val (nonScalableTextId, scalableTextId) =
            remoteComposeTestRule.initialise {
                val nonScalable = dpValue.toRsp() // font-scale independent
                val scalable = 12.rsp // font-scale dependent

                Pair(
                    nonScalable.toPx().getIdForCreationState(it),
                    scalable.toPx().getIdForCreationState(it),
                )
            }

        // 12.dp * 2.0 = 24.0 px (regardless of font scale)
        assertThat(context.getFloat(nonScalableTextId)).isEqualTo(24f)

        // 12.rsp * 2.0 (density) * 2.5 (fontScale) = 60.0 px
        assertThat(context.getFloat(scalableTextId)).isEqualTo(60f)
    }
}
