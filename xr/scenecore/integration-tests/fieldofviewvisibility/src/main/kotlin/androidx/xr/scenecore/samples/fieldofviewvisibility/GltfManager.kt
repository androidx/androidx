/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.samples.fieldofviewvisibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import java.nio.file.Paths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Manages the UI for the GLTF entity. */
@Suppress("Deprecation")
// TODO - b/421386891: is/setHidden is deprecated; this activity needs to be updated to use
class GltfManager(private val session: Session, private val coroutineScope: CoroutineScope) {
    private val mSession: Session
    private var mGltfModel: GltfModel? by mutableStateOf(null)
    private var mGltfModelEntity: GltfModelEntity? by mutableStateOf(null)

    init {
        mSession = session
    }

    @Composable
    fun GltfEntitySettings() {
        var modelIsHidden by remember { mutableStateOf(false) }

        Column() {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // TODO: b/355119589 - Add alpha slider once GLTF entity supports alpha.
                Button(
                    enabled = (mGltfModelEntity != null),
                    onClick = {
                        if (mGltfModelEntity != null) {
                            modelIsHidden = mGltfModelEntity!!.isHidden(true)
                            mGltfModelEntity?.setHidden(!modelIsHidden)
                            modelIsHidden = !modelIsHidden
                        }
                    },
                ) {
                    Text(
                        text = (if (modelIsHidden) "Show Model" else "Hide Model"),
                        fontSize = 20.sp,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = (mGltfModel == null),
                    onClick = {
                        coroutineScope.launch {
                            mGltfModel =
                                GltfModel.create(
                                    session,
                                    Paths.get("models", "Dragon_Evolved.gltf"),
                                )
                        }
                    },
                ) {
                    Text(text = "Load GLTF Entity Model", fontSize = 20.sp)
                }
                Button(
                    enabled = (mGltfModelEntity == null && mGltfModel != null),
                    onClick = {
                        mGltfModelEntity =
                            GltfModelEntity.create(
                                mSession,
                                mGltfModel!!,
                                Pose(Vector3.Forward * 3f + Vector3.Right),
                            )
                    },
                ) {
                    Text(text = "Create GLTF Entity", fontSize = 20.sp)
                }
                Button(
                    enabled = (mGltfModelEntity != null),
                    onClick = {
                        mGltfModelEntity?.dispose()
                        mGltfModelEntity = null
                    },
                ) {
                    Text(text = "Destroy GLTF Entity", fontSize = 20.sp)
                }
            }
        }
    }
}
