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

package androidx.compose.remote.integration.view.demos.examples

import android.annotation.SuppressLint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterInterface
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.ScrollModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun colorList(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            var len = system_accent1.size / 2
            len += system_accent2.size / 2
            len += system_accent3.size / 2
            len += system_error.size / 2
            len += system_neutral1.size / 2
            len += system_neutral2.size / 2
            len += nameList.size / 2

            val touchPosition: Float = addFloatConstant(0f)
            val computedHeight: Float = addFloatConstant(103f)
            val scrollSize: Float =
                floatExpression(computedHeight, len.toFloat(), AnimatedFloatExpression.MUL)
            val visFloat: Float = addFloatConstant(1f)
            val vis = Utils.idFromNan(visFloat)
            val notVisFloat: Float = addFloatConstant(0f)
            val notVis = Utils.idFromNan(notVisFloat)
            val scrollPosition: Float =
                floatExpression(
                    touchPosition,
                    computedHeight,
                    20f,
                    AnimatedFloatExpression.ADD,
                    AnimatedFloatExpression.MUL,
                )
            root {
                box(
                    RecordingModifier().background(0xFFAAAAAA.toInt()).fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    column(
                        RecordingModifier()
                            .fillMaxSize()
                            .then(
                                CustomScroller(
                                    0,
                                    ScrollModifier.VERTICAL,
                                    touchPosition,
                                    scrollPosition,
                                    len - 5,
                                    scrollSize,
                                )
                            )
                    ) {
                        makeColorTab(system_accent1)
                        makeColorTab(system_accent2)
                        makeColorTab(system_accent3)
                        makeColorTab(system_error)
                        makeColorTab(system_neutral1)
                        makeColorTab(system_neutral2)
                        makeColorTab(nameList)
                    }
                }
            }
        }
    return rc.writer
}

@Preview @Composable private fun ColorListPreview() = RemoteDocPreview(colorList())

@Suppress("RestrictedApiAndroidX")
fun colorTable(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            var len = system_accent1.size / 2
            len += system_accent2.size / 2
            len += system_accent3.size / 2
            len += system_error.size / 2
            len += system_neutral1.size / 2
            len += system_neutral2.size / 2
            len += nameList.size / 2

            val touchPosition: Float = addFloatConstant(0f)
            val computedHeight: Float = addFloatConstant(103f)
            val scrollSize: Float =
                floatExpression(computedHeight, len.toFloat(), AnimatedFloatExpression.MUL)
            val visFloat: Float = addFloatConstant(1f)
            val vis = Utils.idFromNan(visFloat)
            val notVisFloat: Float = addFloatConstant(0f)
            val notVis = Utils.idFromNan(notVisFloat)
            val scrollPosition: Float =
                floatExpression(
                    touchPosition,
                    computedHeight,
                    20f,
                    AnimatedFloatExpression.ADD,
                    AnimatedFloatExpression.MUL,
                )
            root {
                box(
                    RecordingModifier().background(0xFFAAAAAA.toInt()).fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    column(
                        RecordingModifier()
                            .fillMaxSize()
                            .then(
                                CustomScroller(
                                    0,
                                    ScrollModifier.VERTICAL,
                                    touchPosition,
                                    scrollPosition,
                                    len - 5,
                                    scrollSize,
                                )
                            )
                    ) {
                        makeColorRows(system_accent1)
                        makeColorRows(system_accent2)
                        makeColorRows(system_accent3)
                        makeColorRows(system_error)
                        makeColorRows(system_neutral1)
                        makeColorRows(system_neutral2)
                        makeColorRows(nameList)
                    }
                }
            }
        }
    return rc.writer
}

@Preview @Composable private fun ColorTablePreview() = RemoteDocPreview(colorTable())

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.makeColorTab(list: Array<String>) {
    val cSet = makeColorSet(list)
    box(RecordingModifier().background(0xFF000000.toInt()).fillMaxWidth().height(4)) {}
    var pad = 0
    for (i in 0 until cSet.size) {
        val c = cSet[i]
        row(
            RecordingModifier().padding(pad, 8, 4, 0).background(0xFF999999.toInt()).fillMaxWidth()
        ) {
            val dim = 48f
            box(RecordingModifier().padding(4).backgroundId(c.id).width(dim * 2).height(dim * 2)) {}
            column {
                row {
                    box(
                        RecordingModifier()
                            .padding(4)
                            .backgroundId(c.lightId)
                            .width(dim)
                            .height(dim - 8)
                    ) {}
                    text(c.lightName, RecordingModifier(), fontSize = dim)
                }
                row {
                    box(
                        RecordingModifier()
                            .padding(4)
                            .backgroundId(c.darkId)
                            .width(dim)
                            .height(dim - 8)
                    ) {}
                    text(c.darkName, RecordingModifier(), fontSize = 48f)
                }
            }
        }
        pad = 4
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.makeColorRows(list: Array<String>) {
    val cids = makeColorList(list)
    box(RecordingModifier().background(0xFF000000.toInt()).fillMaxWidth().height(4)) {}
    var pad = 0
    for (i in 0 until cids.size) {
        val c = cids[i]
        val name = list[i]
        row(
            RecordingModifier().padding(pad, 0, 4, 0).background(0xFF999999.toInt()).fillMaxWidth()
        ) {
            val dim = 48f

            box(RecordingModifier().padding(8, 0, 8, 0).backgroundId(c).width(dim).height(dim)) {}
            text(name, RecordingModifier(), fontSize = dim)
            beginGlobal()

            val blue = getColorAttribute(c, Rc.ColorAttribute.RED)
            val blueTxt = createTextFromFloat(blue, 1, 3, 0)
            endGlobal()

            text(blueTxt, RecordingModifier().backgroundId(c), fontSize = dim)
        }

        pad = 4
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.makeColorList(list: Array<String>): IntArray {
    val retList = IntArray(list.size)
    beginGlobal()
    for (i in 0 until list.size) {
        val colorName = list[i]
        retList[i] = addColor(0xFF00FF00.toInt())
        setColorName(retList[i], "color.$colorName")
    }
    endGlobal()
    return retList
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.makeColorSet(list: Array<String>): List<ColorSet> {
    val retList = ArrayList<ColorSet>()
    val values = IntArray(list.size)
    val n = list.size / 2
    for (i in 0 until n) {
        val ri = n - i
        val lightColor = 0x101000 * ((ri * 255) / n) or 0xFF000000.toInt()
        val darkColor = 0x000010 * ((i * 255) / n) or 0xFF000000.toInt()

        retList.add(ColorSet(writer, lightColor, list[ri], darkColor, list[i]))
    }
    return retList
}

@Suppress("RestrictedApiAndroidX")
class ColorSet(
    writer: RemoteComposeWriter,
    public val light: Int,
    public val lightName: String,
    public val dark: Int,
    public val darkName: String,
) {
    var id: Short = 0
    var lightId: Short = 0
    var darkId: Short = 0

    init {
        with(writer) {
            beginGlobal()
            lightId = addColor(light).toShort()
            darkId = addColor(dark).toShort()
            setColorName(lightId.toInt(), "color.$lightName")
            setColorName(darkId.toInt(), "color.$darkName")
            id = addThemedColor(lightId, darkId)
            endGlobal()
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun tColor(
    writer: RemoteComposeWriter,
    dark: Int,
    light: Int,
    darkName: String,
    lightName: String,
): Short {
    return writer.addThemedColor(lightName, light, darkName, dark)
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.ScrollColumn(content: RemoteComposeWriterInterface) {
    beginGlobal()
    val touchPosition: Float = addFloatConstant(0f)
    val computedHeight: Float = addFloatConstant(1080f)
    val scrollSize: Float = floatExpression(computedHeight, 6f, AnimatedFloatExpression.MUL)
    val visFloat: Float = addFloatConstant(1f)
    val vis = Utils.idFromNan(visFloat)
    val notVisFloat: Float = addFloatConstant(0f)
    val notVis = Utils.idFromNan(notVisFloat)
    val scrollPosition: Float =
        floatExpression(
            touchPosition,
            computedHeight,
            20f,
            AnimatedFloatExpression.ADD,
            AnimatedFloatExpression.MUL,
        )
    endGlobal()
    column(
        RecordingModifier()
            .fillMaxSize()
            .then(
                CustomScroller(
                    0,
                    ScrollModifier.VERTICAL,
                    touchPosition,
                    scrollPosition,
                    6,
                    scrollSize,
                )
            ),
        ColumnLayout.START,
        ColumnLayout.TOP,
    ) {
        startCanvasOperations()
        val scrollHeight: Float = floatExpression(addComponentHeightValue())

        // -------------force refresh when height changes ---------------
        conditionalOperations(Rc.Condition.NEQ, scrollHeight, computedHeight)
        toggle(visFloat, notVisFloat)
        endConditionalOperations()
        startRunActions()
        val action =
            ValueFloatExpressionChange(
                Utils.idFromNan(computedHeight),
                Utils.idFromNan(scrollHeight),
            )
        addAction(action)
        endRunActions()
        // -------------force refresh when height changes ---------------
        drawComponentContent()
        endCanvasOperations()

        // -------------force refresh when height changes ---------------
        drawComponentContent()
        endCanvasOperations()
        content.run()
    }
}

// =====================================================================================
@SuppressLint("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.toggle(visFloat: Float, notVisFloat: Float) {
    startRunActions()
    val notCalc =
        floatExpression(visFloat, 1f, AnimatedFloatExpression.ADD, 2f, AnimatedFloatExpression.MOD)
    val calc =
        floatExpression(
            notVisFloat,
            1f,
            AnimatedFloatExpression.ADD,
            2f,
            AnimatedFloatExpression.MOD,
        )
    val refresh1 = ValueFloatExpressionChange(Utils.idFromNan(visFloat), Utils.idFromNan(notCalc))
    val refresh2 = ValueFloatExpressionChange(Utils.idFromNan(notVisFloat), Utils.idFromNan(calc))
    addAction(refresh2, refresh1)
    endRunActions()
}

private val test1 = arrayOf("system_accent1_0", "system_accent1_900")

private val system_accent1 =
    arrayOf(
        "system_accent1_0",
        "system_accent1_10",
        "system_accent1_100",
        "system_accent1_1000",
        "system_accent1_200",
        "system_accent1_300",
        "system_accent1_400",
        "system_accent1_50",
        "system_accent1_500",
        "system_accent1_600",
        "system_accent1_700",
        "system_accent1_800",
        "system_accent1_900",
    )
private val system_accent2 =
    arrayOf(
        "system_accent2_0",
        "system_accent2_10",
        "system_accent2_100",
        "system_accent2_1000",
        "system_accent2_200",
        "system_accent2_300",
        "system_accent2_400",
        "system_accent2_50",
        "system_accent2_500",
        "system_accent2_600",
        "system_accent2_700",
        "system_accent2_800",
        "system_accent2_900",
    )
private val system_accent3 =
    arrayOf(
        "system_accent3_0",
        "system_accent3_10",
        "system_accent3_100",
        "system_accent3_1000",
        "system_accent3_200",
        "system_accent3_300",
        "system_accent3_400",
        "system_accent3_50",
        "system_accent3_500",
        "system_accent3_600",
        "system_accent3_700",
        "system_accent3_800",
        "system_accent3_900",
    )
private val system_error =
    arrayOf(
        "system_error_0",
        "system_error_10",
        "system_error_100",
        "system_error_1000",
        "system_error_200",
        "system_error_300",
        "system_error_400",
        "system_error_50",
        "system_error_500",
        "system_error_600",
        "system_error_700",
        "system_error_800",
        "system_error_900",
    )

private val system_neutral1 =
    arrayOf(
        "system_neutral1_0",
        "system_neutral1_10",
        "system_neutral1_100",
        "system_neutral1_1000",
        "system_neutral1_200",
        "system_neutral1_300",
        "system_neutral1_400",
        "system_neutral1_50",
        "system_neutral1_500",
        "system_neutral1_600",
        "system_neutral1_700",
        "system_neutral1_800",
        "system_neutral1_900",
    )
private val system_neutral2 =
    arrayOf(
        "system_neutral2_0",
        "system_neutral2_10",
        "system_neutral2_100",
        "system_neutral2_1000",
        "system_neutral2_200",
        "system_neutral2_300",
        "system_neutral2_400",
        "system_neutral2_50",
        "system_neutral2_500",
        "system_neutral2_600",
        "system_neutral2_700",
        "system_neutral2_800",
        "system_neutral2_900",
    )

private val nameList =
    arrayOf(
        "background_dark",
        "background_light",
        "black",
        "darker_gray",
        "holo_blue_bright",
        "holo_blue_dark",
        "holo_blue_light",
        "holo_green_dark",
        "holo_green_light",
        "holo_orange_dark",
        "holo_orange_light",
        "holo_purple",
        "holo_red_dark",
        "holo_red_light",
        "system_background_dark",
        "system_background_light",
        "system_control_activated_dark",
        "system_control_activated_light",
        "system_control_highlight_dark",
        "system_control_highlight_light",
        "system_control_normal_dark",
        "system_control_normal_light",
        "system_error_container_dark",
        "system_error_container_light",
        "system_error_dark",
        "system_error_light",
        "system_on_background_dark",
        "system_on_background_light",
        "system_on_error_container_dark",
        "system_on_error_container_light",
        "system_on_error_dark",
        "system_on_error_light",
        "system_on_primary_container_dark",
        "system_on_primary_container_light",
        "system_on_primary_dark",
        "system_on_primary_fixed",
        "system_on_primary_fixed_variant",
        "system_on_primary_light",
        "system_on_secondary_container_dark",
        "system_on_secondary_container_light",
        "system_on_secondary_dark",
        "system_on_secondary_fixed",
        "system_on_secondary_fixed_variant",
        "system_on_secondary_light",
        "system_on_surface_dark",
        "system_on_surface_disabled",
        "system_on_surface_light",
        "system_on_surface_variant_dark",
        "system_on_surface_variant_light",
        "system_on_tertiary_container_dark",
        "system_on_tertiary_container_light",
        "system_on_tertiary_dark",
        "system_on_tertiary_fixed",
        "system_on_tertiary_fixed_variant",
        "system_on_tertiary_light",
        "system_outline_dark",
        "system_outline_disabled",
        "system_outline_light",
        "system_outline_variant_dark",
        "system_outline_variant_light",
        "system_palette_key_color_neutral_dark",
        "system_palette_key_color_neutral_light",
        "system_palette_key_color_neutral_variant_dark",
        "system_palette_key_color_neutral_variant_light",
        "system_palette_key_color_primary_dark",
        "system_palette_key_color_primary_light",
        "system_palette_key_color_secondary_dark",
        "system_palette_key_color_secondary_light",
        "system_palette_key_color_tertiary_dark",
        "system_palette_key_color_tertiary_light",
        "system_primary_container_dark",
        "system_primary_container_light",
        "system_primary_dark",
        "system_primary_fixed",
        "system_primary_fixed_dim",
        "system_primary_light",
        "system_secondary_container_dark",
        "system_secondary_container_light",
        "system_secondary_dark",
        "system_secondary_fixed",
        "system_secondary_fixed_dim",
        "system_secondary_light",
        "system_surface_bright_dark",
        "system_surface_bright_light",
        "system_surface_container_dark",
        "system_surface_container_high_dark",
        "system_surface_container_high_light",
        "system_surface_container_highest_dark",
        "system_surface_container_highest_light",
        "system_surface_container_light",
        "system_surface_container_low_dark",
        "system_surface_container_low_light",
        "system_surface_container_lowest_dark",
        "system_surface_container_lowest_light",
        "system_surface_dark",
        "system_surface_dim_dark",
        "system_surface_dim_light",
        "system_surface_disabled",
        "system_surface_light",
        "system_surface_variant_dark",
        "system_surface_variant_light",
        "system_tertiary_container_dark",
        "system_tertiary_container_light",
        "system_tertiary_dark",
        "system_tertiary_fixed",
        "system_tertiary_fixed_dim",
        "system_tertiary_light",
        "system_text_hint_inverse_dark",
        "system_text_hint_inverse_light",
        "system_text_primary_inverse_dark",
        "system_text_primary_inverse_disable_only_dark",
        "system_text_primary_inverse_disable_only_light",
        "system_text_primary_inverse_light",
        "system_text_secondary_and_tertiary_inverse_dark",
        "system_text_secondary_and_tertiary_inverse_disabled_dark",
        "system_text_secondary_and_tertiary_inverse_disabled_light",
        "system_text_secondary_and_tertiary_inverse_light",
    )

/**
 * A demo of a custom Scroller that works with RefreshBugKt::dynamicPaging To achieve paging like
 * behavior
 */
@SuppressLint("RestrictedApiAndroidX")
class CustomScroller
@SuppressLint("RestrictedApiAndroidX")
internal constructor(
    var mMode: Int,
    var mDirection: Int,
    private val mTouchPosition: Float,
    private val mScrollPosition: Float,
    var mNotches: Int,
    var mMax: Float,
) : RecordingModifier.Element {
    var mPositionId: Float = 0f
    var mCustom: CustomTouch? = null

    interface CustomTouch {
        fun touch(max: Float, notchMax: Float): Float
    }

    @SuppressLint("RestrictedApiAndroidX")
    override fun write(writer: RemoteComposeWriter) {
        addModifierCustomScroll(writer, mDirection, mScrollPosition, mTouchPosition, mNotches, mMax)
    }

    @SuppressLint("RestrictedApiAndroidX")
    fun addModifierCustomScroll(
        writer: RemoteComposeWriter,
        direction: Int,
        scrollPosition: Float,
        touchPosition: Float,
        notches: Int,
        max: Float,
    ) {
        // float max = this.reserveFloatVariable();
        val notchMax = writer.reserveFloatVariable()
        val touchExpressionDirection =
            if (direction != 0) RemoteContext.FLOAT_TOUCH_POS_X else RemoteContext.FLOAT_TOUCH_POS_Y

        ScrollModifierOperation.apply(
            writer.getBuffer().getBuffer(),
            direction,
            scrollPosition,
            max,
            notchMax,
        )

        writer
            .getBuffer()
            .addTouchExpression(
                Utils.idFromNan(touchPosition),
                0f,
                if ((mMode and 1) == 0) 0f else Float.Companion.NaN,
                (notches + (if ((mMode and 1) == 0) 0 else 1)).toFloat(),
                0f,
                3,
                floatArrayOf(
                    touchExpressionDirection,
                    max,
                    Rc.FloatExpression.DIV,
                    (notches + 1).toFloat(),
                    AnimatedFloatExpression.MUL,
                    -1f,
                    AnimatedFloatExpression.MUL,
                ),
                if ((mMode and 2) == 0) TouchExpression.STOP_NOTCHES_EVEN
                else TouchExpression.STOP_NOTCHES_SINGLE_EVEN,
                floatArrayOf((notches + (if ((mMode and 1) == 0) 0 else 1)).toFloat()),
                writer.easing(0.5f, 10f, 0.1f),
            )
        writer.getBuffer().addContainerEnd()
        writer.addDebugMessage("scroll " + touchPosition)
    }

    companion object {
        const val VERTICAL: Int = 0
        const val HORIZONTAL: Int = 1
    }
}
