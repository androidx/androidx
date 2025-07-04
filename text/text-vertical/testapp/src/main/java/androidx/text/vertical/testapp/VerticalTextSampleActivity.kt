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

package androidx.text.vertical.testapp

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.TextPaint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.text.vertical.VerticalTextLayout
import java.util.Locale
import kotlin.math.max

class VerticalTextSampleActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val demos =
                arrayOf<Pair<String, @Composable () -> Unit>>(
                    "Long Text" to { ZoomableVerticalText { LongText(it) } },
                    "Complex Text" to { ZoomableVerticalText { ComplexText(it) } },
                )

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    var selectedTabIndex by remember { mutableIntStateOf(0) }
                    PrimaryTabRow(selectedTabIndex = 0, modifier = Modifier.fillMaxWidth()) {
                        demos.forEachIndexed { index, (title, _) ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) },
                            )
                        }
                    }
                    demos[selectedTabIndex].second()
                }
            }
        }
    }
}

@Composable
fun ZoomableVerticalText(content: @Composable (TextPaint) -> Unit) {
    val fontSize = with(LocalDensity.current) { 32.sp.toPx() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val paint =
        remember(zoom) {
            TextPaint().apply {
                textSize = fontSize * zoom
                typeface = Typeface.SERIF
                textLocale =
                    Locale.Builder()
                        .setLocale(Locale.JAPANESE)
                        .setUnicodeLocaleKeyword("lb", "strict")
                        .build()
            }
        }

    Box(
        modifier =
            Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoom = 1f
                            offsetX = 0f
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, offsetChange, gestureZoom, _ ->
                        zoom = zoom * gestureZoom
                        offsetX = max(0f, offsetX + offsetChange.x)
                    }
                }
                .graphicsLayer(translationX = offsetX)
    ) {
        content(paint)
    }
}

@Composable
fun VerticalText(text: Spanned, paint: TextPaint, modifier: Modifier = Modifier) {
    var vTextLayout by remember { mutableStateOf<VerticalTextLayout?>(null) }
    Layout(
        modifier =
            modifier.fillMaxSize().drawWithContent {
                drawIntoCanvas { c ->
                    vTextLayout?.draw(c.nativeCanvas, c.nativeCanvas.width.toFloat(), 0f)
                }
            },
        content = {},
    ) { _, constraints ->
        vTextLayout =
            VerticalTextLayout.Builder(
                    text = text,
                    start = 0,
                    end = text.length,
                    paint = paint,
                    height = constraints.maxHeight.toFloat(),
                )
                .build()
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

@Composable
fun LongText(paint: TextPaint, modifier: Modifier = Modifier) {
    VerticalText(
        buildVerticalText {
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
            text("その後猫にもだいぶ逢ったがこんな片輪には一度も出会わした事がない。", mapOf("片端" to "かたわ", "出会" to "でく"))
            text("のみならず顔の真中があまりに突起している。")
            text("そうしてその穴の中から時々ぷうぷうと煙を吹く。", mapOf("煙" to "けむり"))
            text("どうも咽せぽくて実に弱った。", mapOf("咽" to "む"))
            text("これが人間の飲む煙草というものである事はようやくこの頃知った。", mapOf("煙草" to "たばこ"))
            text("\n")
        },
        paint,
        modifier,
    )
}

@Composable
fun ComplexText(paint: TextPaint, modifier: Modifier = Modifier) {
    VerticalText(
        buildVerticalText {
            Upright("2024")
            text("年の")
            ruby("クリスマス") {
                TateChuYoko("12")
                text("月")
                TateChuYoko("25")
                text("日")
            }
            text("に")
            Sideways("Google Pixel")
            text("を買う。\n")

            Upright("2024")
            text("年は")
            TateChuYoko("2024")
            text("年ともかけるし")
            Sideways("2024年")
            text("ともかけるよ。\n")

            text("もちろん")
            withStyle(textColor = Color.Red) {
                ruby(
                    buildVerticalText {
                        text("インライン")
                        withStyle(fontSize = 1.5.em) { text("スタイリング") }
                    }
                ) {
                    withStyle(fontSize = 0.8.em) { Sideways("inline ") }
                    withStyle(backgroundColor = Color.Green) { Sideways("styling") }
                }
                withStyle(backgroundColor = Color.LightGray) {
                    text("も")
                    withStyle(fontSize = 2.em) { text("可能") }
                    text("です。\n")
                }
            }

            TateChuYoko(
                buildVerticalText { // Tate Chu Yoko only respect styling.
                    text("2")
                    withStyle(backgroundColor = Color.Red) { text("0") }
                    withStyle(backgroundColor = Color.Green) { text("2") }
                    text("5")
                }
            )
            text("年もよろしくお願いいたします。")

            withFontShear { text("日本語の斜体はEnglishのItalicとは少し違います。") }
            withEmphasis { text("傍点もSupportされてます。") }
        },
        paint,
        modifier,
    )
}
