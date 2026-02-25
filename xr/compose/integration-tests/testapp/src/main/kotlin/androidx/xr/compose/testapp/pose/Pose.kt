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

package androidx.xr.compose.testapp.pose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SceneCoreEntity
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.sizeIn
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import java.nio.file.Paths

/**
 * A test app to validate the correctness of pose accumulation (multiplication).
 *
 * This app builds multi-leveled hierarchy with nested and chained rotations to test hierarchical
 * pose accumulation.
 *
 * It displays "xyzArrow" models in a row to show the transformations step-by-step:
 * 1. **Case 1:** Initial state (no rotation).
 * 2. **Case 2:** After Level 1 rotation (90-deg X).
 * 3. **Case 3:** Nested pose: (90-deg X) * Level 2 (90-deg Y).
 * 4. **Case 4:** Nested and chained pose: Level 1 (90-deg X) * Level 2 (chained YZ 90-deg).
 * 5. **Case 5:** Triple nested pose: Level 1 (90-deg X) * Level 2 (90-deg Y) * Level 3 (90-deg Z).
 *
 * This test should pass with the correct `Parent.compose(Child)` logic.
 */
class Pose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rotX90 = Quaternion.fromAxisAngle(Vector3.Right, 90f)
        val rotY90 = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        val rotZ90 = Quaternion.fromAxisAngle(Vector3.Backward, 90f)

        setContent {
            IntegrationTestsAppTheme {
                Subspace(
                    modifier =
                        SubspaceModifier.sizeIn(
                            maxWidth = 20.meters.toDp(),
                            maxHeight = 20.meters.toDp(),
                        )
                ) {
                    SpatialRow(
                        modifier =
                            SubspaceModifier.offset(
                                y = (-0.5).meters.toDp(),
                                z = (-5.0).meters.toDp(),
                            ),
                        horizontalArrangement = SpatialArrangement.spacedBy(0.1.meters.toDp()),
                    ) {
                        // Case 1: Initial State
                        //
                        // EXPECTED:
                        //     Green arrow: +Y (Up)
                        //     Blue arrow: +Z (Backward)
                        //     Red arrow: +X (Right)
                        SpatialColumn(horizontalAlignment = SpatialAlignment.CenterHorizontally) {
                            LabelPanel(
                                "Case 1:\nInitial State\n\n" +
                                    "EXPECTED:\n" +
                                    "    Green: +Y (Up)\n" +
                                    "    Blue: +Z (Backward)\n" +
                                    "    Red: +X (Right)"
                            )
                            StaticXyzArrow()
                        }

                        // Case 2: After Level 1 (X-Rot)
                        //
                        // EXPECTED:
                        //     Green: +Z (Backward)
                        //     Blue: -Y (Down)
                        //     Red: +X (Right)
                        SpatialColumn(horizontalAlignment = SpatialAlignment.CenterHorizontally) {
                            LabelPanel(
                                "Case 2:\nParent Rot\n" +
                                    "= rotX90°\n\n" +
                                    "EXPECTED:\n" +
                                    "    Green: +Z (Backward)\n" +
                                    "    Blue: -Y (Down)\n" +
                                    "    Red: +X (Right)"
                            )
                            SpatialBox(modifier = SubspaceModifier.rotate(rotX90)) {
                                StaticXyzArrow()
                            }
                        }

                        // Case 3: After Level 1 * Level 2 (X-Rot * Y-Rot)
                        // Level 1 Pose = rotX90
                        // Level 2 Pose = rotY90
                        //
                        // CORRECT Math: R_Final = Level 1 * Level 2 = rotX90 * rotY90
                        //   (Apply Y, then X)
                        //
                        // EXPECTED:
                        //     Green: +Z (Backward)
                        //     Blue: +X (Right)
                        //     Red: +Y (Up)
                        SpatialColumn(horizontalAlignment = SpatialAlignment.CenterHorizontally) {
                            LabelPanel(
                                "Case 3:\nGrand Parent Rot * Parent Rot\n" +
                                    "= rotX90 * rotY90\n\n" +
                                    "EXPECTED:\n" +
                                    "    Green: +Z (Backward)\n" +
                                    "    Blue: +X (Right)\n" +
                                    "    Red: +Y (Up)"
                            )
                            SpatialBox(modifier = SubspaceModifier.rotate(rotX90)) {
                                SpatialBox(modifier = SubspaceModifier.rotate(rotY90)) {
                                    StaticXyzArrow()
                                }
                            }
                        }

                        // --- Case 4: Combined Pose (Level 1 * Level 2-Chained) ---
                        // Level 1 Pose = rotX90
                        // Level 2 Pose = rotY90.compose(rotZ90) (Inner modifier * Outer modifier)
                        //
                        // CORRECT Math: R_Final = Level 1 * Level 2 = rotX90 * (rotY90 * rotZ90)
                        //   (Apply Z, then Y, then X)
                        //
                        // EXPECTED:
                        //     Green: -Y (Down)
                        //     Blue: +X (Right)
                        //     Red: +Z (Backward)
                        SpatialColumn(horizontalAlignment = SpatialAlignment.CenterHorizontally) {
                            LabelPanel(
                                "Case 4:\nGrand Parent Rot *\n" +
                                    "Parent Inner Rot Y *\n" +
                                    "Parent Outer Rot Z\n" +
                                    "= rotX90 * (rotY90 * rotZ90)\n\n" +
                                    "EXPECTED:\n" +
                                    "    Green: -Y (Down)\n" +
                                    "    Blue: +X (Right)\n" +
                                    "    Red: +Z (Backward)"
                            )
                            SpatialBox(modifier = SubspaceModifier.rotate(rotX90)) {
                                SpatialBox(
                                    modifier =
                                        SubspaceModifier.rotate(rotY90) // Inner modifier
                                            .rotate(rotZ90) // Outer modifier
                                ) {
                                    StaticXyzArrow()
                                }
                            }
                        }

                        // --- Case 5: Combined Pose (Level 1 * Level 2 * Level 3) ---
                        // Level 1 Pose = rotX90
                        // Level 2 Pose = rotY90
                        // Level 3 Pose = rotZ90
                        //
                        // CORRECT Math: R_Final
                        // = Level 1 * Level 2 * Level 3
                        // = rotX90 * rotY90 * rotZ90  (Apply Z, then Y, then X)
                        //
                        // EXPECTED:
                        //     Green: -Y (Down)
                        //     Blue: +X (Right)
                        //     Red: +Z (Backward)
                        SpatialColumn(horizontalAlignment = SpatialAlignment.CenterHorizontally) {
                            LabelPanel(
                                "Case 5:\nGrand Grand Parent Rot *\n" +
                                    "Grand Parent Rot Y *\n" +
                                    "Parent Rot Z\n" +
                                    "= rotX90 * rotY90 * rotZ90\n\n" +
                                    "EXPECTED:\n" +
                                    "    Green: -Y (Down)\n" +
                                    "    Red: +Z (Backward)\n" +
                                    "    Blue: +X (Right)"
                            )
                            SpatialBox(modifier = SubspaceModifier.rotate(rotX90)) {
                                SpatialBox(modifier = SubspaceModifier.rotate(rotY90)) {
                                    SpatialBox(modifier = SubspaceModifier.rotate(rotZ90)) {
                                        StaticXyzArrow()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A simple [SpatialPanel] used for text labels above the test arrows. */
@Composable
@SubspaceComposable
fun LabelPanel(text: String) {
    SpatialPanel(
        modifier =
            SubspaceModifier.width(2.3.meters.toDp())
                .height(1.8.meters.toDp())
                .offset(y = 1.0.meters.toDp())
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.DarkGray.copy(alpha = 0.8f)),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 120.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(all = 48.dp),
                lineHeight = 130.sp,
            )
        }
    }
}

/**
 * A composable that loads the "xyzArrows.glb" model from the assets and renders it as a
 * [SceneCoreEntity].
 *
 * This is a static asset; it has no internal animation. It only shows the orientation resulting
 * from the [SubspaceModifier] passed to it.
 */
@Composable
@SubspaceComposable
fun StaticXyzArrow(modifier: SubspaceModifier = SubspaceModifier) {
    val session = LocalSession.current ?: return
    var gltfModel by remember { mutableStateOf<GltfModel?>(null) }
    val modelSize = 0.5.meters.toDp()

    // Load the model just once
    LaunchedEffect(Unit) {
        gltfModel = GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
    }

    if (gltfModel != null) {
        SceneCoreEntity(
            factory = { GltfModelEntity.create(session, gltfModel!!) },
            // Apply only the incoming modifier from the parent hierarchy.
            // Also set a default size for visibility.
            modifier = modifier.size(modelSize),
        )
    }
}
