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

package androidx.window.embedding

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityEmbeddingOptionsImpl.getOverlayAttributes
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityStackAttributes
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.adapter.extensions.ExtensionsWindowLayoutInfoAdapter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The core implementation of [OverlayController] APIs, which is implemented by [ActivityStack]
 * operations in WM Extensions.
 */
@SuppressLint("NewApi")
@RequiresWindowSdkExtension(5)
internal class OverlayControllerImpl(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter,
) {
    private val globalLock = ReentrantLock()

    @GuardedBy("globalLock")
    internal var overlayAttributesCalculator:
        ((OverlayAttributesCalculatorParams) -> OverlayAttributes)? = null
        get() = globalLock.withLock { field }
        set(value) { globalLock.withLock { field = value } }

    init {
        WindowSdkExtensions.getInstance().requireExtensionVersion(5)

        embeddingExtension.setActivityStackAttributesCalculator { params ->
            globalLock.withLock {
                val windowMetrics = WindowMetricsCalculator.translateWindowMetrics(
                    params.parentContainerInfo.windowMetrics
                )
                val overlayAttributes = calculateOverlayAttributes(
                    params.activityStackTag,
                    params.launchOptions.getOverlayAttributes(),
                    WindowMetricsCalculator.translateWindowMetrics(
                        params.parentContainerInfo.windowMetrics
                    ),
                    params.parentContainerInfo.configuration,
                    ExtensionsWindowLayoutInfoAdapter.translate(
                        windowMetrics,
                        params.parentContainerInfo.windowLayoutInfo
                    ),
                )

                return@setActivityStackAttributesCalculator ActivityStackAttributes.Builder()
                    .setRelativeBounds(
                        EmbeddingBounds.translateEmbeddingBounds(
                            overlayAttributes.bounds,
                            adapter.translate(params.parentContainerInfo),
                        ).toRect()
                    ).setWindowAttributes(adapter.translateWindowAttributes())
                    .build()
            }
        }
    }

    /**
     * Calculates the [OverlayAttributes] to report to the [ActivityStackAttributes] calculator.
     *
     * The calculator then computes [ActivityStackAttributes] for rendering the overlay
     * [ActivityStack].
     *
     * @param tag The overlay [ActivityStack].
     * @param initialOverlayAttrs The [OverlayCreateParams.overlayAttributes] that used to launching
     * this overlay [ActivityStack]
     * @param windowMetrics The parent window container's [WindowMetrics]
     * @param configuration The parent window container's [Configuration]
     * @param windowLayoutInfo The parent window container's [WindowLayoutInfo]
     */
    @VisibleForTesting
    internal fun calculateOverlayAttributes(
        tag: String,
        initialOverlayAttrs: OverlayAttributes?,
        windowMetrics: WindowMetrics,
        configuration: Configuration,
        windowLayoutInfo: WindowLayoutInfo,
    ): OverlayAttributes {
        val defaultOverlayAttrs = initialOverlayAttrs
            ?: throw IllegalArgumentException(
                "Can't retrieve overlay attributes from launch options"
            )

        return overlayAttributesCalculator
            ?.invoke(
                OverlayAttributesCalculatorParams(
                    windowMetrics,
                    configuration,
                    windowLayoutInfo,
                    tag,
                    defaultOverlayAttrs,
                )
            ) ?: defaultOverlayAttrs
    }
}
