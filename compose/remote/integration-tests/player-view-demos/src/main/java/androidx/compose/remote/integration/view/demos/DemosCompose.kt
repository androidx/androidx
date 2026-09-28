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

import android.content.Context
import androidx.compose.remote.integration.view.demos.blog.AnimatedRadius
import androidx.compose.remote.integration.view.demos.blog.CollisionDetect
import androidx.compose.remote.integration.view.demos.blog.DiscoBall
import androidx.compose.remote.integration.view.demos.blog.ParticleBreakup
import androidx.compose.remote.integration.view.demos.blog.TouchBounce
import androidx.compose.remote.integration.view.demos.examples.RcSimpleClock1
import androidx.compose.remote.integration.view.demos.examples.ScrollViewDemo
import androidx.compose.remote.integration.view.demos.examples.SimplePath
import androidx.compose.remote.integration.view.demos.examples.StateLayoutRowToColumnDemo
import androidx.compose.remote.integration.view.demos.examples.StateLayoutToggleDemo
import androidx.compose.remote.integration.view.demos.examples.SwitchWidgetDemo
import androidx.compose.remote.integration.view.demos.examples.WeatherDemo
import androidx.compose.remote.integration.view.demos.examples.demoJson1
import androidx.compose.remote.integration.view.demos.examples.shaderFireworks
import androidx.compose.remote.integration.view.demos.utils.RCDoc

fun getRemoteComposable(context: Context, prefix: String = ""): ArrayList<RCDoc> {
    return arrayListOf(
        getComposeDoc(context, prefix + "Blog/AnimatedRadius") { AnimatedRadius() },
        getComposeDoc(context, prefix + "Blog/CollisionDetect") { CollisionDetect() },
        getComposeDoc(context, prefix + "Blog/DiscoBall") { DiscoBall() },
        getComposeDoc(context, prefix + "Blog/ParticleBreakup") { ParticleBreakup() },
        getComposeDoc(context, prefix + "Blog/TouchBounce") { TouchBounce() },
        getComposeDoc(context, prefix + "Compose/JSON") { demoJson1() },
        getComposeDoc(context, prefix + "Compose/Fireworks") { shaderFireworks() },
        getComposeDoc(context, prefix + "Compose/SimplePath") { SimplePath() },
        getComposeDoc(context, prefix + "Compose/WeatherDemo") { WeatherDemo() },
        getComposeDoc(context, prefix + "Compose/Simple Clock") { RcSimpleClock1() },
        getComposeDoc(context, prefix + "Compose/Switch Widget") { SwitchWidgetDemo() },
        getComposeDoc(context, prefix + "Compose/StateLayout Toggle") { StateLayoutToggleDemo() },
        getComposeDoc(context, prefix + "Compose/StateLayout Row to Column") {
            StateLayoutRowToColumnDemo()
        },
        getComposeDoc(context, "Compose/Calendar") { ScrollViewDemo() },
    )
}
