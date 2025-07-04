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

package androidx.camera.core

import android.util.Range
import androidx.annotation.RestrictTo
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.impl.UseCaseType
import androidx.camera.core.featuregroup.impl.UseCaseType.Companion.getFeatureGroupUseCaseType
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/**
 * Represents a session configuration to start a camera session. When used with `camera-lifecycle`,
 * this SessionConfig is expected to be used for starting a camera session (e.g. by being bound to
 * the [androidx.lifecycle.LifecycleOwner] via
 * `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` API which allows the lifecycle
 * events to start and stop the camera session with this given configuration).
 *
 * It consists of a collection of [UseCase], session parameters to be applied on the camera session,
 * and common properties like the field-of-view defined by [ViewPort], the [CameraEffect], frame
 * rate, required or preferred [GroupableFeature] groups etc.
 *
 * When configuring a session config with `GroupableFeature`s (i.e. [requiredFeatureGroup] or
 * [preferredFeatureGroup] is used), avoid using non-grouping APIs for any feature that is groupable
 * (see [GroupableFeature] to know which features are groupable). Doing so can lead to conflicting
 * configurations and an [IllegalArgumentException]. The following code sample explains this
 * further.
 *
 * @sample androidx.camera.core.samples.configureSessionConfigWithFeatureGroups
 * @property useCases The list of [UseCase] to be attached to the camera and receive camera data.
 *   This can't be empty.
 * @property viewPort The [ViewPort] to be applied on the camera session. If not set, the default is
 *   no viewport.
 * @property effects The list of [CameraEffect] to be applied on the camera session. If not set, the
 *   default is no effects.
 * @property requiredFeatureGroup A set of [GroupableFeature] that are mandatory for the camera
 *   session configuration. If not set, the default is an empty set. See
 *   [SessionConfig.Builder.setRequiredFeatureGroup] for more info.
 * @property preferredFeatureGroup A list of preferred [GroupableFeature] ordered according to
 *   priority in descending order, i.e. a feature with a lower index in the list is considered to
 *   have a higher priority. If not set, the default is an empty list. See
 *   [SessionConfig.Builder.setPreferredFeatureGroup] for more info.
 * @property frameRateRange The desired frame rate range for the camera session. The value must be
 *   one of the supported frame rates queried by [CameraInfo.getSupportedFrameRateRanges] with a
 *   specific [SessionConfig], or an [IllegalArgumentException] will be thrown during
 *   `SessionConfig` binding (i.e. when calling
 *   `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` or
 *   `androidx.camera.lifecycle.LifecycleCameraProvider.bindToLifecycle`). When this value is set,
 *   any target frame rate set on individual [UseCase] will be ignored during `SessionConfig`
 *   binding. If this value is not set, the default is [FRAME_RATE_RANGE_UNSPECIFIED], which means
 *   no specific frame rate. The range defines the acceptable minimum and maximum frame rate for the
 *   camera session. A **dynamic range** (e.g., `[15, 30]`) allows the camera to adjust its frame
 *   rate within the bounds, which will benefit **previewing in low light** by enabling longer
 *   exposures for brighter, less noisy images; conversely, a **fixed range** (e.g., `[30, 30]`)
 *   ensures a stable frame rate crucial for **video recording**, though it can lead to darker,
 *   noisier video in low light due to shorter exposure times.
 * @throws IllegalArgumentException If the combination of config options are conflicting or
 *   unsupported.
 * @See androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle
 */
@ExperimentalSessionConfig
public open class SessionConfig
@JvmOverloads
constructor(
    useCases: List<UseCase>,
    public val viewPort: ViewPort? = null,
    public val effects: List<CameraEffect> = emptyList(),
    public val frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
    public val requiredFeatureGroup: Set<GroupableFeature> = emptySet(),
    public val preferredFeatureGroup: List<GroupableFeature> = emptyList(),
) {
    public val useCases: List<UseCase> = useCases.distinct()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public open val isLegacy: Boolean = false
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val sessionType: Int = SESSION_TYPE_REGULAR

    /**
     * Gets the feature selection listener set to this session config.
     *
     * @see setFeatureSelectionListener
     */
    public var featureSelectionListener: Consumer<Set<GroupableFeature>> =
        Consumer<Set<GroupableFeature>> {}
        private set

    /**
     * Gets the executor set to this session config for feature selection listener invocation.
     *
     * @see setFeatureSelectionListener
     */
    public var featureSelectionListenerExecutor: Executor = CameraXExecutors.mainThreadExecutor()
        private set

    init {
        validateFeatureCombination()
    }

    /** Creates the SessionConfig from use cases only. */
    public constructor(vararg useCases: UseCase) : this(useCases.toList())

    private fun validateFeatureCombination() {
        if (requiredFeatureGroup.isEmpty() && preferredFeatureGroup.isEmpty()) {
            return
        }

        // Currently, there is only feature instance possible per type. But this can change in
        // future, e.g. a VIDEO_STABILIZATION feature object may need to be added in future.
        validateRequiredFeatures()

        require(preferredFeatureGroup.distinct().size == preferredFeatureGroup.size) {
            "Duplicate values in preferredFeatures($preferredFeatureGroup)"
        }

        val duplicateFeatures = requiredFeatureGroup.intersect(preferredFeatureGroup)
        require(duplicateFeatures.isEmpty()) {
            "requiredFeatures and preferredFeatures have duplicate values: $duplicateFeatures"
        }

        useCases.forEach {
            require(it.getFeatureGroupUseCaseType() != UseCaseType.UNDEFINED) {
                "$it is not supported with feature group"
            }
        }

        require(effects.isEmpty()) { "Effects aren't supported with feature group yet" }
    }

    /**
     * Validates that there are no conflicting values for the same feature in
     * [requiredFeatureGroup].
     *
     * @throws IllegalArgumentException If there are conflicting values for the same feature.
     */
    private fun validateRequiredFeatures() {
        val requiredFeatureTypes = requiredFeatureGroup.map { it.featureTypeInternal }.distinct()
        requiredFeatureTypes.forEach { featureType ->
            val distinctFeaturesPerType =
                requiredFeatureGroup.filter { it.featureTypeInternal == featureType }

            require(distinctFeaturesPerType.size <= 1) {
                "requiredFeatures has conflicting feature values: $distinctFeaturesPerType"
            }
        }
    }

    /**
     * Sets a listener to know which features are finally selected when a session config is bound,
     * based on the user-defined priorities/ordering for [preferredFeatureGroup] and device
     * capabilities.
     *
     * Both the required and the selected preferred features are notified to the listener. The
     * listener is invoked when this session config is bound to camera (e.g. when the
     * `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` API is invoked). It is
     * invoked even when no preferred features are selected, providing either the required features
     * or an empty set (if no feature was set as required).
     *
     * Alternatively, the [CameraInfo.isFeatureGroupSupported] API can be used to query if a set of
     * features is supported before binding.
     *
     * @param executor The executor in which the listener will be invoked. If not set, the main
     *   thread is used by default.
     * @param listener The consumer to accept the final set of features when they are selected.
     */
    @JvmOverloads
    public fun setFeatureSelectionListener(
        executor: Executor = CameraXExecutors.mainThreadExecutor(),
        listener: Consumer<Set<GroupableFeature>>,
    ) {
        featureSelectionListener = listener
        featureSelectionListenerExecutor = executor
    }

    /** Builder for [SessionConfig] */
    @ExperimentalSessionConfig
    public class Builder(private val useCases: List<UseCase>) {
        private var viewPort: ViewPort? = null
        private var effects: MutableList<CameraEffect> = mutableListOf()
        private var frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED
        private val requiredFeatureGroup = mutableListOf<GroupableFeature>()
        private val preferredFeatureGroup = mutableListOf<GroupableFeature>()

        public constructor(vararg useCases: UseCase) : this(useCases.toList())

        /** Sets the [ViewPort] to be applied on the camera session. */
        public fun setViewPort(viewPort: ViewPort): Builder {
            this.viewPort = viewPort
            return this
        }

        /** Adds a [CameraEffect] to be applied on the camera session. */
        public fun addEffect(effect: CameraEffect): Builder {
            this.effects.add(effect)
            return this
        }

        /**
         * Sets the frame rate range for the camera session.
         *
         * See [SessionConfig.frameRateRange] for more details.
         *
         * @param frameRateRange The frame rate range to be applied on the camera session.
         */
        public fun setFrameRateRange(frameRateRange: Range<Int>): Builder {
            this.frameRateRange = frameRateRange
            return this
        }

        /**
         * Sets the list of [GroupableFeature] that are mandatory for the camera configuration.
         *
         * If any of the features is not supported or if the features are not supported together as
         * a combination, an [IllegalArgumentException] will be thrown when the [SessionConfig] is
         * bound to a lifecycle (e.g. when the
         * `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` API is invoked).
         *
         * To avoid setting an unsupported feature as required, the [setPreferredFeatureGroup] API
         * can be used since the features from the preferred features are selected on a best-effort
         * basis according to the priority defined by the ordering of features in the list.
         * Alternatively, the [CameraInfo.isFeatureGroupSupported] API can be used before binding to
         * check if the features are supported or not.
         *
         * Note that [CameraEffect] or [ImageAnalysis] use case is currently not supported when a
         * feature is set to a session config. Furthermore, unlike the [setPreferredFeatureGroup]
         * API, the order of the features doesn't matter for this API since each and every one of
         * these features must be configured.
         *
         * @param features The vararg of `GroupableFeature` objects to add to the required features.
         * @return The [Builder] instance, allowing for method chaining.
         * @see GroupableFeature
         */
        public fun setRequiredFeatureGroup(vararg features: GroupableFeature): Builder {
            requiredFeatureGroup.clear()
            requiredFeatureGroup.addAll(features)
            return this
        }

        /**
         * Sets the list of preferred [GroupableFeature], ordered by priority in descending order.
         *
         * Features are evaluated for support based on this specified priority. The feature with a
         * lower index (listed first) is considered higher priority. The system attempts to enable
         * preferred features on a best-effort basis:
         * - It starts with the highest priority feature.
         * - If a feature is supported (considering device capabilities and any other already
         *   selected preferred features or required features), it's added to the selection.
         * - If a preferred feature is *not* supported, it's skipped, and the system proceeds to
         *   evaluate the next feature in the preferred list.
         *
         * For example, consider the following scenarios where [SessionConfig.requiredFeatureGroup]
         * is empty:
         *
         * |Preferred List                  |Device Support              |Selected Features       |
         * |--------------------------------|----------------------------|------------------------|
         * |`[HDR_HLG10, FPS_60, ULTRA_HDR]`|HLG10 + 60 FPS not supported|`[HDR_HLG10, ULTRA_HDR]`|
         * |`[FPS_60, HDR_HLG10, ULTRA_HDR]`|HLG10 + 60 FPS not supported|`[FPS_60, ULTRA_HDR]`   |
         * |`[HDR_HLG10, FPS_60, ULTRA_HDR]`|HLG10 is not supported      |`[FPS_60, ULTRA_HDR]`   |
         * |`[HDR_HLG10, FPS_60]`           |Both supported together     |`[HDR_HLG10, FPS_60]`   |
         *
         * The final set of selected features will be notified to the listener set by the
         * [SessionConfig.setFeatureSelectionListener] API.
         *
         * Note that [CameraEffect] or [ImageAnalysis] use case is currently not supported when a
         * feature is set to a session config.
         *
         * @param features The list of preferred features, ordered by preference.
         * @return The [Builder] instance, allowing for method chaining.
         * @see GroupableFeature
         */
        public fun setPreferredFeatureGroup(vararg features: GroupableFeature): Builder {
            preferredFeatureGroup.clear()
            preferredFeatureGroup.addAll(features)
            return this
        }

        /** Builds a [SessionConfig] from the current configuration. */
        public fun build(): SessionConfig {
            return SessionConfig(
                useCases = useCases,
                viewPort = viewPort,
                effects = effects.toList(),
                frameRateRange = frameRateRange,
                requiredFeatureGroup = requiredFeatureGroup.toSet(),
                preferredFeatureGroup = preferredFeatureGroup.toList(),
            )
        }
    }
}

/** The legacy SessionConfig which allows sequential binding. This is used internally. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalSessionConfig::class)
public class LegacySessionConfig(
    useCases: List<UseCase>,
    viewPort: ViewPort? = null,
    effects: List<CameraEffect> = emptyList(),
) : SessionConfig(useCases, viewPort, effects) {
    public override val isLegacy: Boolean = true

    public constructor(
        useCaseGroup: UseCaseGroup
    ) : this(useCaseGroup.useCases, useCaseGroup.viewPort, useCaseGroup.effects)
}
