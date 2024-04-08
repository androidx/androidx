/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalTextApi::class, ExperimentalFoundationApi::class)

package androidx.compose.desktop.examples.fonts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.asComposeFontFamily
import androidx.compose.ui.text.platform.composeFontFamilyNameOrNull
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.singleWindowApplication
import java.awt.GraphicsEnvironment
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.skia.FontMgr

fun main() = singleWindowApplication(
    title = "Fonts loading sample"
) {
    MaterialTheme {
        Fonts()
    }
}

@Composable
fun Fonts() {
    Column {
        var fontFamilyInfo by remember { mutableStateOf(FontFamilyInfo.Default) }
        var fontSize by remember { mutableStateOf(48) }

        FontSelectionHeader(
            fontFamilyInfo,
            fontSize,
            onFontFamilyInfoChange = { fontFamilyInfo = it ?: FontFamilyInfo.Default },
            onFontSizeChange = { fontSize = it }
        )

        val scrollState = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(scrollState)) {
                val fontFamily = fontFamilyInfo.fontFamily

                for (rawWeight in 100..900 step 100) {
                    val weight = FontWeight(rawWeight)

                    FontShowcaseRow(
                        fontFamily,
                        fontSize.sp,
                        weight,
                        italic = false,
                        Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(1.dp))

                    FontShowcaseRow(
                        fontFamily,
                        fontSize.sp,
                        weight,
                        italic = true,
                        Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(1.dp))
                }
            }

            VerticalScrollbar(
                rememberScrollbarAdapter(scrollState),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun FontSelectionHeader(
    fontFamilyInfo: FontFamilyInfo,
    fontSize: Int,
    onFontFamilyInfoChange: (FontFamilyInfo?) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var useAwtFonts by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Use system fonts",
                modifier = Modifier.clickable(
                    remember { MutableInteractionSource() },
                    indication = null
                ) {
                    useAwtFonts = false
                    onFontFamilyInfoChange(null)
                }
            )

            Switch(
                useAwtFonts,
                {
                    useAwtFonts = it
                    onFontFamilyInfoChange(FontFamilyInfo.Default)
                }
            )

            Text(
                text = "Use AWT fonts",
                modifier = Modifier.clickable(
                    remember { MutableInteractionSource() },
                    indication = null
                ) {
                    useAwtFonts = true
                    onFontFamilyInfoChange(null)
                }
            )
        }

        if (useAwtFonts) {
            AwtFontsSelector(fontFamilyInfo, onFontFamilyInfoChange, Modifier.weight(1f))
        } else {
            SystemFontsSelector(fontFamilyInfo, onFontFamilyInfoChange, Modifier.weight(1f))
        }

        FontSizePicker(fontSize, Modifier.fillMaxWidth().weight(2f), onFontSizeChange)
    }
}

data class FontFamilyInfo(
    val familyName: String,
    val fontFamily: FontFamily,
    val source: Source,
) {
    enum class Source {
        AwtInterop, System
    }

    companion object {
        val Default = FontFamilyInfo(
            familyName = "[Default font]",
            fontFamily = FontFamily.Default,
            source = Source.System
        )
    }
}

@Composable
private fun AwtFontsSelector(
    fontFamilyInfo: FontFamilyInfo,
    onFontFamilyInfoChange: (FontFamilyInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fontFamilies by remember { mutableStateOf(emptyList<FontFamilyInfo>()) }
    LaunchedEffect(Unit) {
        launch(Dispatchers.Default) {
            fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .allFonts
                .map { font ->

                    FontFamilyInfo(
                        familyName = font.composeFontFamilyNameOrNull() ?: font.family,
                        fontFamily = font.asComposeFontFamily(),
                        source = FontFamilyInfo.Source.AwtInterop,
                    )
                }
                .distinctBy { it.familyName }
                .sortedBy { it.familyName }
        }
    }

    FontFamilyPicker(fontFamilies, fontFamilyInfo, onFontFamilyInfoChange, modifier)
}

@Composable
private fun SystemFontsSelector(
    fontFamilyInfo: FontFamilyInfo,
    onFontFamilyInfoChange: (FontFamilyInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fontFamilies by remember { mutableStateOf(emptyList<FontFamilyInfo>()) }

    LaunchedEffect(Unit) {
        val skiaFontManager = FontMgr.default
        launch(Dispatchers.Default) {
            fontFamilies = (0 until skiaFontManager.familiesCount)
                .map { i -> skiaFontManager.getFamilyName(i) }
                .sorted()
                .map { familyName ->
                    FontFamilyInfo(
                        familyName = familyName,
                        fontFamily = FontFamily(familyName),
                        source = FontFamilyInfo.Source.System
                    )
                }
        }
    }

    FontFamilyPicker(fontFamilies, fontFamilyInfo, onFontFamilyInfoChange, modifier)
}

@Composable
private fun FontFamilyPicker(
    families: List<FontFamilyInfo>,
    fontFamilyInfo: FontFamilyInfo,
    onFontFamilyInfoChange: (FontFamilyInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropdownOpen by remember { mutableStateOf(false) }

    Box(modifier) {
        val borderWidth = if (dropdownOpen) 2.dp else 1.dp
        val borderColor = if (dropdownOpen) Color.Blue else Color.Gray
        val shape = remember { RoundedCornerShape(4.dp) }
        Row(
            modifier = Modifier.border(borderWidth, borderColor, shape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .onClick(enabled = families.isNotEmpty()) { dropdownOpen = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (families.isNotEmpty()) {
                Text(
                    text = fontFamilyInfo.familyName,
                    fontFamily = fontFamilyInfo.fontFamily,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    style = LocalTextStyle.current.copy(fontSynthesis = FontSynthesis.None),
                )
            } else {
                Text(
                    text = "Loading...",
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).alpha(ContentAlpha.disabled),
                )
            }

            Text(
                text = "⌄",
                modifier = Modifier.align(Alignment.Top)
                    .padding(bottom = 8.dp)
                    .alpha(if (families.isNotEmpty()) ContentAlpha.high else ContentAlpha.disabled),
                fontWeight = FontWeight.ExtraBold,
            )
        }

        DropdownMenu(
            expanded = dropdownOpen,
            { dropdownOpen = false },
            properties = PopupProperties(focusable = true)
        ) {
            for (familyInfo in families) {
                val (familyName, fontFamily) = familyInfo
                DropdownItem(
                    modifier = Modifier.height(30.dp),
                    onClick = {
                        dropdownOpen = false
                        onFontFamilyInfoChange(familyInfo)
                    },
                ) {
                    if (fontFamilyInfo.fontFamily == fontFamily) {
                        // TODO the fixed width is going to break if font scaling is active
                        Text(
                            text = "✔️",
                            fontSize = 14.sp,
                            modifier = Modifier.width(24.dp).align(Alignment.CenterVertically)
                        )
                    } else {
                        Spacer(Modifier.width(24.dp))
                    }
                    Text(
                        text = familyName,
                        fontFamily = fontFamily,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = LocalTextStyle.current.copy(fontSynthesis = FontSynthesis.None),
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProvideTextStyle(MaterialTheme.typography.subtitle1) {
            val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled
            CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
                content()
            }
        }
    }
}

@Composable
private fun FontSizePicker(
    fontSize: Int,
    modifier: Modifier = Modifier,
    onSizeChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Font size:",
        )
        Slider(
            value = fontSize.toFloat(),
            onValueChange = { onSizeChange(it.toInt()) },
            valueRange = 1f..192f,
            steps = 0,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = fontSize.toString(),
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.width(50.dp)
        )
    }
}

@Composable
private fun FontShowcaseRow(
    fontFamily: FontFamily,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    italic: Boolean,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            Column(
                Modifier.width(100.dp).background(Color.LightGray).padding(8.dp).layoutId("header"),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                Text(
                    "Weight: ${fontWeight.weight}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.Gray,
                )
                Text(
                    "Italic: ${if (italic) "yes" else "no"}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.Gray,
                )
            }

            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                style = TextStyle(
                    fontSize = fontSize,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                    fontSynthesis = FontSynthesis.None,
                ),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.layoutId("text"),
            )
        },
        measurePolicy = { measurables, constraints ->
            val headerMeasurable = measurables.single { it.layoutId == "header" }
            val textMeasurable = measurables.single { it.layoutId == "text" }
            val totalHeight = max(
                headerMeasurable.minIntrinsicHeight(Int.MAX_VALUE),
                textMeasurable.minIntrinsicHeight(Int.MAX_VALUE)
            )

            val headerConstraints =
                constraints.copy(minWidth = 0, minHeight = totalHeight, maxHeight = totalHeight)
            val header = headerMeasurable.measure(headerConstraints)

            val textConstraints = Constraints(maxHeight = header.height)
            val text = textMeasurable.measure(textConstraints)

            val totalWidth =
                if (!constraints.hasFixedWidth) {
                    (header.width + text.width).coerceIn(constraints.minWidth, constraints.maxWidth)
                } else {
                    constraints.maxWidth
                }

            layout(totalWidth, totalHeight) {
                header.place(0, 0)
                val textY =
                    if (text.height >= header.height) {
                        0
                    } else {
                        header.height / 2 - text.height / 2
                    }
                text.place(header.width + 4.dp.roundToPx(), textY)
            }
        }
    )
}
