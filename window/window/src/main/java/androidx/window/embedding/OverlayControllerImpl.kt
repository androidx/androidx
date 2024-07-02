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

import android.content.res.Configuration
import android.util.ArrayMap
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer as JetpackConsumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityEmbeddingOptionsImpl.getOverlayAttributes
import androidx.window.embedding.ActivityEmbeddingOptionsImpl.putActivityStackAlignment
import androidx.window.embedding.OverlayController.Companion.OVERLAY_FEATURE_VERSION
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityStack
import androidx.window.extensions.embedding.ActivityStackAttributes
import androidx.window.extensions.embedding.ParentContainerInfo
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.adapter.extensions.ExtensionsWindowLayoutInfoAdapter
import androidx.window.layout.util.DensityCompatHelper
import androidx.window.reflection.Consumer2
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The core implementation of [OverlayController] APIs, which is implemented by [ActivityStack]
 * operations in WM Extensions.
 */
@Suppress("NewApi") // Suppress #translateWindowMetrics, which requires R.
@RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
internal open class OverlayControllerImpl(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter,
) {
    private val globalLock = ReentrantLock()

    @GuardedBy("globalLock")
    internal var overlayAttributesCalculator:
        ((OverlayAttributesCalculatorParams) -> OverlayAttributes)? =
        null
        get() = globalLock.withLock { field }
        set(value) {
            globalLock.withLock { field = value }
        }

    /**
     * Mapping between the overlay container tag and its default [OverlayAttributes]. It's to record
     * the [OverlayAttributes] updated through [updateOverlayAttributes] and report in
     * [OverlayAttributesCalculatorParams].
     */
    @GuardedBy("globalLock")
    private val overlayTagToDefaultAttributesMap: MutableMap<String, OverlayAttributes> = ArrayMap()

    /**
     * Mapping between the overlay container tag and its current [OverlayAttributes] to provide the
     * [OverlayInfo] updates.
     */
    @GuardedBy("globalLock")
    private val overlayTagToCurrentAttributesMap = ArrayMap<String, OverlayAttributes>()

    @GuardedBy("globalLock")
    private val overlayTagToContainerMap = ArrayMap<String, ActivityStack>()

    /** The mapping from [OverlayInfo] callback to [activityStacks][ActivityStack] callback. */
    @GuardedBy("globalLock")
    private val overlayInfoToActivityStackCallbackMap =
        ArrayMap<JetpackConsumer<OverlayInfo>, Consumer<List<ActivityStack>>>()

    init {
        WindowSdkExtensions.getInstance().requireExtensionVersion(OVERLAY_FEATURE_VERSION)

        embeddingExtension.setActivityStackAttributesCalculator { params ->
            globalLock.withLock {
                val parentContainerInfo = params.parentContainerInfo
                val density =
                    DensityCompatHelper.getInstance()
                        .density(
                            parentContainerInfo.configuration,
                            parentContainerInfo.windowMetrics
                        )
                val windowMetrics =
                    WindowMetricsCalculator.translateWindowMetrics(
                        parentContainerInfo.windowMetrics,
                        density
                    )
                val overlayAttributes =
                    calculateOverlayAttributes(
                        params.activityStackTag,
                        params.launchOptions.getOverlayAttributes(),
                        WindowMetricsCalculator.translateWindowMetrics(
                            params.parentContainerInfo.windowMetrics,
                            density
                        ),
                        params.parentContainerInfo.configuration,
                        ExtensionsWindowLayoutInfoAdapter.translate(
                            windowMetrics,
                            parentContainerInfo.windowLayoutInfo
                        ),
                    )

                // TODO(b/295805497): Migrate to either custom animation APIs or new
                //  ActivityStackAttributes APIs.
                // Set alignment to the bundle options as the hint of the animation direction.
                params.launchOptions.putActivityStackAlignment(overlayAttributes.bounds)
                return@setActivityStackAttributesCalculator overlayAttributes
                    .toActivityStackAttributes(parentContainerInfo)
            }
        }

        embeddingExtension.registerActivityStackCallback(Runnable::run) { activityStacks ->
            globalLock.withLock {
                val lastOverlayTags = overlayTagToContainerMap.keys

                overlayTagToContainerMap.clear()
                overlayTagToContainerMap.putAll(
                    activityStacks.getOverlayContainers().map { overlayContainer ->
                        Pair(overlayContainer.tag!!, overlayContainer)
                    }
                )

                cleanUpDismissedOverlayContainerRecords(lastOverlayTags)
            }
        }
    }

    /**
     * Clean up records associated with dismissed overlay [activityStacks][ActivityStack] when
     * there's a [ActivityStack] state update.
     *
     * The dismissed overlay [activityStacks][ActivityStack] are identified by comparing the
     * differences of [ActivityStack] state before and after update.
     *
     * @param lastOverlayTags Overlay containers' tag before applying [ActivityStack] state update.
     */
    @GuardedBy("globalLock")
    private fun cleanUpDismissedOverlayContainerRecords(lastOverlayTags: Set<String>) {
        if (lastOverlayTags.isEmpty()) {
            // If there's no last overlay container, return.
            return
        }

        val dismissedOverlayTags = ArrayList<String>()
        val currentOverlayTags = overlayTagToContainerMap.keys

        for (overlayTag in lastOverlayTags) {
            if (
                overlayTag !in currentOverlayTags &&
                    // If an overlay activityStack is not in the current overlay container list,
                    // check
                    // whether the activityStack does really not exist in WM Extensions in case
                    // an overlay container is just launched, but th WM Jetpack hasn't received the
                    // update yet.
                    embeddingExtension.getActivityStackToken(overlayTag) == null
            ) {
                dismissedOverlayTags.add(overlayTag)
            }
        }

        for (overlayTag in dismissedOverlayTags) {
            overlayTagToDefaultAttributesMap.remove(overlayTag)
            overlayTagToCurrentAttributesMap.remove(overlayTag)
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
     *   this overlay [ActivityStack]
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
        val defaultOverlayAttrs =
            getUpdatedOverlayAttributes(tag)
                ?: initialOverlayAttrs
                ?: throw IllegalArgumentException(
                    "Can't retrieve overlay attributes from launch options"
                )
        val currentOverlayAttrs =
            overlayAttributesCalculator?.invoke(
                OverlayAttributesCalculatorParams(
                    windowMetrics,
                    configuration,
                    windowLayoutInfo,
                    tag,
                    defaultOverlayAttrs,
                )
            ) ?: defaultOverlayAttrs

        overlayTagToCurrentAttributesMap[tag] = currentOverlayAttrs

        return currentOverlayAttrs
    }

    @VisibleForTesting
    internal open fun getUpdatedOverlayAttributes(overlayTag: String): OverlayAttributes? =
        overlayTagToDefaultAttributesMap[overlayTag]

    internal open fun updateOverlayAttributes(
        overlayTag: String,
        overlayAttributes: OverlayAttributes
    ) {
        globalLock.withLock {
            val activityStackToken =
                overlayTagToContainerMap[overlayTag]?.activityStackToken
                    // Users may call this API before any callback coming. Try to ask platform if
                    // this container exists.
                    ?: embeddingExtension.getActivityStackToken(overlayTag)
                    // Early return if there's no such ActivityStack associated with the tag.
                    ?: return

            embeddingExtension.updateActivityStackAttributes(
                activityStackToken,
                overlayAttributes.toActivityStackAttributes(
                    embeddingExtension.getParentContainerInfo(activityStackToken)!!
                )
            )

            // Update the tag-overlayAttributes map, which will be treated as the default
            // overlayAttributes in calculator.
            overlayTagToDefaultAttributesMap[overlayTag] = overlayAttributes
            overlayTagToCurrentAttributesMap[overlayTag] = overlayAttributes
        }
    }

    private fun OverlayAttributes.toActivityStackAttributes(
        parentContainerInfo: ParentContainerInfo
    ): ActivityStackAttributes =
        ActivityStackAttributes.Builder()
            .setRelativeBounds(
                EmbeddingBounds.translateEmbeddingBounds(
                        bounds,
                        adapter.translate(parentContainerInfo)
                    )
                    .toRect()
            )
            .setWindowAttributes(adapter.translateWindowAttributes())
            .build()

    private fun List<ActivityStack>.getOverlayContainers(): List<ActivityStack> =
        filter { activityStack -> activityStack.tag != null }.toList()

    open fun addOverlayInfoCallback(
        overlayTag: String,
        executor: Executor,
        overlayInfoCallback: JetpackConsumer<OverlayInfo>,
    ) {
        globalLock.withLock {
            val callback =
                Consumer2<List<ActivityStack>> { activityStacks ->
                    val overlayInfoList =
                        activityStacks.filter { activityStack -> activityStack.tag == overlayTag }
                    if (overlayInfoList.size > 1) {
                        throw IllegalStateException(
                            "There must be at most one overlay ActivityStack with $overlayTag"
                        )
                    }
                    val overlayInfo =
                        if (overlayInfoList.isEmpty()) {
                            OverlayInfo(
                                overlayTag,
                                currentOverlayAttributes = null,
                                activityStack = null,
                            )
                        } else {
                            overlayInfoList.first().toOverlayInfo()
                        }
                    overlayInfoCallback.accept(overlayInfo)
                }
            overlayInfoToActivityStackCallbackMap[overlayInfoCallback] = callback

            embeddingExtension.registerActivityStackCallback(executor, callback)
        }
    }

    private fun ActivityStack.toOverlayInfo(): OverlayInfo =
        OverlayInfo(
            tag!!,
            overlayTagToCurrentAttributesMap[tag!!],
            adapter.translate(this),
        )

    open fun removeOverlayInfoCallback(overlayInfoCallback: JetpackConsumer<OverlayInfo>) {
        globalLock.withLock {
            val callback = overlayInfoToActivityStackCallbackMap.remove(overlayInfoCallback)
            if (callback != null) {
                embeddingExtension.unregisterActivityStackCallback(callback)
            }
        }
    }
}
