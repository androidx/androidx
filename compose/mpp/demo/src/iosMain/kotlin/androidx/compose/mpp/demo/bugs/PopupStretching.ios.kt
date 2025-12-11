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

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIModalPresentationFormSheet
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.sheetPresentationController

val PopupStretching = Screen.Example("Popup stretching") {
    val viewController = LocalUIViewController.current

    Button(onClick = {
        val bottomSheetController = ComposeUIViewController {
            VerticalScrollWithIndependentHorizontalRows()
        }

        bottomSheetController.modalPresentationStyle = UIModalPresentationFormSheet
        bottomSheetController.sheetPresentationController?.setDetents(
            listOf(
                UISheetPresentationControllerDetent.mediumDetent(),
                UISheetPresentationControllerDetent.largeDetent(),
            )
        )

        viewController.presentViewController(bottomSheetController, animated = true, completion = {})
    }) {
        Text("Show popup")
    }
}


@Composable
fun VerticalScrollWithIndependentHorizontalRows() {
    Column(
        modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = true),
    ) {
        repeat(10) { rowIndex ->
            val horizontalScrollState = rememberScrollState()

            Spacer(Modifier.height(30.dp).background(Color.DarkGray))
            Row(
                modifier =
                Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .horizontalScroll(horizontalScrollState),
            ) {
                repeat(5) {
                    Box(
                        modifier =
                        Modifier
                            .size(100.dp)
                            .background(Color.Gray),
                    ) {
                        Text("Item $it in row $rowIndex")
                    }
                }
            }
        }
    }
}