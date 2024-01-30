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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import platform.UIKit.UIColor
import platform.UIKit.UIView

/**
 * Issue https://github.com/JetBrains/compose-multiplatform/issues/4004
 */
val UIKitViewOrder = Screen.Example("UIKitViewOrder") {
    Box(modifier = Modifier.fillMaxSize().background(Color.Yellow)) {
        UIKitView(
            factory = {
                UIView().apply {
                    backgroundColor = UIColor.blueColor
                }
            },
            modifier = Modifier.size(100.dp),
        )
        UIKitView(
            factory = {
                UIView().apply {
                    backgroundColor = UIColor.redColor
                }
            },
            modifier = Modifier.size(100.dp).offset(50.dp, 50.dp),
        )
    }
}
