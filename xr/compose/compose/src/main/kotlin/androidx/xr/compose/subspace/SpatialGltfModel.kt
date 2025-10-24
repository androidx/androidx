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

package androidx.xr.compose.subspace

import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.SpatialGltfModelSource.Companion.fromData
import androidx.xr.compose.subspace.SpatialGltfModelSource.Companion.fromPath
import androidx.xr.compose.subspace.SpatialGltfModelSource.Companion.fromUri
import androidx.xr.compose.subspace.layout.CoreModelEntity
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import java.nio.file.Path
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * A composable that loads and displays a 3D glTF model in the scene.
 *
 * This composable renders a `glTF` or `.glb` model that is loaded asynchronously from the provided
 * [source].
 *
 * ### Layout and Sizing
 *
 * The `SpatialGltfModel`'s layout size is determined by the bounding box size of the glTF model
 * that is being displayed coerced into the constraints of the layout.
 * - By default, the layout size will match the bounding box of the loaded 3D asset once it is
 *   loaded bounded by the current constraints.
 * - To force the `SpatialGltfModel` to a specific size, use a size modifier like
 *   `SubspaceModifier.size()`.
 * - The rendered model will be scaled uniformly to fit within the constraints imposed by the layout
 *   and modifiers.
 *
 * **Note:** Because the model is loaded asynchronously, its intrinsic size may be zero during
 * initial composition. The layout will be remeasured with the correct size once the model has
 * finished loading. You can use the [state] parameter to observe the loading status via
 * [SpatialGltfModelState.isSpatialModelReady].
 *
 * @param source A [SpatialGltfModelSource] that defines where to load the 3D model from. Use the
 *   helper functions [fromPath], [fromUri], or [fromData] to create one.
 * @param modifier The [SubspaceModifier] to be applied to this `SpatialGltfModel`.
 * @param state A [SpatialGltfModelState] object to observe and control the `SpatialGltfModel`.
 */
@Composable
@SubspaceComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SpatialGltfModel(
    source: SpatialGltfModelSource,
    modifier: SubspaceModifier = SubspaceModifier,
    state: SpatialGltfModelState? = null,
) {
    var loadingFailed by remember(source) { mutableStateOf(false) }
    if (loadingFailed) return // Do not proceed until the source has changed.

    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val coreModelEntity = remember { CoreModelEntity() }
    var intrinsicSize by remember { mutableStateOf(IntVolumeSize.Zero) }

    LaunchedEffect(state, source) {
        state?.reset()

        supervisorScope {
            launch(
                CoroutineExceptionHandler { _, throwable ->
                    state?.setLoadResult(Result.failure(throwable.cause ?: throwable))
                    loadingFailed = true
                }
            ) {
                val model = source.createModel(session)
                val entity = GltfModelEntity.create(session, model)
                coreModelEntity.attachEntity(entity)
                state?.setLoadResult(Result.success(coreModelEntity))
                intrinsicSize = coreModelEntity.intrinsicSize
            }
        }

        state?.watchAnimationState()
    }

    SubspaceLayout(
        modifier = modifier,
        coreEntity = coreModelEntity,
        measurePolicy = SpatialGltfModelMeasurePolicy(intrinsicSize),
    )
}

/**
 * Defines the source for a 3D model to be rendered by the [SpatialGltfModel] composable.
 *
 * Instances of [SpatialGltfModelSource] are created using [fromPath], [fromUri], or [fromData].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SpatialGltfModelSource {

    /**
     * Creates a [GltfModel] from the source.
     *
     * @param session The XR session to create the model in.
     * @return The created [GltfModel].
     */
    public suspend fun createModel(session: Session): GltfModel

    public companion object {
        /**
         * Creates a [SpatialGltfModelSource] that loads a `glTF` model from a [Path] relative to
         * the application's `assets/` folder.
         *
         * Currently, only binary `glTF` (`.glb`) files are supported.
         *
         * @param path The path of the binary `glTF` (`.glb`) model to be loaded, relative to the
         *   application's `assets/` folder.
         * @return A [SpatialGltfModelSource] that can be used with the [SpatialGltfModel]
         *   composable.
         * @throws IllegalArgumentException if [path] is an absolute path.
         */
        public fun fromPath(path: Path): SpatialGltfModelSource = PathGltfModelSource(path)

        private data class PathGltfModelSource(private val path: Path) : SpatialGltfModelSource {
            override suspend fun createModel(session: Session): GltfModel =
                GltfModel.create(session, path)
        }

        /**
         * Creates a [SpatialGltfModelSource] that loads a `glTF` model from a content [Uri].
         *
         * This is suitable for loading models from various sources, such as network locations or
         * local storage, that can be represented by a `Uri`.
         *
         * Currently, only binary `glTF` (`.glb`) files are supported.
         *
         * @param uri The `Uri` for the binary `glTF` (`.glb`) model to be loaded.
         * @return A [SpatialGltfModelSource] that can be used with the [SpatialGltfModel]
         *   composable.
         */
        public fun fromUri(uri: Uri): SpatialGltfModelSource = UriGltfModelSource(uri)

        private data class UriGltfModelSource(private val uri: Uri) : SpatialGltfModelSource {
            override suspend fun createModel(session: Session): GltfModel =
                GltfModel.create(session, uri)
        }

        /**
         * Creates a [SpatialGltfModelSource] that loads a `glTF` model from a [ByteArray].
         *
         * This is useful for loading models that are embedded directly in the application or
         * generated at runtime.
         *
         * Currently, only binary `glTF` (`.glb`) files are supported.
         *
         * @param assetData The byte array data of a binary `glTF` (`.glb`) model.
         * @param assetKey A unique key to identify the model in the internal cache. If not
         *   provided, the [hashCode] of the [assetData] will be used. Providing a stable key can
         *   improve performance by avoiding redundant parsing of the same model data.
         * @return A [SpatialGltfModelSource] that can be used with the [SpatialGltfModel]
         *   composable.
         */
        public fun fromData(
            assetData: ByteArray,
            assetKey: String = assetData.hashCode().toString(),
        ): SpatialGltfModelSource = DataGltfModelSource(assetData, assetKey)

        private class DataGltfModelSource(
            private val assetData: ByteArray,
            private val assetKey: String,
        ) : SpatialGltfModelSource {
            override suspend fun createModel(session: Session): GltfModel =
                GltfModel.create(session, assetData, assetKey)

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is DataGltfModelSource) return false

                if (!assetData.contentEquals(other.assetData)) return false
                if (assetKey != other.assetKey) return false

                return true
            }

            override fun hashCode(): Int {
                var result = assetData.contentHashCode()
                result = 31 * result + assetKey.hashCode()
                return result
            }
        }
    }
}

/**
 * A state object that can be hoisted to observe and control a [SpatialGltfModel].
 *
 * A `SpatialGltfModelState` can be used to query loading and animation status, and to start or stop
 * animations on the associated `SpatialGltfModel`.
 *
 * To create and remember a `SpatialGltfModelState`, use [remember] in your composable:
 * ```
 * val modelState = remember { SpatialGltfModelState() }
 * SpatialGltfModel(source = ..., state = modelState)
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SpatialGltfModelState {
    private val coreEntityActionQueue = ActionQueue<CoreModelEntity>()

    /** Indicates whether the [SpatialGltfModel] is currently animating or not. */
    public val isAnimating: State<Boolean>
        get() = _isAnimating

    private val _isAnimating: MutableState<Boolean> = mutableStateOf(false)

    /** The exception encountered should the [SpatialGltfModel] fail to load. */
    public val isSpatialModelReady: State<Boolean>
        get() = _isSpatialModelReady

    private val _isSpatialModelReady: MutableState<Boolean> = mutableStateOf(false)

    /** The exception encountered if the [SpatialGltfModel] fails to load. */
    public val loadException: State<Throwable?>
        get() = _loadException

    private val _loadException: MutableState<Throwable?> = mutableStateOf(null)

    private val shouldWatchForAnimationEndFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Starts an animation that will play once. If an animation name is not provided then the
     * default animation will play.
     *
     * @param name The name of the animation to play, as defined in the glTF file.
     */
    public fun startAnimation(name: String? = null) {
        coreEntityActionQueue.executeWhenAvailable {
            startAnimation(name)
            shouldWatchForAnimationEndFlow.compareAndSet(expect = false, update = true)
        }
    }

    /**
     * Starts an animation that will loop indefinitely. If an animation name is not provided then
     * the default animation will play.
     *
     * @param name The name of the animation to play, as defined in the glTF file.
     */
    public fun loopAnimation(name: String? = null) {
        coreEntityActionQueue.executeWhenAvailable {
            loopAnimation(name)
            _isAnimating.value = true
        }
    }

    /** Stops all currently playing animations. */
    public fun stopAllAnimations() {
        coreEntityActionQueue.executeWhenAvailable {
            stopAllAnimations()
            _isAnimating.value = false
        }
    }

    internal fun reset() {
        _loadException.value = null
        _isSpatialModelReady.value = false
        _isAnimating.value = false
    }

    internal fun setLoadResult(result: Result<CoreModelEntity>) {
        result
            .onSuccess { coreEntity ->
                coreEntityActionQueue.value = coreEntity
                _isSpatialModelReady.value = true
            }
            .onFailure { exception -> _loadException.value = exception }
    }

    internal suspend fun watchAnimationState() {
        // We don't have access to animation callbacks to tell us when an animation has ended.
        // Instead, we need to poll the animation state and update our state objects ourselves.
        shouldWatchForAnimationEndFlow.collect { shouldWatch ->
            val entity = coreEntityActionQueue.value ?: return@collect
            if (shouldWatch) {
                while (!entity.isAnimating) {
                    // using awaitFrame() here hangs in unit tests
                    delay(timeMillis = ANIMATION_POLLING_DELAY_MILLIS)
                }
                _isAnimating.value = true

                while (entity.isAnimating) {
                    // using awaitFrame() here hangs in unit tests
                    delay(timeMillis = ANIMATION_POLLING_DELAY_MILLIS)
                }
                _isAnimating.value = false

                shouldWatchForAnimationEndFlow.compareAndSet(expect = true, update = false)
            }
        }
    }

    private companion object {
        private const val ANIMATION_POLLING_DELAY_MILLIS = 20L
    }
}

private class SpatialGltfModelMeasurePolicy(private val intrinsicSize: IntVolumeSize) :
    SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        if (intrinsicSize == IntVolumeSize.Zero) {
            return layout(constraints.minWidth, constraints.minHeight, constraints.minDepth) {}
        }

        val scales =
            constraints.map(intrinsicSize.toFloatSize3d()) { value, min, max ->
                value.coerceIn(min, max) / value.coerceAtLeast(1f)
            }

        // The uniform scale factor is the minimum scale factor necessary to fit the size of
        // the glTF into the provided constraints.
        val scaleFactor = minOf(scales.width, scales.height, scales.depth)

        // The final layout size is the size of the glTF times the scale factor coerced into the
        // provided constraints.
        val finalSize =
            constraints.map(intrinsicSize) { value, min, max ->
                (value * scaleFactor).roundToInt().coerceIn(min, max)
            }

        return layout(finalSize.width, finalSize.height, finalSize.depth) {}
    }

    private fun IntVolumeSize.toFloatSize3d() =
        FloatSize3d(width = width.toFloat(), height = height.toFloat(), depth = depth.toFloat())

    private fun VolumeConstraints.map(
        size: FloatSize3d,
        mapFn: (value: Float, min: Float, max: Float) -> Float,
    ): FloatSize3d =
        FloatSize3d(
            width = mapFn(size.width, minWidth.toFloat(), maxWidth.toFloat()),
            height = mapFn(size.height, minHeight.toFloat(), maxHeight.toFloat()),
            depth = mapFn(size.depth, minDepth.toFloat(), maxDepth.toFloat()),
        )

    private fun VolumeConstraints.map(
        size: IntVolumeSize,
        mapFn: (value: Int, min: Int, max: Int) -> Int,
    ): IntVolumeSize =
        IntVolumeSize(
            width = mapFn(size.width, minWidth, maxWidth),
            height = mapFn(size.height, minHeight, maxHeight),
            depth = mapFn(size.depth, minDepth, maxDepth),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpatialGltfModelMeasurePolicy

        return intrinsicSize == other.intrinsicSize
    }

    override fun hashCode(): Int {
        return intrinsicSize.hashCode()
    }
}
