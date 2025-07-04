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

package androidx.xr.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.CoreEntity

/**
 * Base class for custom [SpatialElement]s implemented using Jetpack Compose UI.
 *
 * Subclasses should implement the [Content] function with the appropriate content.
 *
 * Attempts to call [addChild] or its variants/overloads will result in an [IllegalStateException].
 *
 * This class is based on the existing `AbstractComposeView` class in
 * [androidx.compose.ui.platform].
 *
 * @property compositionContext The [CompositionContext] used for compositions of this element's
 *   hierarchy.
 *
 *   If this composition was created as the top level composition in the hierarchy, then the
 *   recomposer will be cancelled if this element is detached from the subspace. Therefore, this
 *   instance should not be shared or reused in other trees.
 *
 * @property rootCoreEntity The [CoreEntity] associated with the root of this composition. This root
 *   CoreEntity will be the parent entity of the entire composition.
 *
 *   It is not necessary for a composition to have a root entity; however, it may be provided to
 *   ensure that the composition is properly parented when it is a sub-composition of another
 *   composition.
 */
internal abstract class AbstractComposeElement(
    internal var compositionContext: CompositionContext? = null,
    internal val rootCoreEntity: CoreEntity? = null,
) : SpatialElement() {

    /**
     * Whether the composition should be created when the element is attached to a
     * [SpatialComposeScene].
     *
     * If `true`, this [SpatialElement]'s composition will be created when it is attached to a
     * [SpatialComposeScene] for the first time. Defaults to `true`.
     *
     * Subclasses may override this property to prevent eager initial composition if the element's
     * content is not yet ready.
     */
    @get:Suppress("GetterSetterNames")
    protected open val shouldCreateCompositionOnAttachedToSpatialComposeScene: Boolean
        get() = true

    private var creatingComposition = false

    private var composition: Composition? = null

    /**
     * The [AndroidComposeSpatialElement] that will be used to host the composition for this
     * element.
     */
    internal val compositionOwner: AndroidComposeSpatialElement = AndroidComposeSpatialElement()

    /**
     * The Jetpack Compose [SubspaceComposable] UI content for this element.
     *
     * Subclasses must implement this method to provide content. Initial composition will occur when
     * the element is attached to a [SpatialComposeScene] or when [createComposition] is called,
     * whichever comes first.
     */
    @Composable @SubspaceComposable protected abstract fun Content()

    override fun addChild(element: SpatialElement) {
        if (!creatingComposition) {
            throw UnsupportedOperationException(
                "May only add $element to $this during composition."
            )
        }

        super.addChild(element)
    }

    override fun onAttachedToSubspace(spatialComposeScene: SpatialComposeScene) {
        super.onAttachedToSubspace(spatialComposeScene)

        if (shouldCreateCompositionOnAttachedToSpatialComposeScene) {
            createComposition()
        }
    }

    /**
     * Performs the initial composition for this element.
     *
     * This method has no effect if the composition has already been created.
     *
     * This method should only be called if this element is attached to a [SpatialComposeScene] or
     * if a parent [CompositionContext] has been set explicitly.
     */
    @OptIn(InternalComposeUiApi::class)
    protected fun createComposition() {
        if (composition != null) return

        check(isAttachedToSpatialComposeScene) {
            "Element.createComposition() requires the Element to be attached to the subspace."
        }
        check(!hasAncestorWithCompositionContext()) {
            "Cannot construct a composition for $this element. The tree it is currently attached " +
                "to has another composition context."
        }
        check(children.isEmpty()) {
            "Cannot set the composable content. $parent element already contains a subtree."
        }

        creatingComposition = true

        GlobalSnapshotManager.ensureStarted()
        addChild(compositionOwner)
        compositionOwner.root.coreEntity = rootCoreEntity
        composition =
            WrappedComposition(
                    compositionOwner,
                    compositionContext
                        ?: SubspaceRecomposerPolicy.createAndInstallSubspaceRecomposer(this),
                )
                .also {
                    compositionOwner.wrappedComposition = it
                    it.setContent { Content() }
                }

        creatingComposition = false
    }

    /** Whether any of the ancestor elements have a [CompositionContext]. */
    private fun hasAncestorWithCompositionContext(): Boolean =
        generateSequence(parent) { it.parent }
            .any { it is AbstractComposeElement && it.compositionContext != null }

    /**
     * Disposes the composition for this element.
     *
     * This method has no effect if the composition has already been disposed.
     */
    public fun disposeComposition() {
        composition?.dispose()
        composition = null
    }
}

/**
 * An [SpatialElement] that can host Jetpack Compose [SubspaceComposable] content.
 *
 * Use [setContent] to provide the content composable function for the element.
 *
 * This class is based on the existing `ComposeView` class in [androidx.compose.ui.platform].
 *
 * @param scene The [SpatialComposeScene] that this element is attached to.
 * @param compositionContext the [CompositionContext] from a parent composition to propagate
 *   composition state. Should be `null` when this instance is the top-level composition context, in
 *   which case a new [CompositionContext] will be created. This value should be provided when this
 *   instance is a sub-composition of another composition.
 * @param rootCoreEntity The [CoreEntity] associated with the root layout of this composition (see
 *   [AbstractComposeElement.rootCoreEntity])
 */
internal class SpatialComposeElement(
    scene: SpatialComposeScene,
    compositionContext: CompositionContext? = null,
    rootCoreEntity: CoreEntity? = null,
) : AbstractComposeElement(compositionContext, rootCoreEntity) {
    init {
        spatialComposeScene = scene
    }

    public var rootVolumeConstraints
        get() = compositionOwner.rootVolumeConstraints
        set(value) {
            compositionOwner.rootVolumeConstraints = value
        }

    private val content = mutableStateOf<(@Composable @SubspaceComposable () -> Unit)?>(null)

    @get:Suppress("GetterSetterNames")
    override var shouldCreateCompositionOnAttachedToSpatialComposeScene: Boolean = false
        private set

    @Composable
    @SubspaceComposable
    override fun Content() {
        content.value?.invoke()
    }

    /**
     * Sets the Jetpack Compose [SubspaceComposable] UI content for this element.
     *
     * Initial composition will occur when the element is attached to a [SpatialComposeScene] or
     * when [createComposition] is called, whichever comes first.
     *
     * @param content the composable content to display in this element.
     */
    public fun setContent(content: @Composable @SubspaceComposable () -> Unit) {
        shouldCreateCompositionOnAttachedToSpatialComposeScene = true

        this.content.value = content

        if (isAttachedToSpatialComposeScene) {
            createComposition()
        }
    }
}
