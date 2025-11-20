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

package androidx.compose.foundation.demos.text

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastJoinToString

val DEMO_CASES =
    listOf(
        FontVariation.Settings(FontVariation.Setting("wght", 100f)),
        FontVariation.Settings(FontVariation.Setting("wght", 300f)),
        FontVariation.Settings(FontVariation.Setting("wght", 400f)),
        FontVariation.Settings(FontVariation.Setting("wght", 500f)),
        FontVariation.Settings(FontVariation.Setting("wght", 700f)),
        FontVariation.Settings(FontVariation.Setting("wght", 900f)),
        FontVariation.Settings(FontVariation.Setting("ROND", 100f)),
    )

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun FontVariationSettingsDemo() {
    if (Build.VERSION.SDK_INT < 26) {
        Text("Variable fonts are only supported on API 26+")
    }

    var familyName by remember { mutableStateOf("sans-serif") }
    val density = LocalDensity.current

    LazyColumn {
        this.stickyHeader {
            Row {
                Text("Family Name:", fontSize = 18.sp)
                BasicTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 18.sp),
                )
            }
        }

        for (settings in DEMO_CASES) {
            item {
                TagLine(
                    tag =
                        settings.settings.fastJoinToString {
                            "${it.axisName} : ${it.toVariationValue(density)}"
                        }
                )
                DeviceNamedFontFamilyFont(familyName, settings)
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun DeviceNamedFontFamilyFont(familyName: String, axes: FontVariation.Settings) {
    Column(Modifier.fillMaxWidth()) {
        val deviceFonts =
            remember(familyName, axes) {
                if (familyName.isEmpty()) {
                    FontFamily(Font(DeviceFontFamilyName("sans-serif"), variationSettings = axes))
                } else {
                    FontFamily(Font(DeviceFontFamilyName(familyName), variationSettings = axes))
                }
            }

        Text(
            "ABCDEFG",
            fontSize = 48.sp,
            fontFamily = deviceFonts,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
