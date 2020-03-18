/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.animation.AnimationClockObservable
import androidx.animation.rootAnimationClockFactory
import androidx.annotation.MainThread
import androidx.ui.core.input.FocusManager
import androidx.ui.input.TextInputService
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.FrameManager
import androidx.compose.NeverEqual
import androidx.compose.Providers
import androidx.compose.StructurallyEqual
import androidx.compose.ambientOf
import androidx.compose.remember
import androidx.compose.state
import androidx.compose.staticAmbientOf
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.selection.SelectionContainer
import androidx.ui.node.UiComposer
import androidx.ui.savedinstancestate.UiSavedStateRegistryAmbient
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import org.jetbrains.annotations.TestOnly
import java.util.WeakHashMap
import kotlin.coroutines.CoroutineContext

private val TAG_COMPOSITION = "androidx.compose.Composition".hashCode()
private val ROOT_COMPONENTNODES = WeakHashMap<ComponentNode, Composition>()
private val ROOT_VIEWGROUPS = WeakHashMap<ViewGroup, Composition>()

/**
 * Apply Code Changes will invoke the two functions before and after a code swap.
 *
 * This forces the whole view hierarchy to be redrawn to invoke any code change that was
 * introduce in the code swap.
 *
 * All these are private as within JVMTI / JNI accessibility is mostly a formality.
 */
// NOTE(lmr): right now, this class only takes into account Emittables and Views composed using
// compose. In reality, there might be more (ie, Vectors), and we should figure out a more
// holistic way to capture those as well.
private class HotReloader {
    companion object {
        private var compositions = mutableListOf<Pair<Composition, @Composable() () -> Unit>>()

        @TestOnly
        fun clearRoots() {
            ROOT_COMPONENTNODES.clear()
            ROOT_VIEWGROUPS.clear()
        }

        // Called before Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun saveStateAndDispose(context: Any) {
            compositions.clear()

            val componentNodes = ROOT_COMPONENTNODES.entries.toSet()

            for ((_, composition) in componentNodes) {
                compositions.add(composition to composition.composable)
            }
            for ((_, composition) in componentNodes) {
                if (composition.isRoot) {
                    composition.dispose()
                }
            }

            val viewRoots = ROOT_VIEWGROUPS.entries.toSet()

            for ((_, composition) in viewRoots) {
                compositions.add(composition to composition.composable)
            }
            for ((_, composition) in viewRoots) {
                if (composition.isRoot) {
                    composition.dispose()
                }
            }
        }

        // Called after Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun loadStateAndCompose(context: Any) {
            for ((composition, composable) in compositions) {
                composition.composable = composable
            }

            for ((composition, composable) in compositions) {
                if (composition.isRoot) {
                    composition.compose(composable)
                }
            }

            compositions.clear()
        }

        @TestOnly
        internal fun simulateHotReload(context: Any) {
            saveStateAndDispose(context)
            loadStateAndCompose(context)
        }
    }
}

/**
 * @suppress
 */
@TestOnly
fun simulateHotReload(context: Any) = HotReloader.simulateHotReload(context)

/**
 * @suppress
 */
@TestOnly
fun clearRoots() = HotReloader.clearRoots()

internal fun findComposition(view: View): Composition? {
    return view.getTag(TAG_COMPOSITION) as? Composition
}

internal fun storeComposition(view: View, composition: Composition) {
    view.setTag(TAG_COMPOSITION, composition)
    if (view is ViewGroup)
        ROOT_VIEWGROUPS[view] = composition
}

internal fun removeRoot(view: View) {
    view.setTag(TAG_COMPOSITION, null)
    if (view is ViewGroup)
        ROOT_VIEWGROUPS.remove(view)
}

internal fun findComposition(node: ComponentNode): Composition? {
    return ROOT_COMPONENTNODES[node]
}

private fun storeComposition(node: ComponentNode, composition: Composition) {
    ROOT_COMPONENTNODES[node] = composition
}

/**
 * Composes the children of the view with the passed in [composable].
 *
 * @see setViewContent
 * @see Composition.dispose
 */
// TODO: Remove this API when View/ComponentNode mixed trees work
fun ViewGroup.setViewContent(
    parent: CompositionReference? = null,
    composable: @Composable() () -> Unit
): Composition {
    val composition = findComposition(this)
        ?: UiComposition(this, context, parent).also {
            removeAllViews()
        }
    composition.compose(composable)
    return composition
}

/**
 * Sets the contentView of an activity to a FrameLayout, and composes the contents of the layout
 * with the passed in [composable].
 *
 * @see setContent
 * @see Activity.setContentView
 */
// TODO: Remove this API when View/ComponentNode mixed trees work
fun Activity.setViewContent(composable: @Composable() () -> Unit): Composition {
    // TODO(lmr): add ambients here, or remove API entirely if we can
    // If there is already a FrameLayout in the root, we assume we want to compose
    // into it instead of create a new one. This allows for `setContent` to be
    // called multiple times.
    val root = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
        ?: FrameLayout(this).also { setContentView(it) }
    return root.setViewContent(null, composable)
}

/**
 * @suppress
 */
@TestOnly
fun makeCompositionForTesting(
    root: Any,
    context: Context,
    parent: CompositionReference? = null
): Composition = UiComposition(
    root,
    context,
    parent
)

internal class UiComposition(
    private val root: Any,
    private val context: Context,
    parent: CompositionReference? = null
) : Composition(
    { slots, recomposer ->
        UiComposer(
            context,
            root,
            slots,
            recomposer
        )
    },
    parent
) {
    var enabled: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                if (value && disabledComposition != null) {
                    compose(disabledComposition!!)
                    disabledComposition = null
                }
            }
        }

    private var disabledComposition: (@Composable() () -> Unit)? = null

    override fun compose(content: @Composable() () -> Unit) {
        if (!enabled) {
            disabledComposition = content
        } else {
            super.compose(content)
        }
    }

    init {
        when (root) {
            is ViewGroup -> storeComposition(root, this)
            is ComponentNode -> storeComposition(root, this)
        }
    }

    override fun dispose() {
        super.dispose()
        disabledComposition = null
        when (root) {
            is ViewGroup -> removeRoot(root)
            is ComponentNode -> ROOT_COMPONENTNODES.remove(root)
        }
    }
}

// TODO(chuckj): This is a temporary work-around until subframes exist so that
// nextFrame() inside recompose() doesn't really start a new frame, but a new subframe
// instead.
@MainThread
fun subcomposeInto(
    container: ComponentNode,
    context: Context,
    parent: CompositionReference? = null,
    composable: @Composable() () -> Unit
): Composition {
    val composition = findComposition(container)
        ?: UiComposition(container, context, parent)
    composition.compose(composable)
    return composition
}

/**
 * Composes the given composable into the given activity. The composable will become the root view
 * of the given activity.
 *
 * @param content Composable that will be the content of the activity.
 */
fun Activity.setContent(
    content: @Composable() () -> Unit
): Composition {
    FrameManager.ensureStarted()
    val composeView: Owner = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? Owner
        ?: createOwner(this).also {
            if (it is ViewGroup) {
                setContentView(it)
            }
        }

    // TODO(lmr): setup lifecycle-based dispose since we have Activity here

    return doSetContent(composeView, this, content)
}

/**
 * Disposes of a composition of the children of this view. This is a convenience method around
 * [Composition.dispose].
 *
 * @see Composition.dispose
 * @see setContent
 */
@Deprecated(
    "disposing should be done with the Composition object returned by setContent",
    replaceWith = ReplaceWith("Composition#dispose()")
)
fun ViewGroup.disposeComposition() {
    findComposition(this)?.dispose()
}

/**
 * Disposes of a composition that was started using [setContent]. This is a convenience method
 * around [Composition.dispose].
 *
 * @see setContent
 * @see Composition.dispose
 */
@Deprecated(
    "disposing should be done with the Composition object returned by setContent",
    replaceWith = ReplaceWith("Composition#dispose()")
)
fun Activity.disposeComposition() {
    val view = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
        ?: error("No root view found")
    val composition = findComposition(view) ?: error("No composition found")
    composition.dispose()
}

/**
 * We want text/image selection to be enabled by default and disabled per widget. Therefore a root
 * level [SelectionContainer] is installed at the root.
 */
@Composable
private fun WrapWithSelectionContainer(content: @Composable() () -> Unit) {
    SelectionContainer(children = content)
}

/**
 * Composes the given composable into the given view.
 *
 * Note that this [ViewGroup] should have an unique id for the saved instance state mechanism to
 * be able to save and restore the values used within the composition. See [View.setId].
 * @param content Composable that will be the content of the view.
 */
fun ViewGroup.setContent(
    content: @Composable() () -> Unit
): Composition {
    val composeView =
        if (childCount > 0) {
            getChildAt(0) as? Owner
        } else {
            removeAllViews(); null
        }
            ?: createOwner(context).also { if (it is ViewGroup) addView(it) }
    return doSetContent(composeView, context, content)
}

private fun createComposeViewComposition(owner: Owner, context: Context): Composition {
    val composition = UiComposition(owner.root, context, null)
    // we will postpone (disable) the composition until [Owner] restores the state
    if (owner.savedStateRegistry == null) {
        composition.enabled = false
        owner.setOnSavedStateRegistryAvailable {
            composition.enabled = true
        }
    }
    return composition
}

private fun doSetContent(
    owner: Owner,
    context: Context,
    content: @Composable() () -> Unit
): Composition {
    val composition = findComposition(owner.root)
        ?: createComposeViewComposition(owner, context)
    composition.compose {
        WrapWithAmbients(owner, context, Dispatchers.Main) {
            WrapWithSelectionContainer(content)
        }
    }
    return composition
}

@SuppressLint("UnnecessaryLambdaCreation")
@Composable
private fun WrapWithAmbients(
    owner: Owner,
    context: Context,
    coroutineContext: CoroutineContext,
    content: @Composable() () -> Unit
) {
    // TODO(nona): Tie the focus manger lifecycle to Window, otherwise FocusManager won't work
    //             with nested AndroidComposeView case
    val focusManager = remember { FocusManager() }
    var configuration by state(NeverEqual) {
        context.applicationContext.resources.configuration
    }

    // onConfigurationChange is the correct hook to update configuration, however it is
    // possible that the configuration object itself may come from a wrapped
    // context / themed activity, and may not actually reflect the system. So instead we
    // use this hook to grab the applicationContext's configuration, which accurately
    // reflects the state of the application / system.
    owner.configurationChangeObserver = {
        configuration = context.applicationContext.resources.configuration
    }

    // We don't use the attached View's layout direction here since that layout direction may not
    // be resolved since the composables may be composed without attaching to the RootViewImpl.
    // In Jetpack Compose, use the locale layout direction (i.e. layoutDirection came from
    // configuration) as a default layout direction.
    val layoutDirection = when (configuration.layoutDirection) {
        android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
        android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
        // API doc says Configuration#getLayoutDirection only returns LTR or RTL.
        // Fallback to LTR for unexpected return value.
        else -> LayoutDirection.Ltr
    }

    val rootAnimationClock = remember { rootAnimationClockFactory() }
    val savedStateRegistry = requireNotNull(owner.savedStateRegistry)

    Providers(
        ContextAmbient provides context,
        CoroutineContextAmbient provides coroutineContext,
        DensityAmbient provides Density(context),
        FocusManagerAmbient provides focusManager,
        TextInputServiceAmbient provides owner.textInputService,
        FontLoaderAmbient provides owner.fontLoader,
        HapticFeedBackAmbient provides owner.hapticFeedBack,
        AutofillTreeAmbient provides owner.autofillTree,
        ConfigurationAmbient provides configuration,
        OwnerAmbient provides owner,
        LayoutDirectionAmbient provides layoutDirection,
        AnimationClockAmbient provides rootAnimationClock,
        UiSavedStateRegistryAmbient provides savedStateRegistry,
        children = content
    )
}

val ContextAmbient = staticAmbientOf<Context>()

val DensityAmbient = ambientOf<Density>(StructurallyEqual)

val CoroutineContextAmbient = ambientOf<CoroutineContext>()

val ConfigurationAmbient = ambientOf<Configuration>(NeverEqual)

// TODO(b/139866476): The Owner should not be exposed via ambient
val OwnerAmbient = staticAmbientOf<Owner>()

val AutofillAmbient = ambientOf<Autofill?>()

// This will ultimately be replaced by Autofill Semantics (b/138604305).
val AutofillTreeAmbient = staticAmbientOf<AutofillTree>()

val LayoutDirectionAmbient = ambientOf<LayoutDirection>()

val FocusManagerAmbient = ambientOf<FocusManager>()

val TextInputServiceAmbient = staticAmbientOf<TextInputService?>()

val AnimationClockAmbient = staticAmbientOf<AnimationClockObservable>()

val FontLoaderAmbient = staticAmbientOf<Font.ResourceLoader>()

/**
 * The ambient to provide haptic feedback to the user.
 */
val HapticFeedBackAmbient = staticAmbientOf<HapticFeedback>()
