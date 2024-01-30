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

package bugs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExportObjCClass
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCoder
import platform.MapKit.MKMapView
import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

val UIKitViewMatryoshka = Screen.Example("UIKitViewMatryoshka") {
    // Issue: https://github.com/JetBrains/compose-multiplatform/issues/4095
    Box(Modifier.fillMaxSize().background(Color.White).padding(50.dp)) {
        UIKitViewController(
            factory = { InnerUIKitViewController() },
            modifier = Modifier.size(600.dp, 600.dp)
        )
    }
}

@ExportObjCClass
class InnerUIKitViewController: UIViewController(nibName = null, bundle = null) {

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.setBackgroundColor(UIColor.grayColor)

        val innerCompose = InnerComposeViewController()
        addChildViewController(innerCompose)
        view.addSubview(innerCompose.view)
        innerCompose.view.setFrame(CGRectMake(50.0, 50.0, 500.0, 500.0))
        innerCompose.didMoveToParentViewController(this)
    }
}


private fun InnerComposeViewController() = ComposeUIViewController {
    Box(Modifier.fillMaxSize().background(Color.Green).padding(50.dp)) {
        UIKitView(
            factory = { MKMapView() },
            modifier = Modifier.fillMaxSize()
        )
    }
}
