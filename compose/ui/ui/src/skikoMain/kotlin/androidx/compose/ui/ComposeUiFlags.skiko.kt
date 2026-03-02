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

package androidx.compose.ui

import kotlin.jvm.JvmField

internal object SkikoComposeUiFlags {
    @Suppress("MutableBareField")
    @JvmField
    var useLegacyRenderNodeLayers: Boolean = false

    @Suppress("MutableBareField")
    @JvmField
    var isClearFocusOnMouseDownEnabled: Boolean = true

    @Suppress("MutableBareField")
    @JvmField
    var isDialogAnimationEnabled: Boolean = true

    @Suppress("MutableBareField")
    @JvmField
    var areWindowInsetsRulersEnabled: Boolean = true
}

/**
 * Use legacy (pre 1.8) render node layers for cases without
 * [androidx.compose.ui.graphics.layer.GraphicsLayer]. `false` by default.
 *
 * Note that it's a temporary flag for compatibility purposes, it will be removed in the future.
 */
@ExperimentalComposeUiApi
var ComposeUiFlags.useLegacyRenderNodeLayers by SkikoComposeUiFlags::useLegacyRenderNodeLayers

/**
 * This flag enables clearing focus on mouse down by default.
 *
 * More granular control is available in the various platform-specific entry points.
 */
@ExperimentalComposeUiApi
var ComposeUiFlags.isClearFocusOnMouseDownEnabled by SkikoComposeUiFlags::isClearFocusOnMouseDownEnabled

/**
 * When enabled the [androidx.compose.ui.window.Dialog] appear and disappear with animation.
 *
 * Note that it's a temporary flag, it will be removed in the future.
 */
@ExperimentalComposeUiApi
var ComposeUiFlags.isDialogAnimationEnabled by SkikoComposeUiFlags::isDialogAnimationEnabled

/**
 * Enable WindowInsets rulers:
 * * `SystemBarsRulers`
 * * `ImeRulers`
 * * `StatusBarsRulers`
 * * `NavigationBarsRulers`
 * * `CaptionBarRulers`
 * * `MandatorySystemGesturesRulers`
 * * `TappableElementRulers`
 * * `WaterfallRulers`
 * * `SafeDrawingRulers`
 * * `SafeGesturesRulers`
 * * `SafeContentRulers`
 */
@ExperimentalComposeUiApi
var ComposeUiFlags.areWindowInsetsRulersEnabled by SkikoComposeUiFlags::areWindowInsetsRulersEnabled
