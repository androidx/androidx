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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.state.toRemoteDp
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Grid screenshot tests exercising [remoteComponent] scoping and coordinate edge cases on the
 * embedded player (`RcPlayer`), rendering resolved dimensions both visually and textually.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteComponentScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI(itemsPerRow = 3)

    @Test
    fun edgeCasesGrid() = composeTestRule.runScreenshotTest {
        gridScreenshotUI.GridContent(remoteComponentEdgeCaseGridCells())
    }
}

private fun dpStr(px: RemoteFloat): RemoteString =
    (px.toRemoteDp().value + 0.5f).toRemoteInt().toRemoteString()

internal fun remoteComponentEdgeCaseGridCells():
    List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
    listOf(
        "1. Sibling trap" to @RemoteComposable @Composable { PrecedingSiblingTrapCell() },
        "2. 3-level nest" to @RemoteComposable @Composable { ThreeLevelNestingCell() },
        "3. Lazy canvas" to @RemoteComposable @Composable { LazyCanvasReadCell() },
        "4. Center XY" to @RemoteComposable @Composable { CenterCrosshairCell() },
        "5. xy vs rootXY" to @RemoteComposable @Composable { LocalVsRootCoordsCell() },
        "6. CSE 2-boxes" to @RemoteComposable @Composable { DistinctSiblingCacheKeyCell() },
        "7. Late tall sib" to @RemoteComposable @Composable { WrapContentLateSiblingCell() },
        "8. Padded box" to @RemoteComposable @Composable { PaddedContainerCell() },
        "9. Named expr" to @RemoteComposable @Composable { NamedExpressionSiblingCell() },
    )

/**
 * Edge case 1: A small preceding sibling (`24x16`) before `remoteComponent()` must not hijack the
 * parent `100x100` container's width or height.
 */
@Composable
@RemoteComposable
private fun PrecedingSiblingTrapCell() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color(0xFF37474F).rc)) {
        RemoteBox(modifier = RemoteModifier.size(24.rdp, 16.rdp).background(Color(0xFFE53935).rc))
        val parent = remoteComponent()
        RemoteBox(
            modifier =
                RemoteModifier.width((parent.width * 0.5f).toRemoteDp())
                    .height((parent.height * 0.5f).toRemoteDp())
                    .background(Color(0xFF43A047).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                dpStr(parent.width * 0.5f) + "x".rs + dpStr(parent.height * 0.5f),
                fontSize = 8.rsp,
                color = Color.White.rc,
            )
        }
        RemoteText(
            "P:".rs + dpStr(parent.width) + "x".rs + dpStr(parent.height),
            fontSize = 8.rsp,
            color = Color(0xFF1B1B1B).rc,
        )
    }
}

/**
 * Edge case 2: Three nested containers (`100`, `84`, `64` wide), each with a preceding decoy
 * sibling, all read inside the innermost container as proportional gauge bars + text.
 */
@Composable
@RemoteComposable
private fun ThreeLevelNestingCell() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color(0xFF263238).rc)) {
        RemoteBox(modifier = RemoteModifier.size(16.rdp, 6.rdp).background(Color(0xFFE53935).rc))
        val outer = remoteComponent()
        RemoteColumn(
            modifier =
                RemoteModifier.size(84.rdp, 90.rdp)
                    .background(Color(0xFFE3F2FD).rc)
                    .border(1.rdp, Color(0xFF1565C0).rc)
        ) {
            RemoteBox(
                modifier = RemoteModifier.size(12.rdp, 6.rdp).background(Color(0xFFE53935).rc)
            )
            val middle = remoteComponent()
            RemoteColumn(
                modifier =
                    RemoteModifier.size(64.rdp, 80.rdp)
                        .background(Color(0xFFFFF8E1).rc)
                        .border(1.rdp, Color(0xFFEF6C00).rc)
            ) {
                RemoteBox(
                    modifier = RemoteModifier.size(10.rdp, 4.rdp).background(Color(0xFFE53935).rc)
                )
                val inner = remoteComponent()
                RemoteBox(
                    modifier =
                        RemoteModifier.width((outer.width * 0.5f).toRemoteDp())
                            .height(10.rdp)
                            .background(Color(0xFF1565C0).rc)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.width((middle.width * 0.5f).toRemoteDp())
                            .height(10.rdp)
                            .background(Color(0xFF00897B).rc)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.width((inner.width * 0.5f).toRemoteDp())
                            .height(10.rdp)
                            .background(Color(0xFFEF6C00).rc)
                )
                RemoteText(
                    dpStr(outer.width) + "/".rs + dpStr(middle.width) + "/".rs + dpStr(inner.width),
                    fontSize = 8.rsp,
                    color = Color(0xFF1B1B1B).rc,
                )
            }
        }
    }
}

/**
 * Edge case 3: `remoteComponent()` captured in `RemoteBox` without touching properties during
 * composition, then read lazily inside a smaller child [RemoteCanvas] alongside `remote.component`.
 */
@Composable
@RemoteComposable
private fun LazyCanvasReadCell() {
    RemoteBox(modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color(0xFF37474F).rc)) {
        val lazyOuter = remoteComponent()
        RemoteBox(modifier = RemoteModifier.size(20.rdp, 12.rdp).background(Color(0xFFE53935).rc))
        RemoteColumn(modifier = RemoteModifier.padding(top = 14.rdp)) {
            RemoteCanvas(
                modifier =
                    RemoteModifier.size(72.rdp, 56.rdp)
                        .background(Color(0xFFECEFF1).rc)
                        .border(1.rdp, Color(0xFF455A64).rc)
            ) {
                val outerBarPaint = RemotePaint { color = Color(0xFF3949AB).rc }
                val canvasDiagPaint = RemotePaint {
                    color = Color(0xFFD81B60).rc
                    strokeWidth = 3f.rf
                    style = RemotePaintingStyle.Stroke
                }
                // Half of outer 100dp = 50dp wide bar inside 72dp canvas.
                drawRect(
                    paint = outerBarPaint,
                    topLeft = RemoteOffset(0f.rf, 0f.rf),
                    size = RemoteSize(lazyOuter.width * 0.5f, lazyOuter.height * 0.25f),
                )
                // Diagonal across the 72x56 canvas itself via remote.component.
                drawLine(
                    paint = canvasDiagPaint,
                    start = RemoteOffset(0f.rf, 0f.rf),
                    end = RemoteOffset(remote.component.width, remote.component.height),
                )
            }
            RemoteText(
                "O:".rs + dpStr(lazyOuter.width) + "x".rs + dpStr(lazyOuter.height),
                fontSize = 8.rsp,
                color = Color(0xFF1B1B1B).rc,
            )
        }
    }
}

/**
 * Edge case 4: `centerX` and `centerY` align a `20x20` marker onto a canvas crosshair inside a
 * `96x80` box even after an asymmetric decoy sibling.
 */
@Composable
@RemoteComposable
private fun CenterCrosshairCell() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteBox(
            modifier =
                RemoteModifier.size(96.rdp, 80.rdp)
                    .background(Color(0xFFF3E5F5).rc)
                    .border(1.rdp, Color(0xFF6A1B9A).rc)
        ) {
            RemoteBox(
                modifier = RemoteModifier.size(16.rdp, 10.rdp).background(Color(0xFFE53935).rc)
            )
            val box = remoteComponent()
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                val crossPaint = RemotePaint {
                    color = Color(0xFF8E24AA).rc
                    strokeWidth = 2f.rf
                }
                drawLine(
                    paint = crossPaint,
                    start = RemoteOffset(box.centerX, 0f.rf),
                    end = RemoteOffset(box.centerX, box.height),
                )
                drawLine(
                    paint = crossPaint,
                    start = RemoteOffset(0f.rf, box.centerY),
                    end = RemoteOffset(box.width, box.centerY),
                )
            }
            RemoteBox(
                modifier =
                    RemoteModifier.offset(
                            x = box.centerX.toRemoteDp() - 8.rdp,
                            y = box.centerY.toRemoteDp() - 8.rdp,
                        )
                        .size(16.rdp, 16.rdp)
                        .background(Color(0xFF00ACC1).rc)
            )
            RemoteBox(modifier = RemoteModifier.padding(start = 2.rdp, top = 64.rdp)) {
                RemoteText(
                    "c=".rs + dpStr(box.centerX) + ",".rs + dpStr(box.centerY),
                    fontSize = 8.rsp,
                    color = Color(0xFF1B1B1B).rc,
                )
            }
        }
    }
}

/**
 * Edge case 5: Nested offsets distinguishing parent-relative `positionInParent` from root-relative
 * `positionInRoot` inside a grid cell.
 */
@Composable
@RemoteComposable
private fun LocalVsRootCoordsCell() {
    RemoteBox(
        modifier =
            RemoteModifier.fillMaxSize()
                .border(1.rdp, Color(0xFF37474F).rc)
                .padding(start = 10.rdp, top = 12.rdp)
    ) {
        RemoteBox(
            modifier =
                RemoteModifier.size(86.rdp, 84.rdp)
                    .background(Color(0xFFE8F5E9).rc)
                    .border(1.rdp, Color(0xFF2E7D32).rc),
            contentAlignment = RemoteAlignment.BottomEnd,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(66.rdp, 58.rdp)
                        .background(Color(0xFFC8E6C9).rc)
                        .border(1.rdp, Color(0xFF1B5E20).rc)
            ) {
                val target = remoteComponent()
                val pos = target.positionInParent
                val root = target.positionInRoot
                RemoteColumn(modifier = RemoteModifier.padding(2.rdp)) {
                    RemoteText(
                        "xy:".rs + dpStr(pos.x) + ",".rs + dpStr(pos.y),
                        fontSize = 7.rsp,
                        color = Color(0xFF1B1B1B).rc,
                    )
                    RemoteText(
                        "rt:".rs + dpStr(root.x) + ",".rs + dpStr(root.y),
                        fontSize = 7.rsp,
                        color = Color(0xFF0D47A1).rc,
                    )
                    RemoteText(
                        "sz:".rs + dpStr(target.width) + "x".rs + dpStr(target.height),
                        fontSize = 7.rsp,
                        color = Color(0xFF1B5E20).rc,
                    )
                }
            }
        }
    }
}

/**
 * Edge case 6: Two sibling containers of different sizes (`96x40` and `52x54`) evaluating the exact
 * same `(remoteComponent().width * 0.5f)` expression must not collide in the CSE cache.
 */
@Composable
@RemoteComposable
private fun DistinctSiblingCacheKeyCell() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteBox(
            modifier =
                RemoteModifier.size(96.rdp, 42.rdp)
                    .background(Color(0xFFE1F5FE).rc)
                    .border(1.rdp, Color(0xFF0277BD).rc)
        ) {
            val top = remoteComponent()
            RemoteBox(
                modifier =
                    RemoteModifier.width((top.width * 0.5f).toRemoteDp())
                        .height((top.height * 0.5f).toRemoteDp())
                        .background(Color(0xFF0288D1).rc)
            )
            RemoteBox(modifier = RemoteModifier.padding(start = 2.rdp, top = 24.rdp)) {
                RemoteText(
                    "A:".rs + dpStr(top.width) + "x".rs + dpStr(top.height),
                    fontSize = 8.rsp,
                    color = Color(0xFF01579B).rc,
                )
            }
        }
        RemoteBox(modifier = RemoteModifier.height(4.rdp))
        RemoteBox(
            modifier =
                RemoteModifier.size(52.rdp, 52.rdp)
                    .background(Color(0xFFFFF3E0).rc)
                    .border(1.rdp, Color(0xFFE65100).rc)
        ) {
            val bottom = remoteComponent()
            RemoteBox(
                modifier =
                    RemoteModifier.width((bottom.width * 0.5f).toRemoteDp())
                        .height((bottom.height * 0.5f).toRemoteDp())
                        .background(Color(0xFFF57C00).rc)
            )
            RemoteBox(modifier = RemoteModifier.padding(start = 2.rdp, top = 34.rdp)) {
                RemoteText(
                    "B:".rs + dpStr(bottom.width) + "x".rs + dpStr(bottom.height),
                    fontSize = 8.rsp,
                    color = Color(0xFFBF360C).rc,
                )
            }
        }
    }
}

/**
 * Edge case 7: Wrap-content `RemoteRow` whose height (`68dp`) is determined by a *later* sibling,
 * while an *earlier* sibling sizes itself to `50%` of `row.height` (`34dp`).
 */
@Composable
@RemoteComposable
private fun WrapContentLateSiblingCell() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteRow(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .background(Color(0xFFEDE7F6).rc)
                    .border(1.rdp, Color(0xFF4527A0).rc)
        ) {
            val row = remoteComponent()
            // Earlier sibling depends on row.height before the tall sibling is declared.
            RemoteBox(
                modifier =
                    RemoteModifier.width(36.rdp)
                        .height((row.height * 0.5f).toRemoteDp())
                        .background(Color(0xFF26A69A).rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(dpStr(row.height * 0.5f), fontSize = 8.rsp, color = Color.White.rc)
            }
            RemoteBox(modifier = RemoteModifier.width(6.rdp))
            // Tall sibling that dictates the wrap-content row's height.
            RemoteBox(
                modifier =
                    RemoteModifier.width(36.rdp).height(68.rdp).background(Color(0xFF5E35B1).rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("68".rs, fontSize = 8.rsp, color = Color.White.rc)
            }
        }
        RemoteText("50% of 68 = 34".rs, fontSize = 8.rsp, color = Color(0xFF1B1B1B).rc)
    }
}

/**
 * Edge case 8: Container with `size(92, 84)` followed by `padding(10)` — verifying
 * `remoteComponent().width` / `height` report the outer component dimensions (`92x84`) even after a
 * decoy sibling.
 */
@Composable
@RemoteComposable
private fun PaddedContainerCell() {
    RemoteBox(
        modifier =
            RemoteModifier.size(92.rdp, 84.rdp)
                .background(Color(0xFFFFEBEE).rc)
                .border(1.rdp, Color(0xFFC62828).rc)
                .padding(10.rdp)
    ) {
        RemoteBox(modifier = RemoteModifier.size(14.rdp, 8.rdp).background(Color(0xFFE53935).rc))
        val paddedBox = remoteComponent()
        val innerWidthDp = max(paddedBox.width.toRemoteDp().value - 20f.rf, 0f.rf)
        RemoteColumn(modifier = RemoteModifier.padding(top = 12.rdp)) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(innerWidthDp.asRemoteDp())
                        .height(22.rdp)
                        .background(Color(0xFFEF5350).rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(
                    "in:".rs + (innerWidthDp + 0.5f).toRemoteInt().toRemoteString(),
                    fontSize = 7.rsp,
                    color = Color.White.rc,
                )
            }
            RemoteText(
                "box:".rs + dpStr(paddedBox.width) + "x".rs + dpStr(paddedBox.height),
                fontSize = 8.rsp,
                color = Color(0xFFB71C1C).rc,
            )
        }
    }
}

/**
 * Edge case 9: `rememberNamedRemoteFloat` reading `remoteComponent().width`, `height`, and
 * `centerX` after a decoy sibling.
 */
@Composable
@RemoteComposable
private fun NamedExpressionSiblingCell() {
    RemoteColumn(
        modifier =
            RemoteModifier.size(96.rdp, 88.rdp)
                .background(Color(0xFFE0F2F1).rc)
                .border(1.rdp, Color(0xFF00695C).rc)
    ) {
        RemoteBox(modifier = RemoteModifier.size(20.rdp, 12.rdp).background(Color(0xFFE53935).rc))
        val comp = remoteComponent()
        val namedW = rememberNamedRemoteFloat("cell9_w", comp.width)
        val namedH = rememberNamedRemoteFloat("cell9_h", comp.height)
        val namedCx = rememberNamedRemoteFloat("cell9_cx", comp.centerX)
        RemoteBox(
            modifier =
                RemoteModifier.width((namedW * 0.5f).toRemoteDp())
                    .height((namedH * 0.5f).toRemoteDp())
                    .background(Color(0xFF00897B).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                dpStr(namedW * 0.5f) + "x".rs + dpStr(namedH * 0.5f),
                fontSize = 8.rsp,
                color = Color.White.rc,
            )
        }
        RemoteText(
            dpStr(namedW) + "x".rs + dpStr(namedH) + " c".rs + dpStr(namedCx),
            fontSize = 8.rsp,
            color = Color(0xFF004D40).rc,
        )
    }
}
