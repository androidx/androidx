/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey

object AlwaysOnDisplay {
    object Elements {
        val Background = ElementKey("AlwaysOnDisplayBackground")
    }
}

@Composable
fun ContentScope.AlwaysOnDisplay(modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(
            Modifier.element(AlwaysOnDisplay.Elements.Background)
                .fillMaxSize()
                .background(Color.Black)
        )

        Column(Modifier.padding(16.dp)) {
            Clock(Color.White, Modifier.padding(top = 16.dp))
            SmartSpace(Color.White)
        }
    }
}
