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

package androidx.compose.remote.creation.dsl

import org.junit.Assert.assertEquals
import org.junit.Test

class RcTypesTest {

    @Test
    fun testRcSp() {
        val sp1 = 16.rsp
        val sp2 = 16.5f.rsp
        val sp3 = 18.0.rsp

        assertEquals(16f, sp1.value)
        assertEquals(16.5f, sp2.value)
        assertEquals(18f, sp3.value)
    }

    @Test
    fun testRcDp() {
        val dp = 10.rdp
        assertEquals(10f, dp.value)
    }

    @Test
    fun testRcPx() {
        val px = 20.rpx
        assertEquals(20f, px.value)
    }

    @Test
    fun testCustomPropertyFactoriesAndAccessors() {
        val intProp = RcCustomProperty.int(1, 42)
        assertEquals(1.toShort(), intProp.mType)
        assertEquals(RcCustomProperty.INT_PROP, intProp.mDataType)
        assertEquals(42, intProp.mIntValue)

        val intDynamicProp = RcCustomProperty.int(2.toShort(), RcInteger(100L))
        assertEquals(2.toShort(), intDynamicProp.mType)
        assertEquals(RcCustomProperty.INT_ID_PROP, intDynamicProp.mDataType)
        assertEquals(100, intDynamicProp.mIntValue)

        val floatProp = RcCustomProperty.float(3, 3.14f)
        assertEquals(3.toShort(), floatProp.mType)
        assertEquals(RcCustomProperty.FLOAT_PROP, floatProp.mDataType)
        assertEquals(3.14f, floatProp.mFloatValue, 0.001f)

        val floatRcProp = RcCustomProperty.float(4.toShort(), RcFloat(2.5f))
        assertEquals(4.toShort(), floatRcProp.mType)
        assertEquals(RcCustomProperty.FLOAT_PROP, floatRcProp.mDataType)
        assertEquals(2.5f, floatRcProp.getFloatValue().toFloat(), 0.001f)

        val colorProp = RcCustomProperty.color(5, RcColorValue(0xFF00FF))
        assertEquals(5.toShort(), colorProp.mType)
        assertEquals(RcCustomProperty.COLOR_PROP, colorProp.mDataType)
        assertEquals(0xFF00FF, colorProp.mIntValue)

        val colorIdProp = RcCustomProperty.color(6.toShort(), RcColor(50))
        assertEquals(6.toShort(), colorIdProp.mType)
        assertEquals(RcCustomProperty.COLOR_ID_PROP, colorIdProp.mDataType)
        assertEquals(50, colorIdProp.getColorValue().id)

        val textProp = RcCustomProperty.text(7.toShort(), RcText(70))
        assertEquals(7.toShort(), textProp.mType)
        assertEquals(RcCustomProperty.STRING_PROP, textProp.mDataType)
        assertEquals(70, textProp.getStringValue().id)

        val testProfile =
            androidx.compose.remote.creation.profile.Profile(
                androidx.compose.remote.core.CoreDocument.DOCUMENT_API_LEVEL,
                androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX,
                androidx.compose.remote.core.RcPlatformServices.None,
            ) { _, profile, _ ->
                androidx.compose.remote.creation.RemoteComposeWriter(profile)
            }
        val writer = androidx.compose.remote.creation.RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val retFloat = RcCustomProperty.returnFloat(10.toShort(), scope)
        assertEquals(10.toShort(), retFloat.mType)
        assertEquals(RcCustomProperty.FLOAT_RETURN, retFloat.mDataType)

        val retText = RcCustomProperty.returnText(11.toShort(), scope)
        assertEquals(11.toShort(), retText.mType)
        assertEquals(RcCustomProperty.TEXT_RETURN, retText.mDataType)

        val retInt = RcCustomProperty.returnInt(12, scope)
        assertEquals(12.toShort(), retInt.mType)
        assertEquals(RcCustomProperty.INT_RETURN, retInt.mDataType)
        assertEquals(retInt.mIntValue.toLong(), retInt.getIntValue().id)

        val retColor = RcCustomProperty.returnColor(13, scope)
        assertEquals(13.toShort(), retColor.mType)
        assertEquals(RcCustomProperty.COLOR_RETURN, retColor.mDataType)
        assertEquals(retColor.mIntValue, retColor.getColorValue().id)
    }

    @Test
    fun testHandlesAndFactories() {
        assertEquals(0, RcShader.None.id)
        assertEquals(42, RcComponentId.of(42).id)

        val macroArg = RcMacroArg(99)
        assertEquals(99, macroArg.asText().id)
        assertEquals(99, macroArg.asColor().id)
        assertEquals(99, macroArg.asImage().id)
    }
}
