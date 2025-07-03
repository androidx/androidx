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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSSelectorFromString
import platform.MapKit.MKMapView
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIEvent
import platform.UIKit.UILabel
import platform.UIKit.UIMenu
import platform.UIKit.UINavigationController
import platform.UIKit.UIScrollView
import platform.UIKit.UITextField
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

val InteropExample = Screen.Example("Interop") {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        UIKitViewInteractionModeSection()
        UIKitViewControllerSection()
        TextSection()
        TextFieldSection()
        ModalNavigationSection()
        UIMenuSection()
        ComposeSwipeBackFullscreenSection()
        UIScrollViewSection()
        ComposeInsideScrollViewSection()
    }
}

@Composable
private fun UIKitViewInteractionModeSection() {
    Text("UIKitView - interactionMode")
    UIKitView(
        factory = { TouchReactingView("Cooperative, Default") },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.Cooperative()
        )
    )
    UIKitView(
        factory = { TouchReactingView("Cooperative, 1000 ms") },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.Cooperative(delayMillis = 1000)
        )
    )
    UIKitView(
        factory = { TouchReactingView("Non-cooperative") },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative
        )
    )
    UIKitView(
        factory = { TouchReactingView("Non-interactive") },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        properties = UIKitInteropProperties(
            interactionMode = null
        )
    )
}

@Composable
private fun TextFieldSection() {
    var text by remember { mutableStateOf("Type something") }
    Text("Material TextField:")
    TextField(text, onValueChange = { text = it }, Modifier.fillMaxWidth())

    Text("UITextField:")
    ComposeUITextField(
        text,
        onValueChange = { text = it },
        Modifier.fillMaxWidth().height(40.dp)
    )
}

@Composable
private fun ModalNavigationSection() {
    Text("Modal navigation:")
    val controller = LocalUIViewController.current
    Button({
        controller.presentModalViewController(
            ComposeUIViewController { ModalViewContent() },
            animated = true
        )
    }, modifier = Modifier.fillMaxWidth().height(40.dp)) {
        Text("Show modal view")
    }
}

@Composable
private fun ComposeInsideScrollViewSection() {
    Text("Compose scene inside scroll view:")
    val parent = LocalUIViewController.current
    UIKitView(
        factory = {
            val scrollView = UIScrollView()
            scrollView.setContentSize(CGSizeMake(1000.0, 100.0))
            scrollView.backgroundColor = UIColor.whiteColor

            val composeViewController = ComposeUIViewController {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray)
                ) {
                    Button(
                        onClick = { println("Clicked") },
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        Column {
                            Text("Compose Button")
                            Text("(shouldn't click when swipe over)", fontSize = 10.sp)
                        }
                    }
                }
            }
            parent.addChildViewController(composeViewController)
            scrollView.addSubview(composeViewController.view)
            composeViewController.view.setFrame(CGRectMake(10.0, 10.0, 300.0, 80.0))
            composeViewController.didMoveToParentViewController(parent)

            scrollView
        },
        modifier = Modifier.fillMaxWidth().height(100.dp),
    )
}

@Composable
private fun ComposeSwipeBackFullscreenSection() {
    Text("Swipe back on modal navigation:")
    val controller = LocalUIViewController.current
    Button({
        val navigationController = UINavigationController()
        navigationController.setViewControllers(listOf(
            ComposeUIViewController { ModalViewContent() }.also { it.title = "Screen 1"},
            ComposeUIViewController { ModalViewContent() }.also { it.title = "Screen 2"},
            ComposeUIViewController { ModalViewContent() }.also { it.title = "Screen 3"}
        ))

        controller.presentModalViewController(navigationController, animated = true)
    }, modifier = Modifier.fillMaxWidth().height(40.dp)) {
        Text("Show UINavigationController")
    }
}

@Composable
private fun UIScrollViewSection() {
    Text("Horizontal interop UIScrollView:")
    UIKitView(
        factory = {
            val scrollView = UIScrollView()
            scrollView.setContentSize(CGSizeMake(1000.0, 100.0))
            scrollView.backgroundColor = UIColor.lightGrayColor
            scrollView
        },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        update = {
            println("MKMapView updated")
        }
    )

    Text("Vertical interop UIScrollView:")
    var scrollViewSize by mutableStateOf(DpSize.Zero)
    val density = LocalDensity.current
    UIKitView(
        factory = {
            val scrollView = UIScrollView()
            scrollView.setContentSize(
                CGSizeMake(scrollViewSize.width.value.toDouble(), 1000.0)
            )
            scrollView.backgroundColor = UIColor.lightGrayColor
            scrollView
        },
        update = { scrollView ->
            scrollView.setContentSize(
                CGSizeMake(scrollViewSize.width.value.toDouble(), 1000.0)
            )
        },
        modifier = Modifier.fillMaxWidth().height(400.dp).onSizeChanged {
            scrollViewSize = with(density) {
                DpSize(it.width.toDp(), it.height.toDp())
            }
        }
    )
}

@Composable
private fun TextSection() {
    Text("Material and Native text:")
    Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
        Text(
            text = "material.Text",
            modifier = Modifier.weight(0.5f).height(40.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            letterSpacing = 0.sp,
            style = MaterialTheme.typography.body1
        )
        UIKitView(
            factory = {
                val label = UILabel(frame = CGRectZero.readValue())
                label.text = "UIKit.UILabel"
                label.textColor = UIColor.blackColor
                label
            },
            modifier = Modifier.weight(0.5f).height(40.dp)
                .border(1.dp, Color.Blue),
            onReset = { /* Just to make it reusable */ }
        )
    }
}

@Composable
private fun UIKitViewControllerSection() {
    var updatedValue by remember { mutableStateOf(null as Offset?) }

    Text("UIViewController subclass:")
    UIKitViewController(
        factory = {
            BlueViewController()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .onGloballyPositioned { coordinates ->
                val rootCoordinates = coordinates.findRootCoordinates()
                val box =
                    rootCoordinates.localBoundingBoxOf(coordinates, clipBounds = false)
                updatedValue = box.topLeft
            },
        update = { viewController ->
            updatedValue?.let {
                viewController.label.text = "Custom UIViewController:\n${it.x}, ${it.y}"
            }
        },
        properties = UIKitInteropProperties(
            isNativeAccessibilityEnabled = true
        )
    )
}

@Composable
fun UIMenuSection() {
    Text("UIMenu:")
    UIKitView(
        factory = {
            UIButton.buttonWithType(UIButtonTypeSystem).apply {
                showsMenuAsPrimaryAction = true
                menu = UIMenu.menuWithChildren(
                    listOf(
                        UIAction.actionWithTitle("Menu Item 1", null, null) {
                            println("Selected 'Menu Item 1'")
                        },
                        UIAction.actionWithTitle("Menu Item 2", null, null) {
                            println("Selected 'Menu Item 2'")
                        },
                        UIAction.actionWithTitle("Menu Item 3", null, null) {
                            println("Selected 'Menu Item 3'")
                        }
                    )
                )
                setTitle("Open UIMenu", forState = UIControlStateNormal)
            }
        },
        modifier = Modifier.fillMaxWidth().height(40.dp)
    )
}

private class BlueViewController : UIViewController(nibName = null, bundle = null) {
    val label = UILabel()

    override fun loadView() {
        setView(label)
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        label.setNumberOfLines(2)
        label.textAlignment = NSTextAlignmentCenter
        label.textColor = UIColor.whiteColor
        label.backgroundColor = UIColor.blueColor
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

private class TouchReactingView(val label: String) : UIButton(frame = CGRectZero.readValue()) {
    init {
        setDefaultColor()
        setTitle(label, forState = UIControlStateNormal)
    }

    private fun setDefaultColor() {
        backgroundColor = UIColor.darkGrayColor
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)
        backgroundColor = UIColor.redColor
        println("[${label}]: Touches Began")
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)
        println("[${label}]: Touches Moved")
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)
        setDefaultColor()
        println("[${label}]: Touches Ended")
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)
        setDefaultColor()
        println("[${label}]: Touches Cancelled")
    }
}

@Composable
fun ModalViewContent() {
    Column {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.LightGray)) {
            Text(
                text = "Non-scrollable area.\nSwipe down to hide modal view",
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .padding(top = 100.dp)
                    .height(1000.dp)
            ) {
                // TODO: https://youtrack.jetbrains.com/issue/CMP-5707
                Text(
                    text = "Scrollable area.\nSwipe down to collapse - does not work yet.",
                    modifier = Modifier.align(Alignment.TopCenter)
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
@OptIn(BetaInteropApi::class)
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
            println(
                "Update called for UITextField(0x${
                    textField.objcPtr().toLong().toString(16)
                }, value = $value"
            )
            textField.text = value
        }
    )
}

val ReusableMapsExample = Screen.Example("Reusable maps") {
    var allocations: Int by remember { mutableStateOf(0) }
    var allocationsCounter by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Text("Maps allocated: $allocations")
        LazyColumn(Modifier.fillMaxSize()) {
            items(100) { index ->
                UIKitView(
                    factory = {
                        val view = object : MKMapView(frame = CGRectZero.readValue()) {
                            var index = 0

                            override fun didMoveToWindow() {
                                super.didMoveToWindow()

                                if (window != null) {
                                    println("MKMapView appeared, tag = $tag, index = ${this.index}")
                                } else {
                                    println("MKMapView disappeared, tag = $tag, index = ${this.index}")
                                }
                            }
                        }.apply {
                            tag = allocationsCounter.toLong()
                        }
                        allocations += 1
                        allocationsCounter += 1

                        view
                    },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    update = {
                        println("Update called for tag = ${it.tag}, index = $index")
                    },
                    onReset = {
                        it.index = index
                        println("Reset called for tag = ${it.tag}, index = $index")
                    },
                    onRelease = {
                        allocations -= 1
                    }
                )
            }
        }
    }
}
