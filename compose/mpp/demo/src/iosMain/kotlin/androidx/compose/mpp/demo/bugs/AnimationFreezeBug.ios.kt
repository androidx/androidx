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

package androidx.compose.mpp.demo.bugs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Switch
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication

val AnimationFreezeBug = Screen.Example("AnimationFreezeBug") {
    Column(modifier = Modifier.fillMaxSize()) {
        var state by remember { mutableStateOf(true) }
        Switch(checked = state, onCheckedChange = {
            state = it
            UIApplication.sharedApplication.openURL(
                url = URLWithString("app-settings:")!!,
                options = emptyMap<Any?, Any>(),
                completionHandler = null
            )
        })
    }
}