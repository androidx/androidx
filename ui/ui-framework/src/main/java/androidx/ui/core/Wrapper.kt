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
import androidx.compose.compositionFor
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.input.FocusManager
import androidx.ui.core.input.FocusManagerImpl
import androidx.ui.core.selection.SelectionContainer
import androidx.ui.input.TextInputService
import androidx.ui.node.UiComposer
import androidx.ui.platform.AndroidUriHandler
import androidx.ui.platform.UriHandler
import androidx.ui.savedinstancestate.UiSavedStateRegistryAmbient
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

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
): Composition = compositionFor(
    container = this,
    context = context,
    parent = parent,
    onBeforeFirstComposition = {
        removeAllViews()
    }
).apply {
    setContent(composable)
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

// TODO(chuckj): This is a temporary work-around until subframes exist so that
// nextFrame() inside recompose() doesn't really start a new frame, but a new subframe
// instead.
@MainThread
fun subcomposeInto(
    container: ComponentNode,
    context: Context,
    parent: CompositionReference? = null,
    composable: @Composable() () -> Unit
): Composition = compositionFor(container, context, parent).apply {
    setContent(composable)
}

/**
 * Composes the given composable into the given activity. The composable will become the root view
 * of the given activity.
 *
 * Note: the returned [Composition] object is not guaranteed to stay the same between the
 * invocations so it is not safe to use it as a key.
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
            setContentView(it.view)
        }

    // TODO(lmr): setup lifecycle-based dispose since we have Activity here

    return doSetContent(composeView, this, content)
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
 * Note 1: this [ViewGroup] should have an unique id for the saved instance state mechanism to
 * be able to save and restore the values used within the composition. See [View.setId].
 * Note 2: the returned [Composition] object is not guaranteed to stay the same between the
 * invocations so it is not safe to use it as a key.
 * @param content Composable that will be the content of the view.
 */
fun ViewGroup.setContent(
    content: @Composable() () -> Unit
): Composition {
    FrameManager.ensureStarted()
    val composeView =
        if (childCount > 0) {
            getChildAt(0) as? Owner
        } else {
            removeAllViews(); null
        }
            ?: createOwner(context).also { addView(it.view) }
    return doSetContent(composeView, context, content)
}

private fun doSetContent(
    owner: Owner,
    context: Context,
    content: @Composable() () -> Unit
): Composition {
    val originalComposition = compositionFor(owner.root, context)
    val compositionWrapper = object : Composition {
        private var disposed = false

        override fun setContent(content: @Composable() () -> Unit) {
            if (owner.savedStateRegistry != null) {
                originalComposition.setContent {
                    WrapWithAmbients(owner, context, Dispatchers.Main) {
                        WrapWithSelectionContainer(content)
                    }
                }
            } else {
                // we will postpone the real composition until composeView restores the state.
                owner.setOnSavedStateRegistryAvailable {
                    if (!disposed) {
                        setContent(content)
                    }
                }
            }
        }

        override fun dispose() {
            disposed = true
            originalComposition.dispose()
        }
    }
    compositionWrapper.setContent(content)
    return compositionWrapper
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
    val focusManager = remember { FocusManagerImpl() }
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

    val uriHandler = remember { AndroidUriHandler(context) }

    Providers(
        ContextAmbient provides context,
        CoroutineContextAmbient provides coroutineContext,
        DensityAmbient provides Density(context),
        FocusManagerAmbient provides focusManager,
        TextInputServiceAmbient provides owner.textInputService,
        FontLoaderAmbient provides owner.fontLoader,
        HapticFeedBackAmbient provides owner.hapticFeedBack,
        ClipboardManagerAmbient provides owner.clipboardManager,
        AutofillTreeAmbient provides owner.autofillTree,
        ConfigurationAmbient provides configuration,
        OwnerAmbient provides owner,
        LayoutDirectionAmbient provides layoutDirection,
        AnimationClockAmbient provides rootAnimationClock,
        UiSavedStateRegistryAmbient provides savedStateRegistry,
        UriHandlerAmbient provides uriHandler,
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

val UriHandlerAmbient = staticAmbientOf<UriHandler>()

val ClipboardManagerAmbient = staticAmbientOf<ClipboardManager>()

/**
 * The ambient to provide haptic feedback to the user.
 */
val HapticFeedBackAmbient = staticAmbientOf<HapticFeedback>()

private fun compositionFor(
    container: Any,
    context: Context,
    parent: CompositionReference? = null,
    onBeforeFirstComposition: (() -> Unit)? = null
) = compositionFor(
    container = container,
    parent = parent,
    composerFactory = { slotTable, recomposer ->
        onBeforeFirstComposition?.invoke()
        UiComposer(context, container, slotTable, recomposer)
    }
)
