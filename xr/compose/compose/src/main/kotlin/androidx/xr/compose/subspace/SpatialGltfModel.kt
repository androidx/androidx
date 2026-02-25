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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
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
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.GltfModelNode
import java.nio.file.Path
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * This composable renders a glTF or .glb model that is loaded asynchronously from the provided
 * source.
 *
 * ### Layout and Sizing
 *
 * The SpatialGltfModel's layout size is determined by the bounding box size of the glTFmodel that
 * is being displayed coerced into the constraints of the layout.
 * - By default, the layout size will match the bounding box of the loaded 3D asset once it is
 *   loaded bounded by the current constraints.
 * - To force the SpatialGltfModel to a specific size, use a size modifier like
 *   SubspaceModifier.size().
 * - The rendered model will be scaled uniformly to fit within the constraints imposed by the layout
 *   and modifiers.
 * - The [content] will be positioned at the center of the SpatialGltfModel by default. The
 *   developer may use GltfModelNode pose and size information to offset content relative to a
 *   desired position. **Note:** Because the model is loaded asynchronously, its intrinsic size will
 *   be zero during initial composition. The layout will be remeasured with the correct size once
 *   the model has finished loading. You can use the [state] parameter to observe the loading status
 *   via [SpatialGltfModelState.status].
 *
 * @param state A [SpatialGltfModelState] object to observe and control the SpatialGltfModel. This
 *   can be created using [rememberSpatialGltfModelState]. The state should be created with a
 *   [SpatialGltfModelSource] that defines where to load the 3D model from. Use the helper functions
 *   [fromPath], [fromUri], or [fromData] to create a [SpatialGltfModelSource].
 * @param modifier The [SubspaceModifier] to be applied to this SpatialGltfModel.
 * @param content The content within the space of the [SpatialGltfModel]
 * @sample androidx.xr.compose.samples.SpatialGltfModelSample
 */
@Composable
@SubspaceComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SpatialGltfModel(
    state: SpatialGltfModelState,
    modifier: SubspaceModifier = SubspaceModifier,
    content: @Composable @SubspaceComposable () -> Unit = {},
) {
    var loadingFailed by remember(state) { mutableStateOf(false) }
    if (loadingFailed) return // Do not proceed until the source has changed.

    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val coreModelEntity = remember { CoreModelEntity() }
    var intrinsicSize by remember { mutableStateOf(IntVolumeSize.Zero) }

    LaunchedEffect(state) {
        supervisorScope {
            launch(
                CoroutineExceptionHandler { _, throwable ->
                    state.setLoadResult(Result.failure(throwable.cause ?: throwable))
                    loadingFailed = true
                }
            ) {
                val model = state.source.createModel(session)
                val entity = GltfModelEntity.create(session, model)
                coreModelEntity.attachEntity(entity)
                state.setLoadResult(Result.success(coreModelEntity))
                intrinsicSize = coreModelEntity.intrinsicSize
            }
        }

        state.watchAnimationState()
    }

    SubspaceLayout(
        modifier = modifier,
        coreEntity = coreModelEntity,
        measurePolicy = SpatialGltfModelMeasurePolicy(intrinsicSize),
        content = content,
    )
}

/** Remembers a [SpatialGltfModelState] object for use with the [SpatialGltfModel] API. */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberSpatialGltfModelState(source: SpatialGltfModelSource): SpatialGltfModelState {
    return remember(source) { SpatialGltfModelState(source) }
}

/**
 * A state object that can be hoisted to observe and control a [SpatialGltfModel].
 *
 * A `SpatialGltfModelState` can be used to query loading and animation status, and to start or stop
 * animations on the associated `SpatialGltfModel`.
 *
 * To create and remember a `SpatialGltfModelState`, use [rememberSpatialGltfModelState].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SpatialGltfModelState(internal val source: SpatialGltfModelSource) {
    private val coreEntityActionQueue = ActionQueue<CoreModelEntity>()

    /** Indicates whether the [SpatialGltfModel] is currently animating or not. */
    public val isAnimating: State<Boolean>
        get() = _isAnimating

    private val _isAnimating: MutableState<Boolean> = mutableStateOf(false)

    /**
     * The current [SpatialGltfModelStatus] of the glTF model.
     *
     * It will initially be [SpatialGltfModelStatus.Loading] until the model is loaded and ready to
     * be displayed at which point it should be [SpatialGltfModelStatus.Loaded]. However, if the
     * model fails to load for any reason, the `status` will be a [SpatialGltfModelStatus.Failed]
     * with the exception that was thrown.
     */
    public val status: State<SpatialGltfModelStatus>
        get() = _status

    public val nodes: List<GltfModelNode>
        get() = _nodes

    private val _nodes: SnapshotStateList<GltfModelNode> = mutableStateListOf()

    private val _status: MutableState<SpatialGltfModelStatus> =
        mutableStateOf(SpatialGltfModelStatus.Loading())

    /**
     * Starts an animation that will play once. If an animation name is not provided then the
     * default animation will play.
     *
     * @param name The name of the animation to play, as defined in the glTF file.
     */
    public fun startAnimation(name: String? = null) {
        coreEntityActionQueue.executeWhenAvailable { it.startAnimation(name) }
    }

    /**
     * Starts an animation that will loop indefinitely. If an animation name is not provided then
     * the default animation will play.
     *
     * @param name The name of the animation to play, as defined in the glTF file.
     */
    public fun loopAnimation(name: String? = null) {
        coreEntityActionQueue.executeWhenAvailable { it.loopAnimation(name) }
    }

    /** Stops all currently playing animations. */
    public fun stopAllAnimations() {
        coreEntityActionQueue.executeWhenAvailable { it.stopAllAnimations() }
    }

    internal fun setLoadResult(result: Result<CoreModelEntity>) {
        result
            .onSuccess { coreEntity ->
                coreEntityActionQueue.value = coreEntity
                _status.value = SpatialGltfModelStatus.Loaded()
                _nodes.clear()
                _nodes.addAll(coreEntity.nodes)
            }
            .onFailure { exception -> _status.value = SpatialGltfModelStatus.Failed(exception) }
    }

    @Suppress("DEPRECATION")
    internal suspend fun watchAnimationState() {
        val entity = suspendCancellableCoroutine { continuation ->
            coreEntityActionQueue.executeWhenAvailable { continuation.resume(it) }
        }

        entity.animationStateFlow.collect { animationState ->
            _isAnimating.value = animationState == GltfModelEntity.AnimationState.PLAYING
        }
    }
}

/**
 * An object that describes and contains information relevant to the current loading state of the
 * glTF model.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SpatialGltfModelStatus private constructor() {

    /** The glTF model is fully loaded and ready to be displayed. */
    public class Loaded() : SpatialGltfModelStatus()

    /** The glTF model is currently loading and is not ready to be displayed. */
    public class Loading() : SpatialGltfModelStatus()

    /**
     * The glTF model has failed to load properly.
     *
     * The [exception] can be inspected to understand why the glTF model has failed to load.
     *
     * @param exception thrown when the glTF model tried to load.
     */
    public class Failed(public val exception: Throwable) : SpatialGltfModelStatus() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Failed) return false

            if (exception != other.exception) return false

            return true
        }

        override fun hashCode(): Int {
            return exception.hashCode()
        }
    }
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

private class SpatialGltfModelMeasurePolicy(private val intrinsicSize: IntVolumeSize) :
    SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {

        val boxSize: IntVolumeSize =
            if (intrinsicSize == IntVolumeSize.Zero) {
                IntVolumeSize(constraints.minWidth, constraints.minHeight, constraints.minDepth)
            } else {
                val scales =
                    constraints.map(intrinsicSize.toFloatSize3d()) { value, min, max ->
                        value.coerceIn(min, max) / value.coerceAtLeast(1f)
                    }
                val scaleFactor = minOf(scales.width, scales.height, scales.depth)

                constraints.map(intrinsicSize) { value, min, max ->
                    (value * scaleFactor).roundToInt().coerceIn(min, max)
                }
            }
        val placeables = measurables.fastMap { it.measure(constraints) }
        return layout(boxSize.width, boxSize.height, boxSize.depth) {
            placeables.fastForEach { placeable -> placeable.place(Pose.Identity) }
        }
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
