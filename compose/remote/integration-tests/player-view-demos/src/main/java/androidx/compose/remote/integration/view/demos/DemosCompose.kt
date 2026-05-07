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

package androidx.compose.remote.integration.view.demos

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.remote.integration.view.demos.examples.RcSimpleClock1
import androidx.compose.remote.integration.view.demos.examples.ScrollViewDemo
import androidx.compose.remote.integration.view.demos.examples.SimplePath
import androidx.compose.remote.integration.view.demos.examples.SwitchWidgetDemo
import androidx.compose.remote.integration.view.demos.examples.WeatherDemo
import androidx.compose.remote.integration.view.demos.examples.shaderFireworks
import androidx.compose.remote.integration.view.demos.utils.RCDoc

@SuppressLint("RestrictedApiAndroidX")
fun getRemoteComposable(context: Context): ArrayList<RCDoc> {
    return arrayListOf(
        getComposeDoc(context, "Compose/Fireworks") { shaderFireworks() },
        getComposeDoc(context, "Compose/SimplePath") { SimplePath() },
        getComposeDoc(context, "Compose/WeatherDemo") { WeatherDemo() },
        getComposeDoc(context, "Compose/Simple Clock") { RcSimpleClock1() },
        getComposeDoc(context, "Compose/Switch Widget") { SwitchWidgetDemo() },
        getComposeDoc(context, "Compose/Calendar") { ScrollViewDemo() },
    )
}
