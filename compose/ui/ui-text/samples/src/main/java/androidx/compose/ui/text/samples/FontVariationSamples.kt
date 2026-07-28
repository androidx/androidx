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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation.Settings
import androidx.compose.ui.text.font.FontVariation.italic
import androidx.compose.ui.text.font.FontVariation.weight
import androidx.compose.ui.text.font.FontVariation.width

@Sampled
fun FontVariationSettingsMergeSettingsSample() {
    // Define base settings shared across the typography (e.g., normal weight, standard width)
    val baseVariationSettings = Settings(weight(400), width(100f))

    val customTypography =
        Typography(
            displayLarge =
                TextStyle(
                    fontFamily =
                        FontFamily(
                            Font(
                                resId = R.font.myfont,
                                // weight(700), width(100f)
                                variationSettings =
                                    baseVariationSettings.merge(Settings(weight(700))),
                            )
                        )
                ),
            bodyMedium =
                TextStyle(
                    fontFamily =
                        FontFamily(
                            Font(
                                resId = R.font.myfont,
                                // weight(400), width(90f)
                                variationSettings =
                                    baseVariationSettings.merge(Settings(width(90f))),
                            )
                        )
                ),
        )
}

@Sampled
fun FontVariationSettingsMergeVarargSample() {
    // Define base settings shared across the typography (e.g., normal weight, standard width)
    val baseVariationSettings = Settings(weight(400), width(100f))

    val customTypography =
        Typography(
            displayLarge =
                TextStyle(
                    fontFamily =
                        FontFamily(
                            Font(
                                resId = R.font.myfont,
                                // weight(700), width(100f), italic(1.0f)
                                variationSettings =
                                    baseVariationSettings.merge(weight(700), italic(1.0f)),
                            )
                        )
                ),
            bodyMedium =
                TextStyle(
                    fontFamily =
                        FontFamily(
                            Font(
                                resId = R.font.myfont,
                                // weight(400), width(90f)
                                variationSettings = baseVariationSettings.merge(width(90f)),
                            )
                        )
                ),
        )
}
