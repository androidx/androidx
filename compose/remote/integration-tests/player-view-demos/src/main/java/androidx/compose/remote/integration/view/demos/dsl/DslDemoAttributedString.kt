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

package androidx.compose.remote.integration.view.demos.dsl

import android.graphics.Color
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.awt.font.TextAttribute
import java.text.AttributedCharacterIterator
import java.text.AttributedString

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DemoAttributedStringPreview() {
    RemoteDocPreview(RemoteDocument(demoAttributedString()))
}

@Suppress("RestrictedApiAndroidX")
fun demoAttributedString(): ByteArray {
    val attributedString = sampleAttributedString()

    // Collect runs and split by newline
    val lines = mutableListOf<MutableList<Pair<String, IntArray>>>()
    var currentLine = mutableListOf<Pair<String, IntArray>>()
    lines.add(currentLine)

    extractRuns(attributedString) { text, start, end, keyValue ->
        val str = text.substring(start, end)
        if (str.indexOf('\n') >= 0) {
            val split = str.split("\n")
            for (i in split.indices) {
                if (i > 0) {
                    currentLine = mutableListOf()
                    lines.add(currentLine)
                }
                if (split[i].isNotEmpty()) {
                    currentLine.add(Pair(split[i], keyValue))
                }
            }
        } else {
            currentLine.add(Pair(str, keyValue))
        }
    }

    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(modifier = Modifier.fillMaxSize().background(Color.WHITE).padding(4.rdp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontal = RcHorizontalPositioning.Start,
                vertical = RcColumnVerticalPositioning.Top,
            ) {
                for (line in lines) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontal = RcRowHorizontalPositioning.Start,
                        vertical = RcVerticalPositioning.Top,
                    ) {
                        for (run in line) {
                            val textId = remoteText(run.first)
                            println(" >>> " + run.first)
                            emitText(textId, run.second)
                        }
                    }
                }
            }
        }
    }
}

// Helper to emit text with attributes in DSL
@Suppress("RestrictedApiAndroidX")
private fun RcScope.emitText(text: RcText, keyValues: IntArray) {
    var underline = false
    var strikethrough = false
    var color = 0xFF000000.toInt()
    var fontSize = 46f
    var fontStyle = FontStyle.Normal
    var fontWeight = 400f
    var modifier: Modifier = Modifier.alignByBaseline()

    var i = 0
    while (i < keyValues.size) {
        when (keyValues[i]) {
            AttributeRun.POSTURE -> {
                when (keyValues[i + 1]) {
                    AttributeRun.POSTURE_BOLD -> fontWeight = 700f
                    AttributeRun.POSTURE_ITALIC -> fontStyle = FontStyle.Italic
                }
            }

            AttributeRun.FOREGROUND -> color = keyValues[i + 1]
            AttributeRun.BACKGROUND -> modifier = modifier.background(keyValues[i + 1])
            AttributeRun.UNDERLINE -> underline = true
            AttributeRun.STRIKETHROUGH -> strikethrough = true
            AttributeRun.SIZE -> fontSize *= java.lang.Float.intBitsToFloat(keyValues[i + 1])
        }
        i += 2
    }

    Text(text = text, modifier = modifier, color = color, fontSize = fontSize.rsp)
    //    Text(
    //        text = text,
    //        modifier = modifier,
    //        color = color,
    //        fontSize = fontSize.rsp,
    //        fontStyle = fontStyle,
    //        fontWeight = fontWeight,
    //        underline = underline,
    //        strikethrough = strikethrough,
    //    )
}

// Replicate the AttributedString generation
@Suppress("RestrictedApiAndroidX")
private fun sampleAttributedString(): AttributedString {
    val text =
        "AttributedString Demo:\n" +
            "This is Bold, this is Italic.\n" +
            "This text is Red, this is Blue,\n" +
            " and this has a Yellow Background.\n" +
            "This is Underlined, and this has a Strikethrough.\n" +
            "This is Big, and this is Superscript²."

    val attributedString = AttributedString(text)
    try {
        val boldStart = text.indexOf("Bold")
        attributedString.addAttribute(
            TextAttribute.WEIGHT,
            TextAttribute.WEIGHT_BOLD,
            boldStart,
            boldStart + "Bold".length,
        )

        val italicStart = text.indexOf("Italic")
        attributedString.addAttribute(
            TextAttribute.POSTURE,
            TextAttribute.POSTURE_OBLIQUE,
            italicStart,
            italicStart + "Italic".length,
        )

        val redStart = text.indexOf("Red")
        attributedString.addAttribute(
            TextAttribute.FOREGROUND,
            Color.RED,
            redStart,
            redStart + "Red".length,
        )

        val blueStart = text.indexOf("Blue")
        attributedString.addAttribute(
            TextAttribute.FOREGROUND,
            Color.BLUE,
            blueStart,
            blueStart + "Blue".length,
        )

        val backgroundStart = text.indexOf("Yellow Background")
        attributedString.addAttribute(
            TextAttribute.BACKGROUND,
            Color.YELLOW,
            backgroundStart,
            backgroundStart + "Yellow Background".length,
        )

        val underlineStart = text.indexOf("Underlined")
        attributedString.addAttribute(
            TextAttribute.UNDERLINE,
            TextAttribute.UNDERLINE_ON,
            underlineStart,
            underlineStart + "Underlined".length,
        )

        val strikeStart = text.indexOf("Strikethrough")
        attributedString.addAttribute(
            TextAttribute.STRIKETHROUGH,
            TextAttribute.STRIKETHROUGH_ON,
            strikeStart,
            strikeStart + "Strikethrough".length,
        )

        val bigStart = text.indexOf("Big")
        attributedString.addAttribute(TextAttribute.SIZE, 2.0f, bigStart, bigStart + "Big".length)

        val superStart = text.indexOf("Superscript²")
        attributedString.addAttribute(
            TextAttribute.SUPERSCRIPT,
            TextAttribute.SUPERSCRIPT_SUPER,
            superStart + "Superscript".length,
            superStart + "Superscript²".length,
        )
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
    return attributedString
}

private object AttributeRun {
    const val POSTURE = 2
    const val FOREGROUND = 3
    const val BACKGROUND = 4
    const val UNDERLINE = 5
    const val STRIKETHROUGH = 6
    const val SUPERSCRIPT = 7
    const val SIZE = 8
    const val POSTURE_BOLD = 1
    const val POSTURE_ITALIC = 2
}

@Suppress("RestrictedApiAndroidX")
private fun extractRuns(attrStr: AttributedString, cb: (String, Int, Int, IntArray) -> Unit) {
    val iterator = attrStr.iterator
    val sb = StringBuilder()
    var c = iterator.first()
    while (c != AttributedCharacterIterator.DONE) {
        sb.append(c)
        c = iterator.next()
    }
    val text = sb.toString()
    iterator.first()
    while (iterator.getIndex() < iterator.getEndIndex()) {
        val runStart = iterator.runStart
        val runLimit = iterator.runLimit
        val attributes = iterator.attributes
        val keyValues = IntArray(attributes.size * 2)
        var count = 0

        attributes.forEach { (attribute, value) ->
            if (attribute == TextAttribute.WEIGHT && value == TextAttribute.WEIGHT_BOLD) {
                keyValues[count++] = AttributeRun.POSTURE
                keyValues[count++] = AttributeRun.POSTURE_BOLD
            } else if (
                attribute == TextAttribute.POSTURE && value == TextAttribute.POSTURE_OBLIQUE
            ) {
                keyValues[count++] = AttributeRun.POSTURE
                keyValues[count++] = AttributeRun.POSTURE_ITALIC
            } else if (attribute == TextAttribute.FOREGROUND) {
                keyValues[count++] = AttributeRun.FOREGROUND
                keyValues[count++] = value as Int
            } else if (attribute == TextAttribute.BACKGROUND) {
                keyValues[count++] = AttributeRun.BACKGROUND
                keyValues[count++] = value as Int
            } else if (
                attribute == TextAttribute.UNDERLINE && value == TextAttribute.UNDERLINE_ON
            ) {
                keyValues[count++] = AttributeRun.UNDERLINE
                keyValues[count++] = 1
            } else if (
                attribute == TextAttribute.STRIKETHROUGH && value == TextAttribute.STRIKETHROUGH_ON
            ) {
                keyValues[count++] = AttributeRun.STRIKETHROUGH
                keyValues[count++] = 1
            } else if (
                attribute == TextAttribute.SUPERSCRIPT && value == TextAttribute.SUPERSCRIPT_SUPER
            ) {
                keyValues[count++] = AttributeRun.SUPERSCRIPT
                keyValues[count++] = 1
            } else if (attribute == TextAttribute.SIZE) {
                keyValues[count++] = AttributeRun.SIZE
                keyValues[count++] = java.lang.Float.floatToRawIntBits(value as Float)
            }
        }
        cb(text, runStart, runLimit, keyValues)
        iterator.setIndex(runLimit)
    }
}
