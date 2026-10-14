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

package androidx.text.vertical.testapp

import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.text.vertical.AnnotationPosition
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.FontShearSpan
import androidx.text.vertical.compose.VerticalText
import androidx.text.vertical.compose.VerticalTextStyle
import androidx.text.vertical.compose.buildVerticalText

@Composable
fun LongText(style: VerticalTextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val text = remember(density) { makeSampleText(density) }
    VerticalText(text, modifier, style, overflow = TextOverflow.Visible)
}

@Composable
fun LongHorizontalText(style: VerticalTextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val text = remember(density) { makeSampleText(density) }
    LegacyHorizontalText(text, style, modifier)
}

@Composable
fun ComplexHorizontalText(style: VerticalTextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val text =
        remember(density) {
            buildVerticalText(density) {
                // Ruby annotation position. In horizontal writing mode, Before places the ruby
                // above the base text and After places it below.
                text("ルビ位置：")
                withRuby("うえ", position = AnnotationPosition.Before) { text("上") }
                text("と")
                withRuby("した", position = AnnotationPosition.After) { text("下") }
                text("。\n")
                // Emphasis annotation position. In horizontal writing mode, Before places the
                // emphasis mark above the base text and After places it below.
                text("傍点位置：")
                withEmphasis(density, position = AnnotationPosition.Before) { text("上") }
                text("と")
                withEmphasis(density, position = AnnotationPosition.After) { text("下") }
                text("。\n")
                withEmphasis(EmphasisStyle.Circle) { text("圏点") }
                text("も")
                withEmphasis(EmphasisStyle.DoubleCircle) { text("二重丸") }
                text("もSupport")
                withEmphasis(EmphasisStyle.Sesame) { text("されて") }
                withEmphasis(
                    density,
                    style = EmphasisStyle.Triangle,
                    position = AnnotationPosition.After,
                ) {
                    text("います。")
                }
            }
        }
    LegacyHorizontalText(text, style, modifier)
}

fun makeSampleText(density: Density) =
    buildVerticalText(density) {
        text("吾輩は猫である。", mapOf("吾輩" to "わがはい", "猫" to "ねこ"))
        text("名前はまだ無い。", mapOf("名前" to "なまえ", "無" to "な"))
        text("\n")
        text("どこで生まれたかとんと見当がつかぬ。", mapOf("見当" to "けんとう"))
        text("何でも薄暗いじめじめしたところでニャーニャー泣いていた事だけは記憶している。")
        text("吾輩はここで始めて人間というものを見た。")
        text("しかもあとで聞くとそれは書生という人間中で一番獰悪な種族であったそうだ。", mapOf("獰悪" to "どうあく"))
        text("この書生というのは時々我々を捕えて煮て食うという話である。", mapOf("捕" to "つかま", "煮" to "に"))
        text("しかしその当時は何という考もなかったから別段恐しいとも思わなかった。")
        text("ただ彼の掌に載せられてスーと持ち上げられた時何だかフワフワした感じがあったばかりである。", mapOf("掌" to "てのひら"))
        text("掌の上で少し落ちついて書生の顔を見たのがいわゆる人間というものの見始であろう。", mapOf("見始" to "みはじめ"))
        text("この時妙なものだと思った感じが今でも残っている。")
        text("第一毛をもって装飾されべきはずの顔がつるつるしてまるで薬缶だ。", mapOf("薬缶" to "やかん"))
        text("その後猫にもだいぶ逢ったがこんな片輪には一度も出会わした事がない。", mapOf("片輪" to "かたわ", "出会" to "であ"))
        text("のみならず顔の真中があまりに突起している。")
        text("そうしてその穴の中から時々ぷうぷうと煙を吹く。", mapOf("煙" to "けむり"))
        text("どうも咽せぽくて実に弱った。", mapOf("咽" to "む"))
        text("これが人間の飲む煙草というものである事はようやくこの頃知った。", mapOf("煙草" to "たばこ"))
        text("\n")
    }

@Composable
fun ComplexText(style: VerticalTextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val text = remember(density) { buildComplexText(density) }
    VerticalText(text, modifier, style, overflow = TextOverflow.Visible)
}

private fun buildComplexText(density: Density) =
    buildVerticalText(density) {
        // Ruby annotation position. In vertical writing, Before puts the ruby on the right of the
        // base text and After puts it on the left.
        text("ルビ位置：")
        withRuby("みぎ", position = AnnotationPosition.Before) { text("右") }
        text("と")
        withRuby("ひだり", position = AnnotationPosition.After) { text("左") }
        text("。")

        // Emphasis annotation position. In vertical writing, Before puts the emphasis mark on the
        // right of the base text and After puts it on the left.
        text("傍点位置：")
        withEmphasis(density, position = AnnotationPosition.Before) { text("右") }
        text("と")
        withEmphasis(density, position = AnnotationPosition.After) { text("左") }
        text("。\n")

        upright("2024")
        text("年の")
        withRuby("クリスマス") {
            combineUpright("12")
            text("月")
            combineUpright("25")
            text("日")
        }
        text("に")
        sideways("Google Pixel")
        text("を買う。\n")

        upright("2024")
        text("年は")
        combineUpright("2024")
        text("年ともかけるし")
        sideways("2024年")
        text("ともかけるよ。\n")

        text("もちろん")
        withStyle(textColor = Color.Red) {
            withRuby(
                buildVerticalText(density) {
                    text("インライン")
                    withStyle(fontSize = 1.5.em) { text("スタイリング") }
                }
            ) {
                withStyle(fontSize = 0.8.em) { sideways("inline ") }
                withStyle(backgroundColor = Color.Green) { sideways("styling") }
            }
            withStyle(backgroundColor = Color.LightGray) {
                text("も")
                withStyle(fontSize = 2.em) { text("可能") }
                text("です。\n")
            }
        }

        combineUpright(
            buildVerticalText(density) { // Tate Chu Yoko only respect styling.
                text("2")
                withStyle(backgroundColor = Color.Red) { text("0") }
                withStyle(backgroundColor = Color.Green) { text("2") }
                text("5")
            }
        )
        text("年もよろしくお願いいたします。")

        withStyle(fontShear = FontShearSpan.DEFAULT_FONT_SHEAR) {
            text("日本語の斜体はEnglishのItalicとは少し違います。\n")
        }
        withEmphasis(style = EmphasisStyle.Sesame) { text("傍点") }
        text("も")
        text(
            SpannableString("傍線").apply {
                setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )
        text("も")
        text(
            SpannableString("取消線").apply {
                setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )
        text("もSupportされてます。")
    }
