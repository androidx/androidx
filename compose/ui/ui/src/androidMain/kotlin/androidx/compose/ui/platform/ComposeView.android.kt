/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.content.Context
import android.os.IBinder
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.R
import androidx.compose.ui.UiComposable
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.Owner
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.core.viewtree.getParentOrViewTreeDisjointParent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import java.lang.ref.WeakReference

/**
 * Base class for custom [android.view.View]s implemented using Jetpack Compose UI. Subclasses
 * should implement the [Content] function with the appropriate content. Calls to [addView] and its
 * variants and overloads will fail with [IllegalStateException].
 *
 * By default, the composition is disposed according to [ViewCompositionStrategy.Default]. Call
 * [disposeComposition] to dispose of the underlying composition earlier, or if the view is never
 * initially attached to a window. (The requirement to dispose of the composition explicitly in the
 * event that the view is never (re)attached is temporary.)
 *
 * [AbstractComposeView] only supports being added into view hierarchies propagating
 * [LifecycleOwner] and [SavedStateRegistryOwner] via [androidx.lifecycle.setViewTreeLifecycleOwner]
 * and [androidx.savedstate.setViewTreeSavedStateRegistryOwner]. In most cases you will already have
 * it set up correctly as [androidx.activity.ComponentActivity], [androidx.fragment.app.Fragment]
 * and [androidx.navigation.NavController] will provide the correct values.
 */
abstract class AbstractComposeView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ViewGroup(context, attrs, defStyleAttr) {

    init {
        clipChildren = false
        clipToPadding = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    /**
     * The first time we successfully locate this we'll save it here. If this View moves to the
     * [android.view.ViewOverlay] we won't be able to find view tree dependencies; this happens when
     * using transition APIs to animate views out in particular.
     *
     * We only ever set this when we're attached to a window.
     */
    private var cachedViewTreeCompositionContext: WeakReference<CompositionContext>? = null

    /**
     * The [getWindowToken] of the window this view was last attached to. If we become attached to a
     * new window we clear [cachedViewTreeCompositionContext] so that we might appeal to the
     * (possibly lazily created) [windowRecomposer] if [findViewTreeCompositionContext] can't locate
     * one instead of using the previous [cachedViewTreeCompositionContext].
     */
    private var previousAttachedWindowToken: IBinder? = null
        set(value) {
            if (field !== value) {
                field = value
                cachedViewTreeCompositionContext = null
            }
        }

    private var composition: Composition? = null

    /**
     * The explicitly set [CompositionContext] to use as the parent of compositions created for this
     * view. Set by [setParentCompositionContext].
     *
     * If set to a non-null value [cachedViewTreeCompositionContext] will be cleared.
     */
    private var parentContext: CompositionContext? = null
        set(value) {
            if (field !== value) {
                field = value
                if (value != null) {
                    cachedViewTreeCompositionContext = null
                }
                val old = composition
                if (old !== null) {
                    old.dispose()
                    composition = null

                    // Recreate the composition now if we are attached.
                    if (isAttachedToWindow) {
                        ensureCompositionCreated()
                    }
                }
            }
        }

    /**
     * [ComposeViewContext] used by this [ComposeView]. This can be set to allow a [ComposeView] to
     * compose its content when not attached to the view hierarchy. Changing this to `null` will
     * result in any existing composition being disposed.
     */
    internal var composeViewContext: ComposeViewContext? = null
        set(value) {
            val existing = field
            if (existing !== value) {
                if (value == null) {
                    disposeComposition()
                } else if (isNotEmpty()) {
                    val child = getChildAt(0) as? AndroidComposeView
                    child?.composeViewContext = value
                }
                field = value
            }
        }

    /**
     * Set the [CompositionContext] that should be the parent of this view's composition. If
     * [parent] is `null` it will be determined automatically from the window the view is attached
     * to.
     */
    fun setParentCompositionContext(parent: CompositionContext?) {
        parentContext = parent
    }

    // Leaking `this` during init is generally dangerous, but we know that the implementation of
    // this particular ViewCompositionStrategy is not going to do something harmful with it.
    @Suppress("LeakingThis")
    private var disposeViewCompositionStrategy: (() -> Unit)? =
        ViewCompositionStrategy.Default.installFor(this)

    /**
     * Set the strategy for managing disposal of this View's internal composition. Defaults to
     * [ViewCompositionStrategy.Default].
     *
     * This View's composition is a live resource that must be disposed to ensure that long-lived
     * references to it do not persist
     *
     * See [ViewCompositionStrategy] for more information.
     */
    fun setViewCompositionStrategy(strategy: ViewCompositionStrategy) {
        disposeViewCompositionStrategy?.invoke()
        disposeViewCompositionStrategy = strategy.installFor(this)
    }

    /**
     * If `true`, this View's composition will be created when it becomes attached to a window for
     * the first time. Defaults to `true`.
     *
     * Subclasses may choose to override this property to prevent this eager initial composition in
     * cases where the view's content is not yet ready. Initial composition will still occur when
     * this view is first measured.
     */
    protected open val shouldCreateCompositionOnAttachedToWindow: Boolean
        get() = true

    /**
     * Enables the display of visual layout bounds for the Compose UI content of this view. This is
     * typically configured using the system developer setting for "Show layout bounds."
     */
    @OptIn(InternalCoreApi::class)
    @InternalComposeUiApi
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    var showLayoutBounds: Boolean = false
        set(value) {
            field = value
            getChildAt(0)?.let { (it as Owner).showLayoutBounds = value }
        }

    /**
     * Controls behavior for how focus should be automatically cleared for this [ComposeView] when
     * responding to input. The default value is [AutoClearFocusBehavior.Default].
     *
     * This property should be set prior to first composition.
     */
    var autoClearFocusBehavior: AutoClearFocusBehavior
        get() =
            getTag(R.id.auto_clear_focus_behavior_tag) as? AutoClearFocusBehavior
                ?: AutoClearFocusBehavior.Default
        set(value) {
            setTag(R.id.auto_clear_focus_behavior_tag, value)
        }

    /**
     * The Jetpack Compose UI content for this view. Subclasses must implement this method to
     * provide content. Initial composition will occur when the view becomes attached to a window or
     * when [createComposition] is called, whichever comes first.
     */
    @Composable @UiComposable abstract fun Content()

    /**
     * Perform initial composition for this view. Once this method is called or the view becomes
     * attached to a window, either [disposeComposition] must be called or the [LifecycleOwner]
     * returned by [findViewTreeLifecycleOwner] must reach the [Lifecycle.State.DESTROYED] state for
     * the composition to be cleaned up properly. (This restriction is temporary.)
     *
     * If this method is called when the composition has already been created it has no effect.
     *
     * This method should only be called if this view [isAttachedToWindow] or if a parent
     * [CompositionContext] has been [set][setParentCompositionContext] explicitly.
     */
    fun createComposition() {
        check(
            parentContext != null ||
                isAttachedToWindow ||
                (composeViewContext != null && composeViewContext?.view?.isAttachedToWindow == true)
        ) {
            "createComposition requires either a parent reference or the View to be attached" +
                "to a window. Attach the View or call setParentCompositionReference."
        }
        ensureCompositionCreated()
    }

    /**
     * Perform initial composition for this view, even if this view isn't attached to the hierarchy.
     * If the view is never attached to the hierarchy, [disposeComposition] must be called for the
     * composition to be cleaned up properly. If the view is attached to the hierarchy after
     * [createComposition], detaching it will clean up the composition, so calling
     * [disposeComposition] is unnecessary.
     *
     * If this method is called when the composition has already been created, it will update the
     * [composeViewContext] being used for the composition.
     *
     * The [composeViewContext] values override any previously set [setParentCompositionContext]
     * value. The [composeViewContext] values are used for all composition information, including
     * [LifecycleOwner], [SavedStateRegistryOwner], and window information pulled from the
     * [ComposeViewContext]'s attached View.
     *
     * @param composeViewContext The [ComposeViewContext] to use for the composition. The
     *   [ComposeViewContext.view] must be attached to the hierarchy.
     */
    internal fun createComposition(composeViewContext: ComposeViewContext) {
        check(composeViewContext.view.isAttachedToWindow) {
            "createComposition requires the ComposeViewContext's view to be attached to a window."
        }
        this.composeViewContext = composeViewContext
        ensureCompositionCreated()
    }

    private var creatingComposition = false

    private fun checkAddView() {
        if (!creatingComposition) {
            throw UnsupportedOperationException(
                "Cannot add views to " +
                    "${javaClass.simpleName}; only Compose content is supported"
            )
        }
    }

    /**
     * `true` if the [CompositionContext] can be considered to be "alive" for the purposes of
     * locally caching it in case the view is placed into a ViewOverlay. [Recomposer]s that are in
     * the [Recomposer.State.ShuttingDown] state or lower should not be cached or reusedif currently
     * cached, as they will never recompose content.
     */
    private val CompositionContext.isAlive: Boolean
        get() = this !is Recomposer || currentState.value > Recomposer.State.ShuttingDown

    /**
     * Cache this [CompositionContext] in [cachedViewTreeCompositionContext] if it [isAlive] and
     * return the [CompositionContext] itself either way.
     */
    private fun CompositionContext.cacheIfAlive(): CompositionContext = also { context ->
        context.takeIf { it.isAlive }?.let { cachedViewTreeCompositionContext = WeakReference(it) }
    }

    /**
     * Determine the correct [CompositionContext] to use as the parent of this view's composition.
     * This can result in caching a looked-up [CompositionContext] for use later. See
     * [cachedViewTreeCompositionContext] for more details.
     *
     * If [cachedViewTreeCompositionContext] is available but [findViewTreeCompositionContext]
     * cannot find a parent context, we will use the cached context if present before appealing to
     * the [windowRecomposer], as [windowRecomposer] can lazily create a recomposer. If we're
     * reattached to the same window and [findViewTreeCompositionContext] can't find the context
     * that [windowRecomposer] would install, we might be in the [getOverlay] of some part of the
     * view hierarchy to animate the disappearance of this and other views. We still need to be able
     * to compose/recompose in this state without creating a brand new recomposer to do it, as well
     * as still locate any view tree dependencies.
     */
    private fun resolveParentCompositionContext() =
        parentContext
            ?: findViewTreeCompositionContext()?.cacheIfAlive()
            ?: cachedViewTreeCompositionContext?.get()?.takeIf { it.isAlive }
            ?: windowRecomposer.cacheIfAlive()

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("DEPRECATION") // Still using ViewGroup.setContent for now
    private fun ensureCompositionCreated() {
        if (composition == null) {
            try {
                creatingComposition = true
                val composeViewContext = composeViewContext
                val effectiveComposeViewContext =
                    if (composeViewContext == null) {
                        val existingContext =
                            if (isEmpty()) null
                            else (getChildAt(0) as? AndroidComposeView)?.composeViewContext
                        val contextView = findViewTreeComposeViewRoot()
                        val foundComposeViewContext = contextView.composeViewContext
                        if (foundComposeViewContext == null) {
                            // Create one and store it for future create calls
                            val createdContext =
                                ComposeViewContext(
                                    compositionContext = resolveParentCompositionContext(),
                                    lifecycleOwner =
                                        contextView.findViewTreeLifecycleOwner()
                                            ?: existingContext?.lifecycleOwner
                                            ?: throw IllegalStateException(
                                                "Composed into the View which doesn't propagate ViewTreeLifecycleOwner!"
                                            ),
                                    savedStateRegistryOwner =
                                        contextView.findViewTreeSavedStateRegistryOwner()
                                            ?: existingContext?.savedStateRegistryOwner
                                            ?: throw IllegalStateException(
                                                "Composed into the View which doesn't propagate ViewTreeSavedStateRegistryOwner!"
                                            ),
                                    viewModelStoreOwner =
                                        contextView.findViewTreeViewModelStoreOwner()
                                            ?: existingContext?.viewModelStoreOwner,
                                    view = contextView,
                                )
                            contextView.composeViewContext = createdContext
                            createdContext
                        } else {
                            updateAutoCreatedComposeViewContext(
                                contextView,
                                foundComposeViewContext,
                            )
                        }
                    } else {
                        composeViewContext
                    }
                composition = setContent(effectiveComposeViewContext) { Content() }
            } finally {
                creatingComposition = false
            }
        }
    }

    private fun updateAutoCreatedComposeViewContext(
        contextView: View,
        existingContext: ComposeViewContext,
    ): ComposeViewContext {
        val newContext = resolveParentCompositionContext()
        val lifecycleOwner = contextView.findViewTreeLifecycleOwner()
        val viewModelStoreOwner = contextView.findViewTreeViewModelStoreOwner()
        val savedStateRegistryOwner = contextView.findViewTreeSavedStateRegistryOwner()
        if (
            newContext === existingContext.compositionContext &&
                lifecycleOwner === existingContext.lifecycleOwner &&
                viewModelStoreOwner === existingContext.viewModelStoreOwner &&
                savedStateRegistryOwner === existingContext.savedStateRegistryOwner
        ) {
            // No changes
            return existingContext
        }
        val createdContext =
            ComposeViewContext(
                compositionContext = newContext,
                lifecycleOwner = lifecycleOwner ?: existingContext.lifecycleOwner,
                savedStateRegistryOwner =
                    savedStateRegistryOwner ?: existingContext.savedStateRegistryOwner,
                viewModelStoreOwner = viewModelStoreOwner,
                view = contextView,
            )
        contextView.composeViewContext = createdContext
        return createdContext
    }

    /**
     * Dispose of the underlying composition and [requestLayout]. A new composition will be created
     * if [createComposition] is called or when needed to lay out this view.
     */
    fun disposeComposition() {
        val child = getChildAt(0) as? AndroidComposeView
        child?.removeConnectionToComposeViewContext()
        composition?.dispose()
        composition = null
        requestLayout()
    }

    /**
     * `true` if this View is host to an active Compose UI composition. An active composition may
     * consume resources.
     */
    val hasComposition: Boolean
        get() = composition != null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // When the ComposeView is in an overlay, due to a transition, it won't have had
        // setParentOrViewTreeDisjointParent() called yet. It has to wait until after the attach
        // finishes, so we have to post to delay the attachedToWindow() logic. contentChild.parent
        // is false when it is part of the ViewOverlay
        if (this.contentChild.parent == null) {
            handler.postAtFrontOfQueue { attachedToWindow() }
        } else {
            attachedToWindow()
        }
    }

    private fun attachedToWindow() {
        previousAttachedWindowToken = windowToken
        if (composeViewContext == null) {
            val child = if (isEmpty()) null else getChildAt(0) as? AndroidComposeView
            if (child != null) {
                val composeViewContext = child.composeViewContext
                child.composeViewContext =
                    updateAutoCreatedComposeViewContext(
                        findViewTreeComposeViewRoot(),
                        composeViewContext,
                    )
            }
        }

        if (shouldCreateCompositionOnAttachedToWindow) {
            ensureCompositionCreated()
        }
    }

    final override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        ensureCompositionCreated()
        internalOnMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @Suppress("WrongCall")
    internal open fun internalOnMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = getChildAt(0)
        if (child == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val width = maxOf(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight)
        val height = maxOf(0, MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom)
        child.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec)),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)),
        )
        setMeasuredDimension(
            child.measuredWidth + paddingLeft + paddingRight,
            child.measuredHeight + paddingTop + paddingBottom,
        )
    }

    final override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) =
        internalOnLayout(changed, left, top, right, bottom)

    internal open fun internalOnLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        getChildAt(0)
            ?.layout(
                paddingLeft,
                paddingTop,
                right - left - paddingRight,
                bottom - top - paddingBottom,
            )
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        // Force the single child for our composition to have the same LayoutDirection
        // that we do. We will get onRtlPropertiesChanged eagerly as the value changes,
        // but the composition child view won't until it measures. This can be too late
        // to catch the composition pass for that frame, so propagate it eagerly.
        getChildAt(0)?.layoutDirection = layoutDirection
    }

    // Transition group handling:
    // Both the framework and androidx transition APIs use isTransitionGroup as a signal for
    // determining view properties to capture during a transition. As AbstractComposeView uses
    // a view subhierarchy to perform its work but operates as a single unit, mark instances as
    // transition groups by default.
    // This is implemented as overridden methods instead of setting isTransitionGroup = true in
    // the constructor so that values set explicitly by xml inflation performed by the ViewGroup
    // constructor will take precedence. As of this writing all known framework implementations
    // use the public isTransitionGroup method rather than checking the internal ViewGroup flag
    // to determine behavior, making this implementation a slight compatibility risk for a
    // tradeoff of cleaner View-consumer API behavior without the overhead of performing an
    // additional obtainStyledAttributes call to determine a value potentially overridden from xml.

    private var isTransitionGroupSet = false

    override fun isTransitionGroup(): Boolean = !isTransitionGroupSet || super.isTransitionGroup()

    override fun setTransitionGroup(isTransitionGroup: Boolean) {
        super.setTransitionGroup(isTransitionGroup)
        isTransitionGroupSet = true
    }

    // Below: enforce restrictions on adding child views to this ViewGroup

    override fun addView(child: View?) {
        checkAddView()
        super.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        checkAddView()
        super.addView(child, index)
    }

    override fun addView(child: View?, width: Int, height: Int) {
        checkAddView()
        super.addView(child, width, height)
    }

    override fun addView(child: View?, params: LayoutParams?) {
        checkAddView()
        super.addView(child, params)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        checkAddView()
        super.addView(child, index, params)
    }

    override fun addViewInLayout(child: View?, index: Int, params: LayoutParams?): Boolean {
        checkAddView()
        return super.addViewInLayout(child, index, params)
    }

    override fun addViewInLayout(
        child: View?,
        index: Int,
        params: LayoutParams?,
        preventRequestLayout: Boolean,
    ): Boolean {
        checkAddView()
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }

    override fun shouldDelayChildPressedState(): Boolean = false
}

/**
 * A [android.view.View] that can host Jetpack Compose UI content. Use [setContent] to supply the
 * content composable function for the view.
 *
 * By default, the composition is disposed according to [ViewCompositionStrategy.Default]. Call
 * [disposeComposition] to dispose of the underlying composition earlier, or if the view is never
 * initially attached to a window. (The requirement to dispose of the composition explicitly in the
 * event that the view is never (re)attached is temporary.)
 *
 * [ComposeView] only supports being added into view hierarchies propagating [LifecycleOwner] and
 * [SavedStateRegistryOwner] via [androidx.lifecycle.setViewTreeLifecycleOwner] and
 * [androidx.savedstate.setViewTreeSavedStateRegistryOwner]. In most cases you will already have it
 * set up correctly as [androidx.activity.ComponentActivity], [androidx.fragment.app.Fragment] and
 * [androidx.navigation.NavController] will provide the correct values.
 */
class ComposeView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AbstractComposeView(context, attrs, defStyleAttr) {

    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke()
    }

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    /**
     * Set the Jetpack Compose UI content for this view. Initial composition will occur when the
     * view becomes attached to a window or when [createComposition] is called, whichever comes
     * first.
     */
    fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = content
        if (isAttachedToWindow || composeViewContext != null) {
            createComposition()
        }
    }

    /** Here to allow extension functions */
    companion object
}

/**
 * Flag to disable WindowInsetsRulers. System UI needs to disable WindowInsets Rulers for all
 * ComposeViews, so this is a global switch. We don't want to have them add a ComposeView and the
 * new ComposeView suddenly requests WindowInsets updates, changing the behavior so that the insets
 * suddenly notify.
 */
internal var areWindowInsetsRulersEnabled = true

/**
 * Used to disable [androidx.compose.ui.layout.WindowInsetsRulers]. This can be used when UI never
 * reads WindowInsets across the process and having WindowInsets callbacks cause frame generation
 * when no content is updated. Applications typically would not use this method, but it may be
 * necessary for system UI. This should be called before the first [ComposeView] is created to avoid
 * insets calls.
 */
@ExperimentalComposeUiApi
fun ComposeView.Companion.disableWindowInsetsRulers() {
    areWindowInsetsRulersEnabled = false
}

@OptIn(ExperimentalComposeUiApi::class)
private fun View.findViewTreeComposeViewRoot(): View {
    if (!isAttachedToWindow || !AndroidComposeUiFlags.isSharedComposeViewContextEnabled) return this

    val lifecycleOwnerDepth =
        findDepthToTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner)
    val savedStateRegistryOwnerDepth =
        findDepthToTag(androidx.savedstate.R.id.view_tree_saved_state_registry_owner)
    val maxDepth = minOf(lifecycleOwnerDepth, savedStateRegistryOwnerDepth)

    // Look for the View that has the lifecycle owner
    var grandPreviousView: View = this
    var previousView: View = this
    var currentView: View? = this
    var depth = 0
    while (currentView != null) {
        if (depth == maxDepth) {
            // Try not to return the DecorView because its context may not be set as we want
            if (currentView.parent !is ViewGroup) {
                return previousView
            }
            return currentView
        }
        val composeViewContext = currentView.composeViewContext
        if (composeViewContext != null) {
            return currentView
        }

        depth++
        val parent = currentView.getParentOrViewTreeDisjointParent() as? View
        grandPreviousView = previousView
        previousView = currentView
        currentView = parent
    }
    // Try not to return the DecorView because its context may not be set as we want
    return grandPreviousView
}

/**
 * Finds the highest depth of View with the same value of [tag] set on it as the lowest View with
 * [tag] set on it. This walks the up tree to the first View with [tag] and will continue to walk up
 * the tree until a different [tag] value is set. Then the maximum depth of the View with the same
 * [tag] value will be returned. A depth of 0 indicates that this [View] has a [tag] value and its
 * ancestors don't have a value or have a different value. A value of [Int.MAX_VALUE] indicates that
 * no ancestors have a value for [tag].
 */
private fun View.findDepthToTag(tag: Int): Int {
    var view: View? = this
    var foundTag: Any? = null
    var depth = 0
    var foundDepth = Int.MAX_VALUE
    while (view != null) {
        val tagValue = view.getTag(tag)
        if (tagValue != null) {
            if (foundTag == null) {
                foundTag = tagValue
            } else if (tagValue != foundTag) {
                return foundDepth
            }
            foundDepth = depth
        }
        depth++
        view = view.getParentOrViewTreeDisjointParent() as? View
    }
    return foundDepth
}

/**
 * Returns the [ComposeViewContext] used in this View's part of the hierarchy, or `null` if one
 * cannot be found or it doesn't match the values set for [View.findViewTreeLifecycleOwner] or
 * [View.findViewTreeSavedStateRegistryOwner]. For example, if there is a [View.composeViewContext]
 * set in the hierarchy, [findViewTreeComposeViewContext] on a child of that View will normally
 * return that [ComposeViewContext]. However, if the child is within a Fragment, its
 * [LifecycleOwner] differs from that set in the [View.composeViewContext], so
 * [findViewTreeComposeViewContext] will return `null`.
 *
 * @see View.composeViewContext
 */
internal fun View.findViewTreeComposeViewContext(): ComposeViewContext? {
    return findViewTreeComposeViewRoot().composeViewContext
}

/**
 * The [ComposeViewContext] that should be used for [AbstractComposeView]s in this part of the
 * hierarchy, if they share the same [LifecycleOwner] and [SavedStateRegistryOwner].
 *
 * @see View.findViewTreeComposeViewContext
 */
@Suppress("UNCHECKED_CAST")
internal var View.composeViewContext: ComposeViewContext?
    get() =
        (getTag(R.id.androidx_compose_ui_view_compose_view_context)
                as? WeakReference<ComposeViewContext>)
            ?.get()
    set(value) {
        setTag(R.id.androidx_compose_ui_view_compose_view_context, WeakReference(value))
    }
