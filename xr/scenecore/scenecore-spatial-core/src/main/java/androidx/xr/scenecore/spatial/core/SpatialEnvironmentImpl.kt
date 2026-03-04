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

@file:Suppress("BanConcurrentHashMap", "BanSynchronizedMethods")

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.xr.scenecore.runtime.SpatialEnvironment
import androidx.xr.scenecore.runtime.SpatialEnvironmentExt
import androidx.xr.scenecore.runtime.SpatialEnvironmentFeature
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getIsPreferredSpatialEnvironmentActive
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getPassthroughOpacity
import com.android.extensions.xr.XrExtensionResult
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.passthrough.PassthroughState
import com.android.extensions.xr.space.SpatialState
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

/** Concrete implementation of SpatialEnvironment / XR Wallpaper for Android XR. */
internal class SpatialEnvironmentImpl(
    private val activity: Activity,
    private val xrExtensions: XrExtensions,
    rootSceneNode: Node,
    private val spatialStateProvider: Supplier<SpatialState>,
) : SpatialEnvironment, SpatialEnvironmentExt, Consumer<Consumer<Node>> {
    @VisibleForTesting internal val passthroughNode: Node = xrExtensions.createNode()
    private val spatialEnvironmentPreference =
        AtomicReference<SpatialEnvironment.SpatialEnvironmentPreference?>(null)

    // Store listeners with their executors
    private val spatialEnvironmentChangedListeners =
        ConcurrentHashMap<Consumer<Boolean>, Executor>()
    private val passthroughOpacityChangedListeners = ConcurrentHashMap<Consumer<Float>, Executor>()

    // The active passthrough opacity value is updated with every opacity change event. A null value
    // indicates it has not yet been initialized and the value should be read from the
    // spatialStateProvider.
    private var activePassthroughOpacity = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
    // Initialized to null to let system control opacity until preference is explicitly set.
    private var passthroughOpacityPreference = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
    private var previousSpatialState: SpatialState? = null
    private var spatialEnvironmentFeature: SpatialEnvironmentFeature? = null
    private var nodeAttachedListener: Consumer<Node>? = null

    init {
        xrExtensions.createNodeTransaction().use { transaction ->
            transaction
                .setName(passthroughNode, PASSTHROUGH_NODE_NAME)
                .setParent(passthroughNode, rootSceneNode)
                .apply()
        }
    }

    // TODO: Remove these once we know the Equals() and Hashcode() methods are correct.
    fun hasEnvironmentVisibilityChanged(spatialState: SpatialState): Boolean {
        if (previousSpatialState == null) {
            return true
        }

        val previousEnvironmentVisibility = previousSpatialState!!.environmentVisibility
        val currentEnvironmentVisibility = spatialState.environmentVisibility

        return (previousEnvironmentVisibility.currentState !=
            currentEnvironmentVisibility.currentState)
    }

    // TODO: Remove these once we know the Equals() and Hashcode() methods are correct.
    fun hasPassthroughVisibilityChanged(spatialState: SpatialState): Boolean {
        if (previousSpatialState == null) {
            return true
        }

        val previousPassthroughVisibility = previousSpatialState!!.passthroughVisibility
        val currentPassthroughVisibility = spatialState.passthroughVisibility

        if (
            previousPassthroughVisibility.currentState != currentPassthroughVisibility.currentState
        ) {
            return true
        }

        return (previousPassthroughVisibility.opacity != currentPassthroughVisibility.opacity)
    }

    // Package Private method to set the current passthrough opacity and
    // isPreferredSpatialEnvironmentActive from SceneRuntime.
    // This method is synchronized because it sets several internal state variables at once, which
    // should be treated as an atomic set. We could consider replacing with AtomicReferences.
    @Synchronized
    fun setSpatialState(spatialState: SpatialState): EnumSet<ChangedSpatialStates> {
        val changedSpatialStates = EnumSet.noneOf(ChangedSpatialStates::class.java)
        val passthroughVisibilityChanged = hasPassthroughVisibilityChanged(spatialState)
        if (passthroughVisibilityChanged) {
            changedSpatialStates.add(ChangedSpatialStates.PASSTHROUGH_CHANGED)
            activePassthroughOpacity = getPassthroughOpacity(spatialState.passthroughVisibility)
        }

        // TODO: b/371082454 - Check if the app is in FSM to ensure APP_VISIBLE refers to the
        // current app and not another app that is visible.
        val environmentVisibilityChanged = hasEnvironmentVisibilityChanged(spatialState)
        if (environmentVisibilityChanged) {
            changedSpatialStates.add(ChangedSpatialStates.ENVIRONMENT_CHANGED)
            isPreferredSpatialEnvironmentActive =
                getIsPreferredSpatialEnvironmentActive(
                    spatialState.environmentVisibility.currentState
                )
        }

        previousSpatialState = spatialState
        return changedSpatialStates
    }

    @Suppress("UNCHECKED_CAST")
    override fun accept(nodeConsumer: Consumer<Node>) {
        if (spatialEnvironmentFeature is Consumer<*>) {
            try {
                (spatialEnvironmentFeature as Consumer<Consumer<Node>>).accept(nodeConsumer)
            } catch (_: ClassCastException) {
                nodeAttachedListener = nodeConsumer
            }
        }
    }

    override val currentPassthroughOpacity: Float
        // Synchronized because we may need to update the entire Spatial State if the opacity has
        // not been initialized previously.
        @Synchronized
        get() {
            if (activePassthroughOpacity == SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE) {
                setSpatialState(spatialStateProvider.get())
            }
            return activePassthroughOpacity
        }

    override var preferredPassthroughOpacity: Float
        get() = passthroughOpacityPreference
        set(value) {
            // To work around floating-point precision issues, the opacity preference is documented
            // to clamp to 0.0f if it is set below 1% opacity and it clamps to 1.0f if it is set
            // above 99% opacity.
            // TODO: b/3692012 - Publicly document the passthrough opacity threshold values with
            // constants
            val newPassthroughOpacityPreference =
                if (value == SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE)
                    SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
                else (if (value < 0.01f) 0.0f else (if (value > 0.99f) 1.0f else value))

            if (newPassthroughOpacityPreference == passthroughOpacityPreference) {
                return
            }

            passthroughOpacityPreference = newPassthroughOpacityPreference
            // Passthrough should be enabled only if the user has explicitly set the
            // PassthroughOpacityPreference to a valid value, otherwise disabled.
            if (
                passthroughOpacityPreference != SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
            ) {
                xrExtensions.createNodeTransaction().use { transaction ->
                    transaction
                        .setPassthroughState(
                            passthroughNode,
                            passthroughOpacityPreference,
                            PassthroughState.PASSTHROUGH_MODE_MAX,
                        )
                        .apply()
                }
            } else {
                xrExtensions.createNodeTransaction().use { transaction ->
                    transaction
                        .setPassthroughState(
                            passthroughNode,
                            0.0f, // not show the app passthrough
                            PassthroughState.PASSTHROUGH_MODE_OFF,
                        )
                        .apply()
                }
            }
        }

    // This is called on the Activity's UI thread - so we should be careful to not block it.
    @Synchronized
    fun firePassthroughOpacityChangedEvent() {
        passthroughOpacityChangedListeners.forEach { (listener: Consumer<Float>, executor: Executor)
            ->
            executor.execute { listener.accept(currentPassthroughOpacity) }
        }
    }

    override fun addOnPassthroughOpacityChangedListener(
        executor: Executor,
        listener: Consumer<Float>,
    ) {
        passthroughOpacityChangedListeners[listener] = executor
    }

    override fun removeOnPassthroughOpacityChangedListener(listener: Consumer<Float>) {
        passthroughOpacityChangedListeners.remove(listener)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onRenderingFeatureReady(feature: SpatialEnvironmentFeature) {
        spatialEnvironmentFeature = feature
        try {
            if (nodeAttachedListener != null) {
                (feature as Consumer<Consumer<Node>>).accept(nodeAttachedListener!!)
                nodeAttachedListener = null
            }
        } catch (e: ClassCastException) {
            throw ClassCastException(e.toString())
        }
    }

    override var preferredSpatialEnvironment: SpatialEnvironment.SpatialEnvironmentPreference?
        get() {
            if (spatialEnvironmentFeature == null) {
                return spatialEnvironmentPreference.get()
            }
            return spatialEnvironmentFeature!!.preferredSpatialEnvironment
        }
        set(value) {
            if (spatialEnvironmentFeature == null) {
                val hasContent = value != null && (value.skybox != null || value.geometry != null)

                if (hasContent) {
                    throw UnsupportedOperationException(
                        "Did you forget to add scenecore-spatial-rendering in dependencies?"
                    )
                }

                val oldPreference = spatialEnvironmentPreference.getAndSet(value)

                if (oldPreference == value) {
                    return
                }

                if (value == null) {
                    // Detaching the app environment to go back to the system environment.
                    xrExtensions.detachSpatialEnvironment(
                        activity,
                        { it.run() },
                        { _: XrExtensionResult -> },
                    )
                } else {
                    // value is non-null but has no content, so attach an empty environment.
                    val currentRootEnvironmentNode = xrExtensions.createNode()
                    val skyboxMode = XrExtensions.NO_SKYBOX
                    xrExtensions.attachSpatialEnvironment(
                        activity,
                        currentRootEnvironmentNode,
                        skyboxMode,
                        { it.run() },
                        { _: XrExtensionResult -> },
                    )
                }
            } else {
                spatialEnvironmentFeature!!.preferredSpatialEnvironment = value
            }
        }

    override var isPreferredSpatialEnvironmentActive: Boolean = false
        private set

    // This is called on the Activity's UI thread - so we should be careful to not block it.
    @Synchronized
    fun fireOnSpatialEnvironmentChangedEvent() {
        val isActive = isPreferredSpatialEnvironmentActive
        spatialEnvironmentChangedListeners.forEach {
            (listener: Consumer<Boolean>, executor: Executor) ->
            executor.execute { listener.accept(isActive) }
        }
    }

    override fun addOnSpatialEnvironmentChangedListener(
        executor: Executor,
        listener: Consumer<Boolean>,
    ) {
        spatialEnvironmentChangedListeners[listener] = executor
    }

    override fun removeOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>) {
        spatialEnvironmentChangedListeners.remove(listener)
    }

    /**
     * Disposes of the environment and all of its resources.
     *
     * This should be called when the environment is no longer needed.
     */
    fun dispose() {
        if (spatialEnvironmentFeature != null) {
            spatialEnvironmentFeature!!.dispose()
            spatialEnvironmentFeature = null
        }
        activePassthroughOpacity = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        passthroughOpacityPreference = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        spatialEnvironmentPreference.set(null)
        isPreferredSpatialEnvironmentActive = false
        passthroughOpacityChangedListeners.clear()
        spatialEnvironmentChangedListeners.clear()
    }

    // Package Private enum to return which spatial states have changed.
    internal enum class ChangedSpatialStates {
        ENVIRONMENT_CHANGED,
        PASSTHROUGH_CHANGED,
    }

    companion object {
        const val PASSTHROUGH_NODE_NAME: String = "EnvironmentPassthroughNode"
    }
}
