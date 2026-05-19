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
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.xr.arcore.projected.testapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@OptIn(PreviewSpatialApi::class)
class ConfigProjectedGeospatialActivity : ComponentActivity() {

    private val test1Result = mutableStateOf("Pending...")
    private val test2Result = mutableStateOf("Pending...")
    private lateinit var targetMode: GeospatialMode
    private var modeName: String = "SPATIAL"

    @OptIn(ExperimentalProjectedApi::class)
    private val requestPermissionLauncher:
        ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            tryCreateAndConfigureSession()
        }

    @OptIn(ExperimentalProjectedApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modeName = intent.getStringExtra("GEOSPATIAL_MODE") ?: "SPATIAL"
        targetMode = if (modeName == "INERTIAL") GeospatialMode.INERTIAL else GeospatialMode.SPATIAL

        val permissionsRequired =
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        val hasAllPermissions =
            permissionsRequired.all {
                androidx.core.content.ContextCompat.checkSelfPermission(this, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }

        if (hasAllPermissions) {

            tryCreateAndConfigureSession()
        } else {

            val params =
                ProjectedPermissionsRequestParams(
                    permissions = permissionsRequired,
                    rationale = "Location permissions are required in projected mode.",
                )
            requestPermissionLauncher.launch(listOf(params))
        }

        setContent {
            Column(
                modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    "Activity: ConfigProjectedGeospatialActivity",
                    color = Color.White,
                    fontSize = 16.sp,
                )
                Text("Target Mode: $modeName", color = Color.White, fontSize = 16.sp)
                Text(
                    "TEST 1 (session.configure): ${test1Result.value}",
                    color = Color.LightGray,
                    fontSize = 16.sp,
                )
                Text(
                    "TEST 2 (Internal Runtime Logic): ${test2Result.value}",
                    color = Color.LightGray,
                    fontSize = 16.sp,
                )
            }
        }
    }

    private fun tryCreateAndConfigureSession() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (
                    val result =
                        Session.create(
                            context = this@ConfigProjectedGeospatialActivity,
                            lifecycleOwner = this@ConfigProjectedGeospatialActivity,
                        )
                ) {
                    is SessionCreateSuccess -> {
                        val session = result.session

                        // TEST 1 (Config Plumbing)
                        val config =
                            Config.Builder()
                                .setGeospatial(targetMode)
                                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                                .build()
                        try {
                            session.configure(config)
                            test1Result.value = "Success"
                        } catch (e: UnsupportedOperationException) {
                            test1Result.value = "Thrown (UnsupportedOperationException)"
                        } catch (e: Exception) {
                            test1Result.value = "Configuration Error: ${e.message}"
                        }

                        // TEST 2 (Internal Runtime Logic)
                        // TODO: b/510879776 - Update this code once GeospatialMode.INERTIAL is out
                        // in ARCore 1.55
                        try {
                            var foundRes: Boolean? = null
                            for (runtime in session.runtimes) {
                                try {
                                    val method =
                                        runtime.javaClass.getDeclaredMethod(
                                            "isGeoSpatialModeSupportedInArCore1x",
                                            GeospatialMode::class.java,
                                        )
                                    method.isAccessible = true
                                    @Suppress("BanUncheckedReflection")
                                    foundRes = method.invoke(runtime, targetMode) as Boolean
                                    break
                                } catch (e: NoSuchMethodException) {
                                    // continue
                                }
                            }
                            test2Result.value =
                                foundRes?.toString()?.replaceFirstChar { it.uppercase() } ?: "False"
                        } catch (e: Exception) {
                            test2Result.value = "False"
                        }
                    }
                    else -> {
                        test1Result.value = "Session creation failed"
                        test2Result.value = "False"
                    }
                }
            } catch (e: Exception) {
                test1Result.value = "Exception: ${e.message}"
                test2Result.value = "False"
            }
        }
    }
}
