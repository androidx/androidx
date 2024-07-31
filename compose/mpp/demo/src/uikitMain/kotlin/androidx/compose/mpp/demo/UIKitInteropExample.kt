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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSSelectorFromString
import platform.MapKit.MKMapView
import platform.UIKit.*

private class TouchReactingView: UIView(frame = CGRectZero.readValue()) {
    init {
        setUserInteractionEnabled(true)

        setDefaultColor()
    }

    private fun setDefaultColor() {
        backgroundColor = UIColor.greenColor
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)
        backgroundColor = UIColor.redColor
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)
        setDefaultColor()
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)
        setDefaultColor()
    }
}

val UIKitInteropExample = Screen.Example("UIKitInterop") {
    var text by remember { mutableStateOf("Type something") }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            UIKitView(
                factory = {
                    MKMapView()
                },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }

        item {
            UIKitViewController(
                factory = {
                    object : UIViewController(nibName = null, bundle = null) {
                        override fun viewDidLoad() {
                            super.viewDidLoad()

                            view.backgroundColor = UIColor.blueColor
                        }

                        override fun viewWillAppear(animated: Boolean) {
                            super.viewWillAppear(animated)

                            println("viewWillAppear animated=$animated")
                        }

                        override fun viewDidAppear(animated: Boolean) {
                            super.viewDidAppear(animated)

                            println("viewDidAppear animated=$animated")
                        }

                        override fun viewDidDisappear(animated: Boolean) {
                            super.viewDidDisappear(animated)

                            println("viewDidDisappear animated=$animated")
                        }

                        override fun viewWillDisappear(animated: Boolean) {
                            super.viewWillDisappear(animated)

                            println("viewWillDisappear animated=$animated")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
        }
        items(100) { index ->
            when (index % 5) {
                0 -> Text("material.Text $index", Modifier.fillMaxSize().height(40.dp))
                1 -> UIKitView(
                    factory = {
                        val label = UILabel(frame = CGRectZero.readValue())
                        label.text = "UILabel $index"
                        label.textColor = UIColor.blackColor
                        label
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    interactive = false
                )
                2 -> TextField(text, onValueChange = { text = it }, Modifier.fillMaxWidth())
                3 -> ComposeUITextField(text, onValueChange = { text = it }, Modifier.fillMaxWidth().height(40.dp))
                4 -> UIKitView(
                    factory = { TouchReactingView() },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                )
            }
        }
    }
}

/**
 * Compose wrapper for native UITextField.
 * @param value the input text to be shown in the text field.
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field. Size should be specified in modifier.
 */
@Composable
private fun ComposeUITextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    val latestOnValueChanged by rememberUpdatedState(onValueChange)

    UIKitView(
        factory = {
            val textField = object : UITextField(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
                @ObjCAction
                fun editingChanged() {
                    latestOnValueChanged(text ?: "")
                }
            }
            textField.addTarget(
                target = textField,
                action = NSSelectorFromString(textField::editingChanged.name),
                forControlEvents = UIControlEventEditingChanged
            )
            textField
        },
        modifier = modifier,
        update = { textField ->
            textField.text = value
        }
    )
}
