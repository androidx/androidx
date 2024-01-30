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
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Base class for dialogs that enables composition of higher level components.
 */
open class ComponentDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes themeResId: Int = 0
) : Dialog(context, themeResId),
    LifecycleOwner,
    OnBackPressedDispatcherOwner,
    SavedStateRegistryOwner {

    private var _lifecycleRegistry: LifecycleRegistry? = null
    private val lifecycleRegistry: LifecycleRegistry
        get() = _lifecycleRegistry ?: LifecycleRegistry(this).also {
            _lifecycleRegistry = it
        }

    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onSaveInstanceState(): Bundle {
        val bundle = super.onSaveInstanceState()
        savedStateRegistryController.performSave(bundle)
        return bundle
    }

    @Suppress("ClassVerificationFailure") // needed for onBackInvokedDispatcher call
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

    @Suppress("DEPRECATION")
    final override val onBackPressedDispatcher = OnBackPressedDispatcher {
        super.onBackPressed()
    }

    @CallSuper
    override fun onBackPressed() {
        onBackPressedDispatcher.onBackPressed()
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
     * Sets the view tree owners before setting the content view so that the
     * inflation process and attach listeners will see them already present.
     */
    @CallSuper
    open fun initializeViewTreeOwners() {
        window!!.decorView.setViewTreeLifecycleOwner(this)
        window!!.decorView.setViewTreeOnBackPressedDispatcherOwner(this)
        window!!.decorView.setViewTreeSavedStateRegistryOwner(this)
    }
}
