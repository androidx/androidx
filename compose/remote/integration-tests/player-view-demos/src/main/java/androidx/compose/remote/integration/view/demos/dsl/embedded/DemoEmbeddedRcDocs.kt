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

package androidx.compose.remote.integration.view.demos.dsl.embedded

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/**
 * Demo that embeds 5 existing `.rc` documents from `res/raw/` into a single composite page using
 * [RemoteComposeContextAndroid.rcDoc]:
 * 1. `cube32.rc` (alias for `res/raw/cube3d.rc`)
 * 2. `flappy.rc` (`res/raw/flappy.rc`)
 * 3. `stop_notches_even.rc` (`res/raw/stop_notches_even.rc`)
 * 4. `shader_calendar.rc` (`res/raw/shader_calendar.rc`)
 * 5. `clock.rc` (`res/raw/clock.rc`)
 */
@Suppress("RestrictedApiAndroidX")
fun demoEmbeddedRcDocs(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(
            width = 1000,
            height = 1400,
            contentDescription = "Embedded RC Documents Demo",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                column(
                    Modifier.fillMaxSize().background(0xFF0F172A.toInt()).padding(16),
                    horizontal = ColumnLayout.CENTER,
                    vertical = ColumnLayout.TOP,
                ) {
                    // Header banner
                    box(
                        Modifier.fillMaxWidth()
                            .height(64)
                            .clip(RoundedRectShape(12f, 12f, 12f, 12f))
                            .background(0xFF1E293B.toInt())
                            .padding(12),
                        horizontal = BoxLayout.CENTER,
                        vertical = BoxLayout.CENTER,
                    ) {
                        text(
                            "Embedded RC Docs (5 from res/raw)",
                            fontSize = 30f,
                            fontWeight = 700f,
                            color = Color.WHITE,
                        )
                    }

                    // Row 1: cube32.rc (cube3d.rc) & clock.rc
                    row(
                        Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 12, 0, 0),
                        horizontal = RowLayout.SPACE_EVENLY,
                        vertical = RowLayout.CENTER,
                    ) {
                        // 1. cube32.rc
                        column(
                            Modifier.horizontalWeight(1f)
                                .fillMaxHeight()
                                .padding(0, 0, 6, 0)
                                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                .background(0xFF1E293B.toInt())
                                .padding(10),
                            horizontal = ColumnLayout.CENTER,
                        ) {
                            text(
                                "cube32.rc (3D Cube)",
                                fontSize = 22f,
                                fontWeight = 600f,
                                color = 0xFF93C5FD.toInt(),
                            )
                            box(
                                Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 6, 0, 0),
                                horizontal = BoxLayout.CENTER,
                                vertical = BoxLayout.CENTER,
                            ) {
                                rcDoc("cube32.rc", Modifier.fillMaxSize())
                            }
                        }

                        // 2. clock.rc
                        column(
                            Modifier.horizontalWeight(1f)
                                .fillMaxHeight()
                                .padding(6, 0, 0, 0)
                                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                .background(0xFF1E293B.toInt())
                                .padding(10),
                            horizontal = ColumnLayout.CENTER,
                        ) {
                            text(
                                "clock.rc (Live Clock)",
                                fontSize = 22f,
                                fontWeight = 600f,
                                color = 0xFF86EFAC.toInt(),
                            )
                            box(
                                Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 6, 0, 0),
                                horizontal = BoxLayout.CENTER,
                                vertical = BoxLayout.CENTER,
                            ) {
                                rcDoc("clock.rc", Modifier.fillMaxSize())
                            }
                        }
                    }

                    // Row 2: flappy.rc & stop_notches_even.rc
                    row(
                        Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 12, 0, 0),
                        horizontal = RowLayout.SPACE_EVENLY,
                        vertical = RowLayout.CENTER,
                    ) {
                        // 3. flappy.rc
                        column(
                            Modifier.horizontalWeight(1f)
                                .fillMaxHeight()
                                .padding(0, 0, 6, 0)
                                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                .background(0xFF1E293B.toInt())
                                .padding(10),
                            horizontal = ColumnLayout.CENTER,
                        ) {
                            text(
                                "flappy.rc (Tap to Fly)",
                                fontSize = 22f,
                                fontWeight = 600f,
                                color = 0xFFFDE047.toInt(),
                            )
                            box(
                                Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 6, 0, 0),
                                horizontal = BoxLayout.CENTER,
                                vertical = BoxLayout.CENTER,
                            ) {
                                rcDoc("flappy.rc", Modifier.fillMaxSize())
                            }
                        }

                        // 4. stop_notches_even.rc
                        column(
                            Modifier.horizontalWeight(1f)
                                .fillMaxHeight()
                                .padding(6, 0, 0, 0)
                                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                .background(0xFF1E293B.toInt())
                                .padding(10),
                            horizontal = ColumnLayout.CENTER,
                        ) {
                            text(
                                "stop_notches_even.rc (Touch Dial)",
                                fontSize = 22f,
                                fontWeight = 600f,
                                color = 0xFFFCA5A5.toInt(),
                            )
                            box(
                                Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 6, 0, 0),
                                horizontal = BoxLayout.CENTER,
                                vertical = BoxLayout.CENTER,
                            ) {
                                rcDoc("stop_notches_even.rc", Modifier.fillMaxSize())
                            }
                        }
                    }

                    // Row 3: shader_calendar.rc
                    column(
                        Modifier.fillMaxWidth()
                            .verticalWeight(1f)
                            .padding(0, 12, 0, 0)
                            .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                            .background(0xFF1E293B.toInt())
                            .padding(10),
                        horizontal = ColumnLayout.CENTER,
                    ) {
                        text(
                            "shader_calendar.rc (Shader Calendar)",
                            fontSize = 22f,
                            fontWeight = 600f,
                            color = 0xFFD8B4FE.toInt(),
                        )
                        box(
                            Modifier.fillMaxWidth().verticalWeight(1f).padding(0, 6, 0, 0),
                            horizontal = BoxLayout.CENTER,
                            vertical = BoxLayout.CENTER,
                        ) {
                            rcDoc("shader_calendar.rc", Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        .writer
}
