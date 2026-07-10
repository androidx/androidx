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

package androidx.wear.tiles.snippet_samples

import androidx.annotation.RawRes
import androidx.annotation.Sampled
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.layout.imageResource
import androidx.wear.protolayout.layout.lottieResource
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.backgroundImage
import androidx.wear.protolayout.material3.imageButton
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile

/** Returns basic [Material3TileService] with Material3 layout and automatic resources handling. */
@Sampled
fun material3TileServiceHelloWorld(@RawRes lottieResId: Int, myColorScheme: ColorScheme) =
    object : Material3TileService(defaultColorScheme = myColorScheme) {
        override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile =
            Tile.Builder()
                .setTileTimeline(
                    Timeline.fromLayoutElement(
                        primaryLayout(
                            mainSlot = {
                                imageButton(
                                    onClick = clickable(),
                                    backgroundContent = {
                                        backgroundImage(
                                            resource =
                                                imageResource(
                                                    lottie =
                                                        lottieResource(rawResourceId = lottieResId)
                                                )
                                        )
                                    },
                                )
                            }
                        )
                    )
                )
                .build()
    }
