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

package androidx.compose.remote.document

import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RcProfiles as CoreRcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PathAppend
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.core.operations.Skip
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.remote.core.operations.utilities.StringUtils
import androidx.compose.remote.core.operations.utilities.easing.CubicEasing
import androidx.compose.remote.core.operations.utilities.easing.Easing
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import androidx.compose.remote.core.semantics.AccessibleComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Bit-for-bit parity tests verifying schema-generated `RemoteProtocol` constants, NaN/Long ID
 * helpers, animation spec packing, and paint constants against `remote-core`.
 */
class RemoteProtocolParityTest {

    private fun assertRawFloatArrayEquals(
        expected: FloatArray,
        actual: FloatArray,
        message: String? = null,
    ) {
        assertEquals(expected.size, actual.size, message ?: "FloatArray size mismatch")
        for (i in expected.indices) {
            assertEquals(
                expected[i].toRawBits(),
                actual[i].toRawBits(),
                "${message ?: "Mismatch"} at index $i: expected=${expected[i]} (0x${expected[i].toRawBits().toUInt().toString(16)}) " +
                    "actual=${actual[i]} (0x${actual[i].toRawBits().toUInt().toString(16)})",
            )
        }
    }

    @Test
    fun testRcProfilesParityAcrossApiLevelsAndMasks() {
        assertEquals(CoreRcProfiles.PROFILE_BASELINE, RcProfiles.PROFILE_BASELINE)
        assertEquals(CoreRcProfiles.PROFILE_EXPERIMENTAL, RcProfiles.PROFILE_EXPERIMENTAL)
        assertEquals(CoreRcProfiles.PROFILE_DEPRECATED, RcProfiles.PROFILE_DEPRECATED)
        assertEquals(CoreRcProfiles.PROFILE_OEM, RcProfiles.PROFILE_OEM)
        assertEquals(CoreRcProfiles.PROFILE_LOW_POWER, RcProfiles.PROFILE_LOW_POWER)
        assertEquals(CoreRcProfiles.PROFILE_WIDGETS, RcProfiles.PROFILE_WIDGETS)
        assertEquals(CoreRcProfiles.PROFILE_ANDROIDX, RcProfiles.PROFILE_ANDROIDX)
        assertEquals(CoreRcProfiles.PROFILE_ANDROID_NATIVE, RcProfiles.PROFILE_ANDROID_NATIVE)
        assertEquals(CoreRcProfiles.PROFILE_WEAR_WIDGETS, RcProfiles.PROFILE_WEAR_WIDGETS)

        val apiLevels = intArrayOf(6, 7, 8)
        val profileMasks =
            intArrayOf(
                RcProfiles.PROFILE_BASELINE,
                RcProfiles.PROFILE_ANDROIDX,
                RcProfiles.PROFILE_WIDGETS,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_WIDGETS,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                RcProfiles.PROFILE_WIDGETS or RcProfiles.PROFILE_EXPERIMENTAL,
                RcProfiles.PROFILE_ANDROIDX or
                    RcProfiles.PROFILE_WIDGETS or
                    RcProfiles.PROFILE_EXPERIMENTAL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_DEPRECATED,
                RcProfiles.PROFILE_WIDGETS or
                    RcProfiles.PROFILE_DEPRECATED or
                    RcProfiles.PROFILE_EXPERIMENTAL,
            )

        val knownSchemaOpcodes =
            RcProfiles.supportedOpcodes(
                8,
                RcProfiles.PROFILE_ANDROIDX or
                    RcProfiles.PROFILE_EXPERIMENTAL or
                    RcProfiles.PROFILE_DEPRECATED,
            ) +
                RcProfiles.supportedOpcodes(
                    8,
                    RcProfiles.PROFILE_WIDGETS or
                        RcProfiles.PROFILE_EXPERIMENTAL or
                        RcProfiles.PROFILE_DEPRECATED,
                )

        for (apiLevel in apiLevels) {
            for (mask in profileMasks) {
                val supportedSet = RcProfiles.supportedOpcodes(apiLevel, mask)
                val includesExperimental = (mask and RcProfiles.PROFILE_EXPERIMENTAL) != 0
                for (opcode in 0..255) {
                    val actual = RcProfiles.isOpcodeSupported(opcode, apiLevel, mask)
                    assertEquals(
                        actual,
                        opcode in supportedSet,
                        "RcProfiles.supportedOpcodes mismatch for opcode=$opcode apiLevel=$apiLevel mask=0x${mask.toString(16)}",
                    )
                    // Experimental ops in remote-core may land before their wire schema is added
                    // to operations.json; enforce strict 0..255 parity for non-experimental
                    // profiles and every schema-modeled opcode.
                    if (includesExperimental && opcode !in knownSchemaOpcodes) continue
                    val expected = Operations.valid(opcode, apiLevel, mask)
                    assertEquals(
                        expected,
                        actual,
                        "RcProfiles.isOpcodeSupported mismatch for opcode=$opcode apiLevel=$apiLevel mask=0x${mask.toString(16)}",
                    )
                }
            }
        }
    }

    @Test
    fun testFloatAndIntRpnParity() {
        assertEquals(AnimatedFloatExpression.OFFSET, FloatRpn.OFFSET)
        assertEquals(AnimatedFloatExpression.ADD.toRawBits(), FloatRpn.ADD.toRawBits())
        assertEquals(AnimatedFloatExpression.SUB.toRawBits(), FloatRpn.SUB.toRawBits())
        assertEquals(AnimatedFloatExpression.MUL.toRawBits(), FloatRpn.MUL.toRawBits())
        assertEquals(AnimatedFloatExpression.DIV.toRawBits(), FloatRpn.DIV.toRawBits())
        assertEquals(AnimatedFloatExpression.MAD.toRawBits(), FloatRpn.MAD.toRawBits())
        assertEquals(AnimatedFloatExpression.IFELSE.toRawBits(), FloatRpn.IFELSE.toRawBits())
        assertEquals(AnimatedFloatExpression.CLAMP.toRawBits(), FloatRpn.CLAMP.toRawBits())
        assertEquals(AnimatedFloatExpression.DEG.toRawBits(), FloatRpn.DEG.toRawBits())
        assertEquals(AnimatedFloatExpression.RAD.toRawBits(), FloatRpn.RAD.toRawBits())
        assertEquals(AnimatedFloatExpression.A_DEREF.toRawBits(), FloatRpn.A_DEREF.toRawBits())
        assertEquals(AnimatedFloatExpression.A_MAX.toRawBits(), FloatRpn.A_MAX.toRawBits())
        assertEquals(AnimatedFloatExpression.A_MIN.toRawBits(), FloatRpn.A_MIN.toRawBits())
        assertEquals(AnimatedFloatExpression.A_SUM.toRawBits(), FloatRpn.A_SUM.toRawBits())
        assertEquals(AnimatedFloatExpression.A_AVG.toRawBits(), FloatRpn.A_AVG.toRawBits())
        assertEquals(AnimatedFloatExpression.A_LEN.toRawBits(), FloatRpn.A_LEN.toRawBits())
        assertEquals(AnimatedFloatExpression.A_SPLINE.toRawBits(), FloatRpn.A_SPLINE.toRawBits())
        assertEquals(AnimatedFloatExpression.LERP.toRawBits(), FloatRpn.LERP.toRawBits())
        assertEquals(
            AnimatedFloatExpression.SMOOTH_STEP.toRawBits(),
            FloatRpn.SMOOTH_STEP.toRawBits(),
        )
        assertEquals(AnimatedFloatExpression.STORE_RO.toRawBits(), FloatRpn.STORE_R0.toRawBits())
        assertEquals(AnimatedFloatExpression.LOAD_R0.toRawBits(), FloatRpn.LOAD_R0.toRawBits())
        assertEquals(AnimatedFloatExpression.CUBIC.toRawBits(), FloatRpn.CUBIC.toRawBits())
        assertEquals(AnimatedFloatExpression.A_LERP.toRawBits(), FloatRpn.A_LERP.toRawBits())

        assertTrue(FloatRpn.isOperator(FloatRpn.ADD))
        assertEquals(FloatRpn.ID_A_LERP, FloatRpn.operatorId(FloatRpn.A_LERP))
        assertTrue(FloatRpn.isSupportedAtApiLevel(FloatRpn.SMOOTH_STEP, 6))
        assertFalse(FloatRpn.isSupportedAtApiLevel(FloatRpn.LOG2, 6))
        assertTrue(FloatRpn.isSupportedAtApiLevel(FloatRpn.A_LERP, 7))
        assertTrue(FloatRpn.ADD.toMaybeVariableFloat().isRpnOperator)

        assertEquals(IntegerExpressionEvaluator.OFFSET, IntRpn.OFFSET)
        assertEquals(IntegerExpressionEvaluator.I_ADD, IntRpn.I_ADD)
        assertEquals(IntegerExpressionEvaluator.I_SUB, IntRpn.I_SUB)
        assertEquals(IntegerExpressionEvaluator.I_MUL, IntRpn.I_MUL)
        assertEquals(IntegerExpressionEvaluator.I_DIV, IntRpn.I_DIV)
        assertEquals(IntegerExpressionEvaluator.I_SHL, IntRpn.I_SHL)
        assertEquals(IntegerExpressionEvaluator.I_USHR, IntRpn.I_USHR)
        assertEquals(IntegerExpressionEvaluator.I_CLAMP, IntRpn.I_CLAMP)
        assertEquals(IntegerExpressionEvaluator.I_IFELSE, IntRpn.I_IFELSE)
        assertEquals(IntegerExpressionEvaluator.I_MAD, IntRpn.I_MAD)
        assertEquals(IntegerExpressionEvaluator.I_VAR1, IntRpn.I_VAR1)
        assertEquals(IntegerExpressionEvaluator.I_VAR2, IntRpn.I_VAR2)
        assertTrue(IntRpn.isOperatorCode(IntRpn.I_IFELSE))
        assertFalse(IntRpn.isOperatorCode(42))
    }

    @Test
    fun testSystemVariablesAndTypedIdHelpersParity() {
        assertEquals(RemoteComposeState.START_ID, NanEncoding.START_ID)
        assertEquals(NanMap.START_ARRAY, NanEncoding.START_ARRAY_ID)

        assertEquals(
            RemoteContext.FLOAT_CONTINUOUS_SEC.toRawBits(),
            RemoteContextIds.FLOAT_CONTINUOUS_SEC.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_TIME_IN_SEC.toRawBits(),
            RemoteContextIds.FLOAT_TIME_IN_SEC.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_TIME_IN_MIN.toRawBits(),
            RemoteContextIds.FLOAT_TIME_IN_MIN.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_TIME_IN_HR.toRawBits(),
            RemoteContextIds.FLOAT_TIME_IN_HR.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_WINDOW_WIDTH.toRawBits(),
            RemoteContextIds.FLOAT_WINDOW_WIDTH.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_WINDOW_HEIGHT.toRawBits(),
            RemoteContextIds.FLOAT_WINDOW_HEIGHT.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_COMPONENT_WIDTH.toRawBits(),
            RemoteContextIds.FLOAT_COMPONENT_WIDTH.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_COMPONENT_HEIGHT.toRawBits(),
            RemoteContextIds.FLOAT_COMPONENT_HEIGHT.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_DENSITY.toRawBits(),
            RemoteContextIds.FLOAT_DENSITY.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_FONT_SIZE.toRawBits(),
            RemoteContextIds.FLOAT_FONT_SIZE.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_TOUCH_POS_X.toRawBits(),
            RemoteContextIds.FLOAT_TOUCH_POS_X.rawBits,
        )
        assertEquals(
            RemoteContext.FLOAT_TOUCH_POS_Y.toRawBits(),
            RemoteContextIds.FLOAT_TOUCH_POS_Y.rawBits,
        )
        assertEquals(RemoteContext.INT_EPOCH_SECOND, RemoteContextIds.INT_EPOCH_SECOND)

        val floatId = RemoteFloatId(42)
        assertEquals(Utils.asNan(42).toRawBits(), floatId.toMaybeVariableFloat().rawBits)
        assertEquals(42, NanEncoding.idFromNan(floatId.toNanFloat()))
        assertFalse(floatId.isSystemVariable)
        assertTrue(RemoteFloatId(RemoteContextIds.ID_DENSITY).isSystemVariable)

        val arrayId = RemoteArrayId(NanEncoding.START_ARRAY_ID)
        assertTrue(NanEncoding.isArrayNan(arrayId.toNanFloat()))
        assertTrue(arrayId.toMaybeVariableFloat().isArrayVariable)

        val intId = RemoteIntId(55)
        val encodedLong = intId.toLongEncodedId()
        assertTrue(NanEncoding.isLongId(encodedLong))
        assertEquals(55, NanEncoding.decodeLongId(encodedLong))
    }

    @Test
    fun testConstantsAndBitPackersParity() {
        assertEquals(Header.DOC_WIDTH, DocumentHeaderTags.DOC_WIDTH)
        assertEquals(Header.DOC_HEIGHT, DocumentHeaderTags.DOC_HEIGHT)
        assertEquals(Header.DOC_PROFILES, DocumentHeaderTags.DOC_PROFILES)
        assertEquals(Header.COMPRESS, DocumentHeaderTags.COMPRESS)

        assertEquals(DimensionModifierOperation.Type.EXACT.ordinal, DimensionType.EXACT)
        assertEquals(DimensionModifierOperation.Type.FILL.ordinal, DimensionType.FILL)
        assertEquals(DimensionModifierOperation.Type.WRAP.ordinal, DimensionType.WRAP)
        assertEquals(DimensionModifierOperation.Type.WEIGHT.ordinal, DimensionType.WEIGHT)
        assertEquals(
            DimensionModifierOperation.Type.INTRINSIC_MIN.ordinal,
            DimensionType.INTRINSIC_MIN,
        )
        assertEquals(
            DimensionModifierOperation.Type.INTRINSIC_MAX.ordinal,
            DimensionType.INTRINSIC_MAX,
        )
        assertEquals(DimensionModifierOperation.Type.EXACT_DP.ordinal, DimensionType.EXACT_DP)
        assertEquals(
            DimensionModifierOperation.Type.FILL_PARENT_MAX_WIDTH.ordinal,
            DimensionType.FILL_PARENT_MAX_WIDTH,
        )
        assertEquals(
            DimensionModifierOperation.Type.FILL_PARENT_MAX_HEIGHT.ordinal,
            DimensionType.FILL_PARENT_MAX_HEIGHT,
        )

        assertEquals(BoxLayout.START, LayoutPositioning.START)
        assertEquals(BoxLayout.CENTER, LayoutPositioning.CENTER)
        assertEquals(BoxLayout.END, LayoutPositioning.END)
        assertEquals(BoxLayout.TOP, LayoutPositioning.TOP)
        assertEquals(BoxLayout.BOTTOM, LayoutPositioning.BOTTOM)

        assertEquals(ClipPath.INTERSECT, ClipRegionOp.INTERSECT)
        assertEquals(ClipPath.DIFFERENCE, ClipRegionOp.DIFFERENCE)
        val packedClip = ClipRegionOp.pack(pathId = 123, regionOp = ClipRegionOp.DIFFERENCE)
        assertEquals(123, packedClip and 0x00FFFFFF)
        assertEquals(ClipRegionOp.DIFFERENCE, packedClip ushr 24)

        val packedPath = PathWinding.pack(pathId = 456, winding = PathWinding.EVEN_ODD)
        assertEquals(456, packedPath and 0x00FFFFFF)
        assertEquals(PathWinding.EVEN_ODD, packedPath ushr 24)

        assertEquals(NamedVariable.STRING_TYPE, NamedVariableType.STRING_TYPE)
        assertEquals(NamedVariable.FLOAT_TYPE, NamedVariableType.FLOAT_TYPE)
        assertEquals(NamedVariable.COLOR_TYPE, NamedVariableType.COLOR_TYPE)
        assertEquals(NamedVariable.IMAGE_TYPE, NamedVariableType.IMAGE_TYPE)
        assertEquals(NamedVariable.INT_TYPE, NamedVariableType.INT_TYPE)
        assertEquals(NamedVariable.LONG_TYPE, NamedVariableType.LONG_TYPE)

        assertEquals(HostNamedActionOperation.NONE_TYPE, HostNamedActionType.NONE_TYPE)
        assertEquals(HostNamedActionOperation.FLOAT_TYPE, HostNamedActionType.FLOAT_TYPE)
        assertEquals(HostNamedActionOperation.INT_TYPE, HostNamedActionType.INT_TYPE)
        assertEquals(HostNamedActionOperation.STRING_TYPE, HostNamedActionType.STRING_TYPE)
        assertEquals(
            HostNamedActionOperation.FLOAT_ARRAY_TYPE,
            HostNamedActionType.FLOAT_ARRAY_TYPE,
        )

        assertEquals(TextFromFloat.PAD_AFTER_SPACE, TextFromFloatFlags.PAD_AFTER_SPACE)
        assertEquals(TextFromFloat.PAD_AFTER_NONE, TextFromFloatFlags.PAD_AFTER_NONE)
        assertEquals(TextFromFloat.PAD_AFTER_ZERO, TextFromFloatFlags.PAD_AFTER_ZERO)
        assertEquals(TextFromFloat.PAD_PRE_SPACE, TextFromFloatFlags.PAD_PRE_SPACE)
        assertEquals(TextFromFloat.PAD_PRE_NONE, TextFromFloatFlags.PAD_PRE_NONE)
        assertEquals(TextFromFloat.PAD_PRE_ZERO, TextFromFloatFlags.PAD_PRE_ZERO)
        assertEquals(TextFromFloat.GROUPING_BY3, TextFromFloatFlags.GROUPING_BY3)
        assertEquals(TextFromFloat.GROUPING_BY4, TextFromFloatFlags.GROUPING_BY4)
        assertEquals(TextFromFloat.GROUPING_BY32, TextFromFloatFlags.GROUPING_BY32)
        assertEquals(
            TextFromFloat.SEPARATOR_PERIOD_COMMA,
            TextFromFloatFlags.SEPARATOR_PERIOD_COMMA,
        )
        assertEquals(
            TextFromFloat.SEPARATOR_COMMA_PERIOD,
            TextFromFloatFlags.SEPARATOR_COMMA_PERIOD,
        )
        assertEquals(TextFromFloat.SEPARATOR_SPACE_COMMA, TextFromFloatFlags.SEPARATOR_SPACE_COMMA)
        assertEquals(
            TextFromFloat.SEPARATOR_UNDER_PERIOD,
            TextFromFloatFlags.SEPARATOR_UNDER_PERIOD,
        )
        assertEquals(TextFromFloat.OPTIONS_ROUNDING, TextFromFloatFlags.OPTIONS_ROUNDING)
        assertEquals(
            TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES,
            TextFromFloatFlags.OPTIONS_NEGATIVE_PARENTHESES,
        )
        assertEquals(TextFromFloat.LEGACY_MODE, TextFromFloatFlags.LEGACY_MODE)
        assertEquals(TextFromFloat.FULL_FORMAT, TextFromFloatFlags.FULL_FORMAT)
        assertEquals(DrawTextAnchored.ANCHOR_TEXT_RTL, DrawTextAnchoredFlags.ANCHOR_TEXT_RTL)
        assertEquals(DrawTextAnchored.BASELINE_RELATIVE, DrawTextAnchoredFlags.BASELINE_RELATIVE)
        assertEquals(TextLayout.TEXT_ALIGN_CENTER, TextLayoutFlags.TEXT_ALIGN_CENTER)
        assertEquals(TextLayout.OVERFLOW_ELLIPSIS, TextLayoutFlags.OVERFLOW_ELLIPSIS)
        assertEquals(ImageScaling.SCALE_FIT, ImageScaleType.SCALE_FIT)
        assertEquals(ImageScaling.SCALE_CROP, ImageScaleType.SCALE_CROP)
        assertEquals(RootContentBehavior.ALIGNMENT_CENTER, RootContentFlags.ALIGNMENT_CENTER)
        assertEquals(RootContentBehavior.LAYOUT_MATCH_PARENT, RootContentFlags.LAYOUT_MATCH_PARENT)
        assertEquals(
            AccessibleComponent.Role.BUTTON.ordinal.toByte(),
            SemanticsConstants.ROLE_BUTTON,
        )
        assertEquals(AccessibleComponent.Mode.MERGE.ordinal.toByte(), SemanticsConstants.MODE_MERGE)
        assertEquals(Skip.SKIP_IF_API_LESS_THAN.toInt(), SkipCondition.SKIP_IF_API_LESS_THAN)
        assertEquals(ConditionalOperations.TYPE_EQ, ConditionType.TYPE_EQ)

        assertEquals(BitmapData.TYPE_PNG_8888.toInt(), BitmapFormat.TYPE_PNG_8888)
        assertEquals(BitmapData.TYPE_PNG.toInt(), BitmapFormat.TYPE_PNG)
        assertEquals(BitmapData.TYPE_RAW8.toInt(), BitmapFormat.TYPE_RAW8)
        assertEquals(BitmapData.TYPE_RAW8888.toInt(), BitmapFormat.TYPE_RAW8888)
        assertEquals(BitmapData.TYPE_PNG_ALPHA_8.toInt(), BitmapFormat.TYPE_PNG_ALPHA_8)
        assertEquals(BitmapData.ENCODING_INLINE.toInt(), BitmapFormat.ENCODING_INLINE)
        assertEquals(BitmapData.ENCODING_URL.toInt(), BitmapFormat.ENCODING_URL)
        assertEquals(BitmapData.ENCODING_FILE.toInt(), BitmapFormat.ENCODING_FILE)
        assertEquals(BitmapData.ENCODING_EMPTY.toInt(), BitmapFormat.ENCODING_EMPTY)
        assertEquals(
            BitmapData.ENCODING_COMPONENT_OFFSCREEN_BUFFER.toInt(),
            BitmapFormat.ENCODING_COMPONENT_OFFSCREEN_BUFFER,
        )
        assertEquals(4L, BitmapFormat.bytesPerPixel(BitmapFormat.TYPE_RAW8))
        assertEquals(1L, BitmapFormat.bytesPerPixel(BitmapFormat.TYPE_PNG_ALPHA_8))
        assertEquals(4L, BitmapFormat.bytesPerPixel(BitmapFormat.TYPE_RAW8888))
        assertEquals(4L, BitmapFormat.bytesPerPixel(BitmapFormat.TYPE_PNG))

        assertEquals(PathData.MOVE, PathCommandTags.MOVE)
        assertEquals(PathData.LINE, PathCommandTags.LINE)
        assertEquals(PathData.QUADRATIC, PathCommandTags.QUADRATIC)
        assertEquals(PathData.CONIC, PathCommandTags.CONIC)
        assertEquals(PathData.CUBIC, PathCommandTags.CUBIC)
        assertEquals(PathData.CLOSE, PathCommandTags.CLOSE)
        assertEquals(PathData.DONE, PathCommandTags.DONE)
        assertEquals(PathAppend.RESET, PathCommandTags.RESET)
        assertEquals(PathAppend.RESET_NAN.toRawBits(), PathCommandTags.RESET_NAN.toRawBits())
    }

    @Test
    fun testPackAnimationSpecBitForBitParity() {
        assertEquals(Easing.CUBIC_STANDARD, AnimationEasing.CUBIC_STANDARD)
        assertEquals(Easing.CUBIC_CUSTOM, AnimationEasing.CUBIC_CUSTOM)
        assertEquals(Easing.SPLINE_CUSTOM, AnimationEasing.SPLINE_CUSTOM)
        assertEquals(Easing.EASE_OUT_BOUNCE, AnimationEasing.BOUNCE)
        assertEquals(Easing.EASE_OUT_ELASTIC, AnimationEasing.ELASTIC)

        val cases =
            listOf(
                packAnimationSpec() to
                    RemoteComposeBuffer.packAnimation(
                        1f,
                        Easing.CUBIC_STANDARD,
                        null,
                        Float.NaN,
                        Float.NaN,
                    ),
                packAnimationSpec(duration = 0.35f, type = AnimationEasing.CUBIC_DECELERATE) to
                    RemoteComposeBuffer.packAnimation(
                        0.35f,
                        Easing.CUBIC_DECELERATE,
                        null,
                        Float.NaN,
                        Float.NaN,
                    ),
                packAnimationSpec(
                    duration = 0.5f,
                    type = AnimationEasing.CUBIC_CUSTOM,
                    spec = floatArrayOf(0.2f, 0f, 0f, 1f),
                ) to
                    RemoteComposeBuffer.packAnimation(
                        0.5f,
                        Easing.CUBIC_CUSTOM,
                        floatArrayOf(0.2f, 0f, 0f, 1f),
                        Float.NaN,
                        Float.NaN,
                    ),
                packAnimationSpec(
                    duration = 1.25f,
                    type = AnimationEasing.ELASTIC,
                    spec = floatArrayOf(1.5f, 0.4f),
                    initialValue = 0f,
                    wrap = 360f,
                ) to
                    RemoteComposeBuffer.packAnimation(
                        1.25f,
                        Easing.EASE_OUT_ELASTIC,
                        floatArrayOf(1.5f, 0.4f),
                        0f,
                        360f,
                    ),
            )

        for ((index, pair) in cases.withIndex()) {
            assertRawFloatArrayEquals(
                expected = pair.second,
                actual = pair.first,
                message = "packAnimationSpec mismatch on case $index",
            )
        }
    }

    @Test
    fun testPaintConstantsParityWithPaintBundle() {
        assertEquals(PaintBundle.STYLE_FILL, PaintConstants.STYLE_FILL)
        assertEquals(PaintBundle.STYLE_STROKE, PaintConstants.STYLE_STROKE)
        assertEquals(PaintBundle.STYLE_FILL_AND_STROKE, PaintConstants.STYLE_FILL_AND_STROKE)
        assertEquals(PaintBundle.FONT_TYPE_DEFAULT, PaintConstants.FONT_TYPE_DEFAULT)
        assertEquals(PaintBundle.FONT_TYPE_SANS_SERIF, PaintConstants.FONT_TYPE_SANS_SERIF)
        assertEquals(PaintBundle.FONT_TYPE_SERIF, PaintConstants.FONT_TYPE_SERIF)
        assertEquals(PaintBundle.FONT_TYPE_MONOSPACE, PaintConstants.FONT_TYPE_MONOSPACE)
        assertEquals(PaintBundle.BLEND_MODE_SRC_OVER, PaintConstants.BLEND_MODE_SRC_OVER)
        assertEquals(PaintBundle.BLEND_MODE_MULTIPLY, PaintConstants.BLEND_MODE_MULTIPLY)
        assertEquals(PaintBundle.BLEND_MODE_NULL, PaintConstants.BLEND_MODE_NULL)
        assertEquals(PaintBundle.PORTER_MODE_ADD, PaintConstants.PORTER_MODE_ADD)
        for (code in -1..35) {
            assertEquals(PaintBundle.blendModeString(code), PaintConstants.blendModeName(code))
        }
        assertEquals("CLAMP", PaintConstants.tileModeName(PaintConstants.TILE_CLAMP))
        assertEquals("REPEAT", PaintConstants.tileModeName(PaintConstants.TILE_REPEAT))
        assertEquals("MIRROR", PaintConstants.tileModeName(PaintConstants.TILE_MIRROR))
        assertEquals("DECAL", PaintConstants.tileModeName(PaintConstants.TILE_DECAL))
        assertEquals("UNKNOWN", PaintConstants.tileModeName(99))
        assertTrue(PaintConstants.isSrcOverCompatibleBlendMode(PaintConstants.BLEND_MODE_SRC_OVER))
        assertTrue(PaintConstants.isSrcOverCompatibleBlendMode(PaintConstants.BLEND_MODE_NULL))
        assertFalse(PaintConstants.isSrcOverCompatibleBlendMode(PaintConstants.BLEND_MODE_MULTIPLY))
    }

    @Test
    fun testRemoteContextIdsIsTimeParityWithRemoteContext() {
        for (id in 1..50) {
            val nanFloat = Utils.asNan(id)
            assertEquals(
                RemoteContext.isTime(nanFloat),
                RemoteContextIds.isTime(id),
                "isTime(Int) mismatch for id=$id",
            )
            assertEquals(
                RemoteContext.isTime(nanFloat),
                RemoteContextIds.isTime(MaybeVariableFloat.variable(id)),
                "isTime(MaybeVariableFloat) mismatch for id=$id",
            )
        }
        assertFalse(RemoteContextIds.isTime(0))
        assertFalse(RemoteContextIds.isTime(-1))
        assertFalse(RemoteContextIds.isTime(MaybeVariableFloat.literal(1f)))
        assertFalse(RemoteContextIds.isTime(MaybeVariableFloat.literal(Float.NaN)))
        assertFalse(RemoteContextIds.isTime(MaybeVariableFloat.literal(Float.fromBits(0x3F800001))))
    }

    @Test
    fun testNanEncodingIgnoresSignBit() {
        assertEquals(0x7F800000, NanEncoding.NAN_EXPONENT)
        assertEquals(-1, NanEncoding.NO_ID)
        assertTrue(NanEncoding.isNan(NanEncoding.asNan(42)))
        assertEquals(42, NanEncoding.idFromNan(NanEncoding.asNan(42)))
        assertTrue(NanEncoding.isNan(Float.fromBits(0x7F800005)))
        assertEquals(5, NanEncoding.idFromNan(Float.fromBits(0x7F800005)))
        assertTrue(NanEncoding.isNan(Float.fromBits(0x7FC00001)))
        assertEquals(1, NanEncoding.idFromNan(Float.fromBits(0x7FC00001)))
        assertFalse(NanEncoding.isNan(Float.NaN))
        assertEquals(NanEncoding.NO_ID, NanEncoding.idFromNan(Float.NaN))
        assertFalse(NanEncoding.isNan(Float.fromBits(0xFFC00000.toInt())))
        assertEquals(NanEncoding.NO_ID, NanEncoding.idFromNan(Float.fromBits(0xFFC00000.toInt())))
        assertFalse(NanEncoding.isNan(Float.POSITIVE_INFINITY))
        assertFalse(NanEncoding.isNan(Float.NEGATIVE_INFINITY))

        val posAddToken =
            Float.fromBits((FloatRpn.OFFSET + FloatRpn.ID_ADD) or NanEncoding.NAN_EXPONENT)
        assertTrue(FloatRpn.isOperator(posAddToken))
        assertEquals(FloatRpn.ID_ADD, FloatRpn.operatorId(posAddToken))
        assertFalse(FloatRpn.isOperator(Float.NaN))
        assertEquals(NanEncoding.NO_ID, FloatRpn.operatorId(Float.NaN))
    }

    @Test
    fun testMaybeDereferencedIdAndNanEncodingHelpers() {
        assertEquals(PaintOperation.PTR_DEREFERENCE, MaybeDereferencedId.PTR_DEREFERENCE)
        assertEquals(PaintOperation.VALUE_MASK, MaybeDereferencedId.VALUE_MASK)

        val direct = MaybeDereferencedId.direct(42)
        assertFalse(direct.isDereferenced)
        assertEquals(42, direct.directId)
        assertNull(direct.pointerId)
        assertEquals(42, direct.unmaskedId)

        val indirect = MaybeDereferencedId.dereferenced(42)
        assertTrue(indirect.isDereferenced)
        assertNull(indirect.directId)
        assertEquals(42, indirect.pointerId)
        assertEquals(42, indirect.unmaskedId)

        // High bits above 16-bit VALUE_MASK (including sign bit 31 and bits 16..29) are masked out
        val highBitsRaw = MaybeDereferencedId.fromRawInt(0x8003_002A.toInt())
        assertFalse(highBitsRaw.isDereferenced)
        assertEquals(42, highBitsRaw.unmaskedId)
        assertEquals(42, highBitsRaw.directId)
        assertNull(highBitsRaw.pointerId)
        val highBitsPtr = MaybeDereferencedId.fromRawInt(0xC003_002A.toInt())
        assertTrue(highBitsPtr.isDereferenced)
        assertEquals(42, highBitsPtr.unmaskedId)
        assertNull(highBitsPtr.directId)
        assertEquals(42, highBitsPtr.pointerId)
        assertEquals(42, MaybeDereferencedId.direct(0x1234_002A).raw)
        assertEquals(
            42 or PaintOperation.PTR_DEREFERENCE,
            MaybeDereferencedId.dereferenced(0x1234_002A).raw,
        )

        for (id in -5..60) {
            assertEquals(Utils.isSystemGlobal(id), NanEncoding.isSystemGlobalId(id))
        }
        for (id in intArrayOf(0x3FFF, 0x4000, 0x4FFF, 0x5000)) {
            assertEquals(Utils.isMacroLocal(id), NanEncoding.isMacroLocalId(id))
        }

        assertEquals(-1, NanEncoding.idRegion(-1))
        assertEquals(-1, NanEncoding.idRegion(Int.MIN_VALUE))
        assertEquals(NanEncoding.REGION_SYSTEM, NanEncoding.idRegion(42))
        assertEquals(
            NanEncoding.REGION_VARIABLE,
            NanEncoding.idRegion(NanEncoding.REGION_VARIABLE + 1),
        )
        assertEquals(NanEncoding.REGION_ARRAY, NanEncoding.idRegion(NanEncoding.START_ARRAY_ID))
        assertEquals(
            NanEncoding.REGION_OPERATION,
            NanEncoding.idRegion(NanEncoding.REGION_OPERATION + 5),
        )

        assertTrue(NanEncoding.isNonIdFloatNan(Float.NaN))
        assertFalse(NanEncoding.isNonIdFloatNan(Float.fromBits(0x7F800005)))
        assertFalse(NanEncoding.isNonIdFloatNan(Float.fromBits(0x7FC00001)))
        assertTrue(NanEncoding.isNonIdFloatNan(Float.fromBits(0xFFC00000.toInt())))
        assertFalse(NanEncoding.isNonIdFloatNan(NanEncoding.asNan(42)))
        assertFalse(NanEncoding.isNonIdFloatNan(Float.POSITIVE_INFINITY))
        assertFalse(NanEncoding.isNonIdFloatNan(Float.NEGATIVE_INFINITY))
        assertFalse(NanEncoding.isNonIdFloatNan(1.5f))

        assertEquals(42, NanEncoding.decodeIdOrNull(NanEncoding.asNan(42)))
        assertNull(NanEncoding.decodeIdOrNull(Float.NaN))
        assertNull(NanEncoding.decodeIdOrNull(1.5f))

        assertEquals(0, NanEncoding.decodeLongIdOrNull(NanEncoding.LONG_ID_OFFSET))
        assertEquals(42, NanEncoding.decodeLongIdOrNull(NanEncoding.encodeLongId(42)))
        assertEquals(
            Int.MAX_VALUE,
            NanEncoding.decodeLongIdOrNull(NanEncoding.LONG_ID_OFFSET + Int.MAX_VALUE),
        )
        assertNull(NanEncoding.decodeLongIdOrNull(NanEncoding.LONG_ID_OFFSET - 1L))
        assertNull(NanEncoding.decodeLongIdOrNull(NanEncoding.LONG_ID_OFFSET + Int.MAX_VALUE + 1L))
        assertNull(NanEncoding.decodeLongIdOrNull(42L))

        assertTrue(MaybeVariableFloat.variable(NanEncoding.START_ARRAY_ID).isArrayVariable)
        assertFalse(MaybeVariableFloat.variable(42).isArrayVariable)
        assertFalse(1.5f.mvf.isArrayVariable)
    }

    @Test
    fun testUnpackAnimationSpecAndCubicControlPointsParity() {
        val packed =
            packAnimationSpec(
                duration = 0.75f,
                type = AnimationEasing.CUBIC_CUSTOM,
                spec = floatArrayOf(0.2f, 0.1f, 0.8f, 0.9f),
                initialValue = 10f,
                wrap = 360f,
            )
        val unpacked = unpackAnimationSpec(packed)
        assertEquals(0.75f, unpacked.duration)
        assertEquals(AnimationEasing.CUBIC_CUSTOM, unpacked.type)
        assertRawFloatArrayEquals(floatArrayOf(0.2f, 0.1f, 0.8f, 0.9f), unpacked.params)
        assertEquals(10f, unpacked.initialValue)
        assertEquals(360f, unpacked.wrap)
        assertEquals(0, unpacked.directionalSnap)
        assertFalse(unpacked.isPropagate)

        // Verify directionalSnap + propagate bit extraction matches FloatAnimation
        val flagsWord = AnimationEasing.CUBIC_STANDARD or (2 shl 10) or (1 shl 12)
        val rawWithFlags = floatArrayOf(0.5f, Float.fromBits(flagsWord))
        val unpackedFlags = unpackAnimationSpec(rawWithFlags)
        assertEquals(2, unpackedFlags.directionalSnap)
        assertTrue(unpackedFlags.isPropagate)
        val coreString = FloatAnimation.unpackAnimationToString(rawWithFlags)
        assertTrue(coreString.contains("directionalSnap=2"))
        assertTrue(coreString.contains("propagate"))

        // Preset cubic control points match CubicEasing
        val expectedPresets =
            mapOf(
                AnimationEasing.CUBIC_STANDARD to floatArrayOf(0.4f, 0.0f, 0.2f, 1.0f),
                AnimationEasing.CUBIC_ACCELERATE to floatArrayOf(0.4f, 0.05f, 0.8f, 0.7f),
                AnimationEasing.CUBIC_DECELERATE to floatArrayOf(0.0f, 0.0f, 0.2f, 0.95f),
                AnimationEasing.CUBIC_LINEAR to floatArrayOf(1.0f, 1.0f, 0.0f, 0.0f),
                AnimationEasing.CUBIC_ANTICIPATE to floatArrayOf(0.36f, 0.0f, 0.66f, -0.56f),
                AnimationEasing.CUBIC_OVERSHOOT to floatArrayOf(0.34f, 1.56f, 0.64f, 1.0f),
            )
        for ((preset, expected) in expectedPresets) {
            val pts = AnimationEasing.cubicControlPoints(preset)
            assertNotNull(pts)
            assertRawFloatArrayEquals(expected, pts)
            val corePreset = CubicEasing().apply { config(preset) }
            val coreFromPts = CubicEasing().apply { setup(pts[0], pts[1], pts[2], pts[3]) }
            assertEquals(corePreset.get(0.35f), coreFromPts.get(0.35f), 1e-5f)
        }
        assertNull(AnimationEasing.cubicControlPoints(AnimationEasing.CUBIC_CUSTOM))
        assertNull(AnimationEasing.cubicControlPoints(AnimationEasing.SPLINE_CUSTOM))
        assertNull(AnimationEasing.cubicControlPoints(AnimationEasing.EASE_OUT_BOUNCE))
        assertNull(AnimationEasing.cubicControlPoints(AnimationEasing.EASE_OUT_ELASTIC))
        assertNull(AnimationEasing.cubicControlPoints(99))

        // Spring spec detection and rejection by unpackAnimationSpec
        val springSpec = floatArrayOf(0f, 1f, 2f, 3f, 4f)
        assertTrue(isSpringAnimationSpec(springSpec))
        assertFalse(isSpringAnimationSpec(floatArrayOf(0f, 1f, 2f, 3f)))
        assertFalse(isSpringAnimationSpec(floatArrayOf(0.5f, 1f, 2f, 3f, 4f)))
        assertFailsWith<IllegalArgumentException> { unpackAnimationSpec(springSpec) }

        // Empty, 1-element, hasInit-only, hasWrap-only specs
        assertEquals(FloatAnimationSpec(), unpackAnimationSpec(floatArrayOf()))
        assertEquals(
            FloatAnimationSpec(duration = 2.5f),
            unpackAnimationSpec(floatArrayOf(2.5f)),
        )
        val initOnly = unpackAnimationSpec(packAnimationSpec(duration = 1f, initialValue = 42f))
        assertEquals(42f, initOnly.initialValue)
        assertTrue(initOnly.wrap.isNaN())
        val wrapOnly = unpackAnimationSpec(packAnimationSpec(duration = 1f, wrap = 180f))
        assertTrue(wrapOnly.initialValue.isNaN())
        assertEquals(180f, wrapOnly.wrap)

        // Defensive copying & equals/hashCode
        val srcParams = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val a =
            FloatAnimationSpec(
                duration = 1f,
                type = AnimationEasing.CUBIC_CUSTOM,
                params = srcParams,
            )
        srcParams[0] = 99f
        assertEquals(0.1f, a.params[0])
        val exposed = a.params
        exposed[0] = 88f
        assertEquals(0.1f, a.params[0])
        val b =
            FloatAnimationSpec(
                duration = 1f,
                type = AnimationEasing.CUBIC_CUSTOM,
                params = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f),
            )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a != FloatAnimationSpec(duration = 2f))

        // Truncated params, initialValue, and wrap throw IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            unpackAnimationSpec(
                floatArrayOf(1f, Float.fromBits((4 shl 16) or AnimationEasing.CUBIC_CUSTOM))
            )
        }
        assertFailsWith<IllegalArgumentException> {
            unpackAnimationSpec(
                floatArrayOf(1f, Float.fromBits((2 shl 8) or AnimationEasing.CUBIC_STANDARD))
            )
        }
        assertFailsWith<IllegalArgumentException> {
            unpackAnimationSpec(
                floatArrayOf(1f, Float.fromBits((1 shl 8) or AnimationEasing.CUBIC_STANDARD))
            )
        }
    }

    @Test
    fun testRemoteColorMathBitForBitParityWithUtils() {
        assertEquals(
            ColorExpression.COLOR_COLOR_INTERPOLATE.toInt(),
            ColorExpressionModes.COLOR_COLOR_INTERPOLATE,
        )
        assertEquals(
            ColorExpression.ID_COLOR_INTERPOLATE.toInt(),
            ColorExpressionModes.ID_COLOR_INTERPOLATE,
        )
        assertEquals(
            ColorExpression.COLOR_ID_INTERPOLATE.toInt(),
            ColorExpressionModes.COLOR_ID_INTERPOLATE,
        )
        assertEquals(
            ColorExpression.ID_ID_INTERPOLATE.toInt(),
            ColorExpressionModes.ID_ID_INTERPOLATE,
        )
        assertEquals(ColorExpression.HSV_MODE.toInt(), ColorExpressionModes.HSV_MODE)
        assertEquals(ColorExpression.ARGB_MODE.toInt(), ColorExpressionModes.ARGB_MODE)
        assertEquals(ColorExpression.IDARGB_MODE.toInt(), ColorExpressionModes.ID_ARGB_MODE)

        val hsvFlags = (0x80 shl 16) or ColorExpressionModes.HSV_MODE
        assertEquals(ColorExpressionModes.HSV_MODE, ColorExpressionModes.mode(hsvFlags))
        assertEquals(0x80, ColorExpressionModes.alphaPayload(hsvFlags))

        val sampleColors =
            intArrayOf(
                0xFF000000.toInt(),
                0xFFFFFFFF.toInt(),
                0xFFFF0000.toInt(),
                0xFFFF0080.toInt(), // red-max with green < blue exercises negative hue wrap
                0xFF00FF00.toInt(),
                0xFF0000FF.toInt(),
                0x80336699.toInt(),
                0xCCABCDEF.toInt(),
            )
        val tValues = floatArrayOf(-0.25f, -0f, 0f, 0.25f, 0.5f, 0.75f, 1f, 1.25f, Float.NaN)
        for (c1 in sampleColors) {
            assertEquals(Utils.getHue(c1), RemoteColorMath.hue(c1))
            assertEquals(Utils.getSaturation(c1), RemoteColorMath.saturation(c1))
            assertEquals(Utils.getBrightness(c1), RemoteColorMath.brightness(c1))
            assertEquals(
                c1,
                RemoteColorMath.argb(
                    RemoteColorMath.alpha(c1),
                    RemoteColorMath.red(c1),
                    RemoteColorMath.green(c1),
                    RemoteColorMath.blue(c1),
                ),
            )
            for (c2 in sampleColors) {
                for (t in tValues) {
                    assertEquals(
                        Utils.interpolateColor(c1, c2, t),
                        RemoteColorMath.interpolateColor(c1, c2, t),
                        "interpolateColor mismatch for c1=$c1 c2=$c2 t=$t",
                    )
                }
            }
        }
        assertEquals(0, RemoteColorMath.clampChannel(-10))
        assertEquals(0, RemoteColorMath.clampChannel(0))
        assertEquals(128, RemoteColorMath.clampChannel(128))
        assertEquals(255, RemoteColorMath.clampChannel(300))

        val hues = floatArrayOf(-0.2f, 0f, 0.1f, 0.25f, 0.45f, 0.6f, 0.8f, 0.95f, 1f)
        for (h in hues) {
            assertEquals(Utils.hsvToRgb(h, 0.8f, 0.9f), RemoteColorMath.hsvToRgb(h, 0.8f, 0.9f))
        }
        assertEquals(Utils.hsvToRgb(0.5f, 0f, 0.75f), RemoteColorMath.hsvToRgb(0.5f, 0f, 0.75f))
        assertEquals(Utils.hsvToRgb(0.5f, 0.8f, 0f), RemoteColorMath.hsvToRgb(0.5f, 0.8f, 0f))
        assertEquals(0, RemoteColorMath.hsvToRgb(1f, 0.8f, 0.9f))
        assertEquals(0, RemoteColorMath.hsvToRgb(-0.2f, 0.8f, 0.9f))
        assertEquals(
            Utils.toARGB(0.5f, 0.25f, 0.75f, 1.0f),
            RemoteColorMath.toArgb(0.5f, 0.25f, 0.75f, 1.0f),
        )
    }

    @Test
    fun testTextFromFloatFormatterBitForBitParityWithStringUtils() {
        val values =
            floatArrayOf(
                0f,
                -0f,
                1e-4f,
                12.345f,
                -987654.3f,
                0.999f,
                1234567.8f,
                -0.05f,
                42f,
                Float.NaN,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
            )
        val flagCombos =
            intArrayOf(
                TextFromFloatFlags.PAD_PRE_NONE or TextFromFloatFlags.PAD_AFTER_ZERO,
                TextFromFloatFlags.PAD_PRE_ZERO or TextFromFloatFlags.PAD_AFTER_ZERO,
                TextFromFloatFlags.PAD_PRE_SPACE or TextFromFloatFlags.PAD_AFTER_SPACE,
                TextFromFloatFlags.PAD_PRE_NONE or
                    TextFromFloatFlags.PAD_AFTER_NONE or
                    TextFromFloatFlags.GROUPING_BY3 or
                    TextFromFloatFlags.SEPARATOR_COMMA_PERIOD,
                TextFromFloatFlags.PAD_PRE_NONE or
                    TextFromFloatFlags.PAD_AFTER_ZERO or
                    TextFromFloatFlags.GROUPING_BY4 or
                    TextFromFloatFlags.SEPARATOR_PERIOD_COMMA or
                    TextFromFloatFlags.OPTIONS_NEGATIVE_PARENTHESES,
                TextFromFloatFlags.PAD_PRE_NONE or
                    TextFromFloatFlags.PAD_AFTER_ZERO or
                    TextFromFloatFlags.GROUPING_BY32 or
                    TextFromFloatFlags.SEPARATOR_SPACE_COMMA or
                    TextFromFloatFlags.OPTIONS_ROUNDING,
                TextFromFloatFlags.PAD_PRE_ZERO or
                    TextFromFloatFlags.PAD_AFTER_ZERO or
                    TextFromFloatFlags.GROUPING_BY3 or
                    TextFromFloatFlags.SEPARATOR_UNDER_PERIOD or
                    TextFromFloatFlags.OPTIONS_ROUNDING or
                    TextFromFloatFlags.OPTIONS_NEGATIVE_PARENTHESES,
                TextFromFloatFlags.PAD_PRE_ZERO or
                    TextFromFloatFlags.PAD_AFTER_ZERO or
                    TextFromFloatFlags.LEGACY_MODE,
                TextFromFloatFlags.PAD_PRE_NONE or
                    TextFromFloatFlags.PAD_AFTER_NONE or
                    TextFromFloatFlags.LEGACY_MODE,
                TextFromFloatFlags.PAD_PRE_SPACE or
                    TextFromFloatFlags.PAD_AFTER_SPACE or
                    TextFromFloatFlags.LEGACY_MODE,
                TextFromFloatFlags.FULL_FORMAT,
            )
        val digitPairs = listOf(8 to 2, 8 to 0, 0 to 2, 8 to 4)

        for ((before, after) in digitPairs) {
            for (flags in flagCombos) {
                // Decode flags independently using remote-core's TextFromFloat constants so the
                // oracle does not depend on TextFromFloatFormatter.parseSpec.
                val oraclePadAfter =
                    when (flags and 3) {
                        TextFromFloat.PAD_AFTER_SPACE -> ' '
                        TextFromFloat.PAD_AFTER_NONE -> '\u0000'
                        TextFromFloat.PAD_AFTER_ZERO -> '0'
                        else -> ' '
                    }
                val oraclePadBefore =
                    when (flags and (3 shl 2)) {
                        TextFromFloat.PAD_PRE_SPACE -> ' '
                        TextFromFloat.PAD_PRE_NONE -> '\u0000'
                        TextFromFloat.PAD_PRE_ZERO -> '0'
                        else -> ' '
                    }
                val oracleGrouping =
                    when (flags and (3 shl 4)) {
                        TextFromFloat.GROUPING_BY3 -> StringUtils.GROUPING_BY3
                        TextFromFloat.GROUPING_BY4 -> StringUtils.GROUPING_BY4
                        TextFromFloat.GROUPING_BY32 -> StringUtils.GROUPING_BY32
                        else -> StringUtils.GROUPING_NONE
                    }
                val oracleSeparator =
                    when (flags and (3 shl 6)) {
                        TextFromFloat.SEPARATOR_PERIOD_COMMA -> StringUtils.SEPARATOR_PERIOD_COMMA
                        TextFromFloat.SEPARATOR_COMMA_PERIOD -> StringUtils.SEPARATOR_COMMA_PERIOD
                        TextFromFloat.SEPARATOR_SPACE_COMMA -> StringUtils.SEPARATOR_SPACE_COMMA
                        else -> StringUtils.SEPARATOR_UNDER_PERIOD
                    }
                for (v in values) {
                    val isFull = (flags and TextFromFloat.FULL_FORMAT) != 0
                    val expected =
                        when {
                            isFull -> v.toString()
                            (flags and TextFromFloat.LEGACY_MODE) != 0 ->
                                StringUtils.floatToString(
                                    v,
                                    before,
                                    after,
                                    oraclePadBefore,
                                    oraclePadAfter,
                                )
                            else -> {
                                var opts = 0
                                if ((flags and TextFromFloat.OPTIONS_ROUNDING) != 0) {
                                    opts = opts or StringUtils.ROUNDING
                                }
                                if ((flags and TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES) != 0) {
                                    opts = opts or StringUtils.NEGATIVE_PARENTHESES
                                }
                                StringUtils.floatToString(
                                    v,
                                    before,
                                    after,
                                    oraclePadBefore,
                                    oraclePadAfter,
                                    oracleSeparator,
                                    oracleGrouping,
                                    opts,
                                )
                            }
                        }
                    assertEquals(
                        expected,
                        TextFromFloatFormatter.format(
                            value = v,
                            digitsBefore = before,
                            digitsAfter = after,
                            flags = flags,
                        ),
                        "Formatting mismatch for v=$v before=$before after=$after flags=0x${flags.toString(16)}",
                    )
                }
            }
        }

        // Bounds validation rejects negative and excessively large digit counts
        assertFailsWith<IllegalArgumentException> {
            TextFromFloatFormatter.parseSpec(-1, 2, TextFromFloatFlags.PAD_PRE_NONE)
        }
        assertFailsWith<IllegalArgumentException> {
            TextFromFloatFormatter.parseSpec(2, -1, TextFromFloatFlags.PAD_PRE_NONE)
        }
        assertFailsWith<IllegalArgumentException> {
            TextFromFloatFormatter.parseSpec(
                TextFromFloatFormatter.MAX_DIGITS + 1,
                2,
                TextFromFloatFlags.PAD_PRE_NONE,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TextFromFloatFormatter.format(
                1.5f,
                2,
                TextFromFloatFormatter.MAX_DIGITS + 1,
                TextFromFloatFlags.PAD_PRE_NONE,
            )
        }
    }

    @Test
    fun testProfileOperationRegistry() {
        val registry = ProfileOperationRegistry()
        val otherRegistry = ProfileOperationRegistry()
        val maskA = 1 shl 28
        val maskB = 1 shl 29
        val maskBoth = maskA or maskB
        val opcodeA = 254
        val opcodeB = 255
        val opcodeOverride = 253

        // register(0) rejected; unregister on absent mask is a no-op
        assertFailsWith<IllegalArgumentException> { registry.register(0) { emptySet() } }
        registry.unregister(maskA)

        // Built-in RcProfiles delegation (HEADER opcode 0 is supported in API 6)
        assertTrue(registry.isOpcodeSupported(0, apiLevel = 6, profileFlags = 0))

        assertFalse(registry.isOpcodeSupported(opcodeA, 7, maskA))
        registry.register(maskA) { setOf(opcodeA) }
        assertEquals(setOf(opcodeA), registry.resolveOperationIds(maskA))
        assertTrue(registry.isOpcodeSupported(opcodeA, 7, maskA))
        assertFalse(registry.isOpcodeSupported(opcodeA, 7, 0))

        // Separate instances remain isolated from each other
        assertFalse(otherRegistry.isOpcodeSupported(opcodeA, 7, maskA))
        assertTrue(otherRegistry.resolveOperationIds(maskA).isEmpty())

        // apiLevel <= 6 ignores custom profile providers
        assertFalse(registry.isOpcodeSupported(opcodeA, 6, maskA))

        // Multi-provider additive union & partial-bit non-match
        registry.register(maskBoth) { setOf(opcodeB) }
        assertEquals(setOf(opcodeA), registry.resolveOperationIds(maskA))
        assertFalse(registry.isOpcodeSupported(opcodeB, 7, maskA))
        assertFalse(registry.isOpcodeSupported(opcodeB, 7, maskB))
        assertEquals(setOf(opcodeA, opcodeB), registry.resolveOperationIds(maskBoth))
        assertTrue(registry.isOpcodeSupported(opcodeA, 7, maskBoth))
        assertTrue(registry.isOpcodeSupported(opcodeB, 7, maskBoth))

        // Re-registering same mask overwrites previous provider
        registry.register(maskA) { setOf(opcodeOverride) }
        assertEquals(setOf(opcodeOverride), registry.resolveOperationIds(maskA))
        assertFalse(registry.isOpcodeSupported(opcodeA, 7, maskA))
        assertTrue(registry.isOpcodeSupported(opcodeOverride, 7, maskA))

        registry.unregister(maskA)
        registry.unregister(maskBoth)
        assertTrue(registry.resolveOperationIds(maskBoth).isEmpty())
    }
}
