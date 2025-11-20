/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.activity

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** Base class for dialogs that enables composition of higher level components. */
open class ComponentDialog
@JvmOverloads
constructor(context: Context, @StyleRes themeResId: Int = 0) :
    Dialog(context, themeResId),
    LifecycleOwner,
    OnBackPressedDispatcherOwner,
    NavigationEventDispatcherOwner,
    SavedStateRegistryOwner {

    private var _lifecycleRegistry: LifecycleRegistry? = null
    private val lifecycleRegistry: LifecycleRegistry
        get() = _lifecycleRegistry ?: LifecycleRegistry(this).also { _lifecycleRegistry = it }

    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    // Input from for `ComponentDialog.onBackPressed()`, which can get called when API < 33 or
    // when `android:enableOnBackInvokedCallback` is `false`.
    private val onBackPressedInput: DirectNavigationEventInput by lazy {
        val input = DirectNavigationEventInput()
        navigationEventDispatcher.addInput(input)
        input
    }

    override fun onSaveInstanceState(): Bundle {
        val bundle = super.onSaveInstanceState()
        savedStateRegistryController.performSave(bundle)
        return bundle
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.setOnBackInvokedDispatcher(onBackInvokedDispatcher)
        }
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    @CallSuper
    override fun onStop() {
        // This is the closest thing to onDestroy that a Dialog has
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        _lifecycleRegistry = null
        super.onStop()
    }

    /**
     * Retrieve the [OnBackPressedDispatcher] that will be triggered when [onBackPressed] is called.
     *
     * @return The [OnBackPressedDispatcher] associated with this ComponentDialog.
     */
    @Suppress("DEPRECATION")
    final override val onBackPressedDispatcher: OnBackPressedDispatcher by lazy {
        OnBackPressedDispatcher { @Suppress("DEPRECATION") super.onBackPressed() }
    }

    /**
     * Lazily provides a [NavigationEventDispatcher] for back navigation handling, including support
     * for predictive back gestures introduced in Android 13 (API 33+).
     *
     * This dispatcher acts as the central point for back navigation events. When a navigation event
     * occurs (e.g., a back gesture), it safely invokes [ComponentDialog.onBackPressed].
     */
    override val navigationEventDispatcher: NavigationEventDispatcher
        get() = onBackPressedDispatcher.eventDispatcher

    @CallSuper
    @Deprecated(
        """This method has been deprecated in favor of using the
      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.
      The OnBackPressedDispatcher controls how back button events are dispatched
      to one or more {@link OnBackPressedCallback} objects."""
    )
    override fun onBackPressed() {
        onBackPressedInput.backCompleted()
    }

    override fun setContentView(layoutResID: Int) {
        initializeViewTreeOwners()
        super.setContentView(layoutResID)
    }

    override fun setContentView(view: View) {
        initializeViewTreeOwners()
        super.setContentView(view)
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        initializeViewTreeOwners()
        super.setContentView(view, params)
    }

    override fun addContentView(view: View, params: ViewGroup.LayoutParams?) {
        initializeViewTreeOwners()
        super.addContentView(view, params)
    }

    /**
     * Sets the view tree owners before setting the content view so that the inflation process and
     * attach listeners will see them already present.
     */
    @CallSuper
    open fun initializeViewTreeOwners() {
        window!!.decorView.setViewTreeLifecycleOwner(this)
        window!!.decorView.setViewTreeOnBackPressedDispatcherOwner(this)
        window!!.decorView.setViewTreeSavedStateRegistryOwner(this)
        window!!.decorView.setViewTreeNavigationEventDispatcherOwner(this)
    }
}
