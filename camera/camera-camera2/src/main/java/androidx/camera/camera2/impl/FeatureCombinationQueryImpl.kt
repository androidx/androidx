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

package androidx.camera.camera2.impl

import androidx.annotation.RequiresApi
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.ConfigQueryResult
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.impl.SessionConfig
import kotlinx.coroutines.runBlocking

// TODO: b/417839748 - Decide on the appropriate API level for CameraX feature combo API
@RequiresApi(35)
internal class FeatureCombinationQueryImpl(
    private val cameraMetadata: CameraMetadata,
    private val cameraPipe: CameraPipe,
    private val cameraQuirks: CameraQuirks,
) : FeatureCombinationQuery {
    override fun isSupported(sessionConfig: SessionConfig): Boolean {
        val configProvider =
            CameraGraphConfigProvider(
                callbackMap = CameraCallbackMap(),
                requestListener = ComboRequestListener(),
                cameraConfig = CameraConfig(cameraMetadata.camera),
                cameraQuirks = cameraQuirks,
                zslControl = ZslControlNoOpImpl(), // TODO: b/400835309 - Handle ZSL properly
                templateParamsOverride = TemplateParamsQuirkOverride(cameraQuirks.quirks),
                cameraMetadata = cameraMetadata,
            )

        val graphConfigBundle =
            configProvider.create(
                operatingMode = CameraGraph.OperatingMode.NORMAL,
                sessionConfig = sessionConfig,
                setOutputType = true,
            )

        return runBlocking {
            cameraPipe.isConfigSupported(graphConfigBundle.graphConfig).apply {
                Camera2Logger.debug {
                    val streamsLog =
                        graphConfigBundle.graphConfig.streams.map { cameraStream ->
                            cameraStream.outputs.map {
                                "size=${it.size}, format=${it.format}," +
                                    " dynamicRangeProfile${it.dynamicRangeProfile}"
                            }
                        }

                    "FeatureCombinationQueryImpl#isSupported: result = $this for " +
                        "sessionParameters = ${graphConfigBundle.graphConfig.sessionParameters} " +
                        "and streams = $streamsLog"
                }
            } == ConfigQueryResult.SUPPORTED
        }
    }
}
