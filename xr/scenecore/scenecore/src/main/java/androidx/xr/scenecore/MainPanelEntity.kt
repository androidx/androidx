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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.PixelDimensions as RtPixelDimensions
import androidx.xr.runtime.math.IntSize2d
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.function.Consumer

// TODO: b/428764847 - Replace platformAdapter property with rtEntity when PerceivedResolution
// methods are moved to MainPanelEntityImpl
/**
 * Represents the main spatialized panel in a [Scene].
 *
 * This entity serves as the primary 2D surface for the application, especially in Home Space Mode,
 * where it functions as the activity's main window.
 */
public class MainPanelEntity
internal constructor(
    private val lifecycleManager: LifecycleManager,
    private val platformAdapter: JxrPlatformAdapter,
    entityManager: EntityManager,
) :
    PanelEntity(
        lifecycleManager,
        platformAdapter.mainPanelEntity,
        entityManager,
        isMainPanelEntity = true,
    ) {

    private val perceivedResolutionListeners:
        ConcurrentMap<Consumer<IntSize2d>, Consumer<RtPixelDimensions>> =
        ConcurrentHashMap()

    // TODO: b/429429326 - Make this callback work in Full Space Mode
    /**
     * Sets the listener to be invoked when the perceived resolution of the main window changes in
     * Home Space Mode.
     *
     * The main panel's own rotation and the display's viewing direction are disregarded; this value
     * represents the pixel dimensions of the panel on the camera view without changing its distance
     * to the display.
     *
     * The listener is invoked on the provided executor.
     *
     * Non-zero values are only guaranteed in Home Space Mode. In Full Space Mode, the callback will
     * always return a (0,0) size. Use the [getPerceivedResolution] method to retrieve non-zero
     * values in Full Space Mode.
     *
     * @param callbackExecutor The [Executor] to run the listener on.
     * @param listener The [Consumer] to be invoked asynchronously on the given callbackExecutor
     *   whenever the maximum perceived resolution of the main panel changes. The parameter passed
     *   to the Consumer’s accept method is the new value for [IntSize2d] value for perceived
     *   resolution.
     */
    public fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<IntSize2d>,
    ): Unit {
        val rtListener =
            Consumer<RtPixelDimensions> { rtDimensions: RtPixelDimensions ->
                listener.accept(rtDimensions.toIntSize2d())
            }
        perceivedResolutionListeners.compute(
            listener,
            { _, _ ->
                platformAdapter.addPerceivedResolutionChangedListener(callbackExecutor, rtListener)
                rtListener
            },
        )
    }

    // TODO: b/429429326 - Make this callback work in Full Space Mode
    /**
     * Sets the listener to be invoked on the Main Thread Executor when the perceived resolution of
     * the main window changes in Home Space Mode.
     *
     * The main panel's own rotation and the display's viewing direction are disregarded; this value
     * represents the pixel dimensions of the panel on the camera view without changing its distance
     * to the display.
     *
     * There can only be one listener set at a time. If a new listener is set, the previous listener
     * will be released.
     *
     * Non-zero values are only guaranteed in Home Space Mode. In Full Space Mode, the callback will
     * always return a (0,0) size. Use the [PanelEntity.getPerceivedResolution] or
     * [SurfaceEntity.getPerceivedResolution] methods directly on the relevant entities to retrieve
     * non-zero values in Full Space Mode.
     *
     * @param listener The [Consumer] to be invoked asynchronously on the given callbackExecutor
     *   whenever the maximum perceived resolution of the main panel changes. The parameter passed
     *   to the Consumer’s accept method is the new value for [IntSize2d] value for perceived
     *   resolution.
     */
    public fun addPerceivedResolutionChangedListener(listener: Consumer<IntSize2d>): Unit =
        addPerceivedResolutionChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Releases the listener previously added by [addPerceivedResolutionChangedListener].
     *
     * @param listener The [Consumer] to be removed. It will no longer receive change events.
     */
    public fun removePerceivedResolutionChangedListener(listener: Consumer<IntSize2d>): Unit {
        perceivedResolutionListeners.computeIfPresent(
            listener,
            { _, rtListener ->
                platformAdapter.removePerceivedResolutionChangedListener(rtListener)
                null
            },
        )
    }

    public companion object {
        /** Returns the MainPanelEntity backed by the main window for the Activity. */
        internal fun create(
            lifecycleManager: LifecycleManager,
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
        ): MainPanelEntity = MainPanelEntity(lifecycleManager, adapter, entityManager)
    }
}
