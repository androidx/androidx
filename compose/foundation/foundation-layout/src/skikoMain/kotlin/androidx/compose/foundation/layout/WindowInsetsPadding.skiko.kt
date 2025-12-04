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

@file:OptIn(InternalComposeUiApi::class)

package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.PlatformWindowInsetsProviderNode
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.safeContent
import androidx.compose.ui.platform.safeDrawing
import androidx.compose.ui.platform.safeGestures

actual fun Modifier.safeDrawingPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "safeDrawingPadding" },
        safeDrawingPaddingLambda
    )

private val safeDrawingPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    safeDrawing.toWindowInsets()
}

actual fun Modifier.safeGesturesPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "safeGesturesPadding" },
        safeGesturesPaddingLambda
    )

private val safeGesturesPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    safeGestures.toWindowInsets()
}

actual fun Modifier.safeContentPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "safeContentPadding" },
        safeContentPaddingLambda
    )

private val safeContentPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    safeContent.toWindowInsets()
}

actual fun Modifier.systemBarsPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "systemBarsPadding" },
        systemBarsPaddingLambda
    )

private val systemBarsPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    systemBars.toWindowInsets()
}

actual fun Modifier.displayCutoutPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "displayCutoutPadding" },
        displayCutoutPaddingLambda
    )

private val displayCutoutPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    displayCutout.toWindowInsets()
}

actual fun Modifier.statusBarsPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "statusBarsPadding" },
        statusBarsPaddingLambda
    )

private val statusBarsPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    statusBars.toWindowInsets()
}

actual fun Modifier.imePadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "imePadding" },
        imePaddingLambda
    )

private val imePaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    ime.toWindowInsets()
}

actual fun Modifier.navigationBarsPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "navigationBarsPadding" },
        navigationBarsPaddingLambda
    )

private val navigationBarsPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    navigationBars.toWindowInsets()
}

actual fun Modifier.captionBarPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "captionBarPadding" },
        captionBarPaddingLambda
    )

private val captionBarPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    captionBar.toWindowInsets()
}

actual fun Modifier.waterfallPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "waterfallPadding" },
        waterfallPaddingLambda
    )

private val waterfallPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    waterfall.toWindowInsets()
}

actual fun Modifier.systemGesturesPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "systemGesturesPadding" },
        systemGesturesPaddingLambda
    )

private val systemGesturesPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    systemGestures.toWindowInsets()
}

actual fun Modifier.mandatorySystemGesturesPadding(): Modifier =
    windowInsetsPadding(
        debugInspectorInfo { name = "mandatorySystemGesturesPadding" },
        mandatorySystemGesturesPaddingLambda
    )

private val mandatorySystemGesturesPaddingLambda: PlatformWindowInsets.() -> WindowInsets = {
    mandatorySystemGestures.toWindowInsets()
}

@Stable
private fun Modifier.windowInsetsPadding(
    inspectorInfo: InspectorInfo.() -> Unit,
    insetsCalculation: PlatformWindowInsets.() -> WindowInsets
): Modifier =
    this then PlatformWindowInsetsPaddingModifierElement(inspectorInfo, insetsCalculation)

private class PlatformWindowInsetsPaddingModifierElement(
    private val inspectorInfo: InspectorInfo.() -> Unit,
    private val insetsGetter: PlatformWindowInsets.() -> WindowInsets
): ModifierNodeElement<PlatformWindowInsetsPaddingModifierNode>() {
    override fun create(): PlatformWindowInsetsPaddingModifierNode = PlatformWindowInsetsPaddingModifierNode(insetsGetter)
    override fun update(node: PlatformWindowInsetsPaddingModifierNode) = node.update(insetsGetter)
    override fun InspectorInfo.inspectableProperties() = inspectorInfo()
    override fun hashCode(): Int = insetsGetter.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformWindowInsetsPaddingModifierElement) return false
        return insetsGetter === other.insetsGetter
    }
}

private class PlatformWindowInsetsPaddingModifierNode(
    private var insetsGetter: PlatformWindowInsets.() -> WindowInsets,
): PlatformWindowInsetsProviderNode(), ObserverModifierNode {

    private val insetsPaddingNode = delegate(
        InsetsPaddingModifierNode(WindowInsets())
    )

    fun update(insetsGetter: (PlatformWindowInsets) -> WindowInsets) {
        if (this.insetsGetter !== insetsGetter) {
            this.insetsGetter = insetsGetter
            windowInsetsInvalidated()
        }
    }

    override fun calculatePlatformInsets(ancestorWindowInsets: PlatformWindowInsets): PlatformWindowInsets = ancestorWindowInsets

    override fun onAttach() {
        super.onAttach()

        onObservedReadsChanged()
    }

    override fun windowInsetsInvalidated() {
        super.windowInsetsInvalidated()

        onObservedReadsChanged()
    }

    override fun onObservedReadsChanged() {
        observeReads {
            insetsPaddingNode.update(windowInsets.insetsGetter())
        }
    }
}