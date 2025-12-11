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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UILabel

val InteropViewAndSemanticsConfigMerge = Screen.Example("InteropViewAndSemanticsConfigMerge") {
    Column {
        Button(onClick = {
            println("Clicked")
        }) {
            UIKitView(
                factory = {
                    val view = UILabel(frame = CGRectZero.readValue())
                    view.text = "UILabel"
                    view
                },
                modifier = Modifier.size(80.dp, 40.dp),
                accessibilityEnabled = false
            )
        }

        Button(onClick = {
            println("Clicked")
        }) {
            Row {
                UIKitView(
                    factory = {
                        val view = UILabel(frame = CGRectZero.readValue())
                        view.text = "Illegal"
                        view
                    },
                    modifier = Modifier.size(80.dp, 40.dp)
                )

                UIKitView(
                    factory = {
                        val view = UILabel(frame = CGRectZero.readValue())
                        view.text = "Merge"
                        view
                    },
                    modifier = Modifier.size(80.dp, 40.dp)
                )
            }
        }
    }
}