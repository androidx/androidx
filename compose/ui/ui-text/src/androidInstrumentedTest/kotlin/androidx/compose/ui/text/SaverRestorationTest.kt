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

package androidx.compose.ui.text

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextForegroundStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.collections.get
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaverRestorationTest {

    @get:Rule val rule = createComposeRule()

    // FontFamily Saver is not supported yet.
    // PlatformStyle and drawStyle are not saved.
    val spanStyleParamValueMap =
        mapOf<String, Any?>(
            "textForegroundStyle" to TextForegroundStyle.from(Color.Green),
            "fontSize" to 16.sp,
            "fontWeight" to FontWeight(400),
            "fontStyle" to FontStyle.Italic,
            "fontSynthesis" to FontSynthesis.Weight,
            "fontFeatureSettings" to "settings",
            "letterSpacing" to 10.sp,
            "baselineShift" to BaselineShift.Subscript,
            "textGeometricTransform" to TextGeometricTransform(scaleX = 2.0f),
            "localeList" to LocaleList("en"),
            "background" to Color.Yellow,
            "textDecoration" to TextDecoration.Underline,
            "shadow" to Shadow(color = Color.Red),
        )

    val paragraphStyleParamValueMap =
        mapOf<String, Any?>(
            "textAlign" to TextAlign.Left,
            "textDirection" to TextDirection.Ltr,
            "lineHeight" to 24.sp,
            "textIndent" to TextIndent(firstLine = 16.sp),
            "platformStyle" to
                PlatformParagraphStyle(EmojiSupportMatch.All, includeFontPadding = false),
            "lineHeightStyle" to
                LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.Both),
            "lineBreak" to LineBreak.Simple,
            "hyphens" to Hyphens.Auto,
            "textMotion" to TextMotion.Static,
        )

    @Test
    fun spanStyle_restoration() {
        val restorationTester = StateRestorationTester(rule)
        var style: MutableState<SpanStyle>? = null

        restorationTester.setContent {
            style = rememberSaveable(stateSaver = SpanStyleSaver) { mutableStateOf(SpanStyle()) }
        }

        val spanStyleConstructor = SpanStyle::class.primaryConstructor!!

        val args = mutableMapOf<KParameter, Any?>()
        // Fill in necessary parameters first
        for (parameter in spanStyleConstructor.parameters) {
            if (!parameter.isOptional) {
                args.put(parameter, spanStyleParamValueMap[parameter.name])
            }
        }

        for (parameter in spanStyleConstructor.parameters) {
            args.put(parameter, spanStyleParamValueMap[parameter.name])
            val newSpanStyle = spanStyleConstructor.callBy(args)

            rule.runOnIdle {
                style!!.value = newSpanStyle
                // we null it to ensure recomposition happened
                style = null
            }

            restorationTester.emulateSavedInstanceStateRestore()

            rule.runOnIdle { assertThat(style!!.value).isEqualTo(newSpanStyle) }

            if (parameter.isOptional) {
                args.remove(parameter)
            }
        }
    }

    @Test
    fun paragraphStyle_restoration() {
        val restorationTester = StateRestorationTester(rule)
        var style: MutableState<ParagraphStyle>? = null

        restorationTester.setContent {
            style =
                rememberSaveable(stateSaver = ParagraphStyleSaver) {
                    mutableStateOf(ParagraphStyle())
                }
        }

        val paragraphStyleConstructor = ParagraphStyle::class.primaryConstructor!!

        val args = mutableMapOf<KParameter, Any?>()

        for (parameter in paragraphStyleConstructor.parameters) {
            args.put(parameter, paragraphStyleParamValueMap[parameter.name])
            val newParagraphStyle = paragraphStyleConstructor.callBy(args)

            rule.runOnIdle {
                style!!.value = newParagraphStyle
                // we null it to ensure recomposition happened
                style = null
            }

            restorationTester.emulateSavedInstanceStateRestore()

            rule.runOnIdle { assertThat(style!!.value).isEqualTo(newParagraphStyle) }

            if (parameter.isOptional) {
                args.remove(parameter)
            }
        }
    }
}
