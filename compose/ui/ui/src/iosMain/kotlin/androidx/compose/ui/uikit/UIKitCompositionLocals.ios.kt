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

package androidx.compose.ui.uikit

import androidx.compose.runtime.staticCompositionLocalOf
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/**
 * Public value to get UIViewController of Compose window for library authors.
 * Maybe useful for features, like VideoPlayer and Bottom menus.
 * Please use it carefully and don't add or remove other views - check
 * [androidx.compose.ui.interop.UIKitView] for those purposes.
 */
val LocalUIViewController = staticCompositionLocalOf<UIViewController> {
    error("CompositionLocal UIViewController not provided")
}

/**
 * Public value to get top UIView of the current Compose window for library authors.
 * This might be useful for adding extra UIKit views on top of Composable content.
 * Please use it carefully and don't add or remove other views - check
 * [androidx.compose.ui.interop.UIKitView] for those purposes.
 */
val LocalUIView = staticCompositionLocalOf<UIView> {
    error("CompositionLocal UIView not provided")
}
