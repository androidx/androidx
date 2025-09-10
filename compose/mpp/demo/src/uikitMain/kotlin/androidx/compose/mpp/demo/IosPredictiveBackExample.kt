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

package androidx.compose.mpp.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationFormSheet
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.sheetPresentationController

val IosPredictiveBackExample = Screen.Example("IosPredictiveBackExample") {
    IosPredictiveBackExampleContent()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun IosPredictiveBackExampleContent() {
    val viewController = LocalUIViewController.current

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        var showDialog by remember { mutableStateOf(false) }
        var showPopup by remember { mutableStateOf(false) }
        var enablePredictiveBackHandler by remember { mutableStateOf(true) }
        var predictiveBackHandlerState by remember { mutableStateOf("empty") }
        Button(
            onClick = { showDialog = true }
        ) {
            Text("Show dialog with Back Handler")
        }
        Button(
            onClick = { showPopup = true }
        ) {
            Text("Show popup with Back Handler")
        }
        if (showDialog) {
            Dialog(
                properties = DialogProperties(dismissOnClickOutside = false),
                onDismissRequest = {
                    showDialog = false
                }
            ) {
                Card {
                    Box(modifier = Modifier.background(MaterialTheme.colors.surface).padding(16.dp)) {
                        var bhState by remember { mutableStateOf("empty") }
                        Text("state: $bhState")
                        PredictiveBackHandler { events ->
                            try {
                                events.collect { event ->
                                    bhState = "\nx=${event.touchX}\ny=${event.touchY}\nprogress=${event.progress}\nedge=${event.swipeEdge}"
                                }
                                bhState = "DONE"
                                showDialog = false
                            } catch (e: Exception) {
                                bhState = "CANCEL"
                            }
                        }
                    }
                }
            }
        }
        if (showPopup) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showPopup = false },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = true
                )
            ) {
                Card {
                    Box(modifier = Modifier.background(MaterialTheme.colors.surface).padding(16.dp)) {
                        var bhState by remember { mutableStateOf("empty") }
                        Text("state: $bhState")
                        PredictiveBackHandler { events ->
                            try {
                                events.collect { event ->
                                    bhState = "\nx=${event.touchX}\ny=${event.touchY}\nprogress=${event.progress}\nedge=${event.swipeEdge}"
                                }
                                bhState = "DONE"
                                showPopup = false
                            } catch (e: Exception) {
                                bhState = "CANCEL"
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = {
            val presentedViewController = UIViewController(nibName = null, bundle = null)
            presentedViewController.view.backgroundColor = UIColor.yellowColor

            val composeViewController = ComposeUIViewController {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Box(modifier = Modifier.background(MaterialTheme.colors.surface).padding(16.dp)) {
                            var bhState by remember { mutableStateOf("empty") }
                            Text("state: $bhState")
                            PredictiveBackHandler { events ->
                                try {
                                    events.collect { event ->
                                        bhState = "\nx=${event.touchX}\ny=${event.touchY}\nprogress=${event.progress}\nedge=${event.swipeEdge}"
                                    }
                                    bhState = "DONE"
                                    showDialog = false
                                } catch (e: Exception) {
                                    bhState = "CANCEL"
                                }
                            }
                        }
                    }
                }
            }

            presentedViewController.addChildViewController(composeViewController)
            presentedViewController.view.addSubview(composeViewController.view)
            composeViewController.view.translatesAutoresizingMaskIntoConstraints = false
            composeViewController.view.layer.borderColor = UIColor.redColor.CGColor
            composeViewController.view.layer.borderWidth = 2.0
            NSLayoutConstraint.activateConstraints(
                listOf(
                    composeViewController.view.centerXAnchor.constraintEqualToAnchor(presentedViewController.view.centerXAnchor),
                    composeViewController.view.centerYAnchor.constraintEqualToAnchor(presentedViewController.view.centerYAnchor),
                    composeViewController.view.widthAnchor.constraintEqualToAnchor(presentedViewController.view.widthAnchor, 0.75),
                    composeViewController.view.heightAnchor.constraintEqualToAnchor(presentedViewController.view.heightAnchor, 0.5)
                )
            )
            composeViewController.didMoveToParentViewController(presentedViewController)

            presentedViewController.modalPresentationStyle = UIModalPresentationFormSheet
            presentedViewController.sheetPresentationController?.apply {
                detents = listOf(
                    UISheetPresentationControllerDetent.largeDetent(),
                    UISheetPresentationControllerDetent.mediumDetent()
                )

                prefersGrabberVisible = true
            }
            viewController.presentViewController(presentedViewController, true, null)
        }) {
            Text("Show native popup")
        }

        Button(
            onClick = { enablePredictiveBackHandler = !enablePredictiveBackHandler }
        ) {
            Text("Enable Predictive Back Handler: $enablePredictiveBackHandler")
        }
        Text("Predictive back handler state: $predictiveBackHandlerState")

        PredictiveBackHandler(enablePredictiveBackHandler) { events ->
            try {
                events.collect { event ->
                    predictiveBackHandlerState = "\nx=${event.touchX}\ny=${event.touchY}\nprogress=${event.progress}\nedge=${event.swipeEdge}"
                }
                predictiveBackHandlerState = "DONE"
            } catch (e: Exception) {
                predictiveBackHandlerState = "CANCEL"
            }
        }
    }

}