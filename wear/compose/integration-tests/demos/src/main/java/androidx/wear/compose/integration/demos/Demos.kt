/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Text

val Info = DemoCategory(
    "App Info",
    listOf(
        ComposableDemo("App Version") {
            val version =
                @Suppress("DEPRECATION")
                LocalContext.current.packageManager
                    .getPackageInfo(LocalContext.current.packageName, 0).versionName

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Version: $version",
                    textAlign = TextAlign.Center,
                )
            }
        }
    ),
)

/**
 * [DemoCategory] containing all the top level demo categories.
 */
@ExperimentalWearMaterialApi
val WearComposeDemos = DemoCategory(
    "Wear Compose Demos",
    listOf(
        WearFoundationDemos,
        WearMaterialDemos,
        Info
    )
)
