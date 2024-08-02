/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.core.app

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import androidx.collection.SimpleArrayMap
import androidx.core.view.KeyEventDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ReportFragment

/**
 * Base class for activities to allow intercepting [KeyEvent] methods in a composable way in core.
 *
 * You most certainly **don't** want to extend this class, but instead extend
 * `androidx.activity.ComponentActivity`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class ComponentActivity : Activity(), LifecycleOwner, KeyEventDispatcher.Component {
    /**
     * Storage for [ExtraData] instances.
     *
     * Note that these objects are not retained across configuration changes
     */
    @Suppress("DEPRECATION")
    private val extraDataMap = SimpleArrayMap<Class<out ExtraData>, ExtraData>()

    /**
     * This is only used for apps that have not switched to Fragments 1.1.0, where this behavior is
     * provided by `androidx.activity.ComponentActivity`.
     */
    @Suppress("LeakingThis") private val lifecycleRegistry = LifecycleRegistry(this)

    /**
     * Store an instance of [ExtraData] for later retrieval by class name via [getExtraData].
     *
     * Note that these objects are not retained across configuration changes
     *
     * @see getExtraData
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Suppress("DEPRECATION")
    @Deprecated("Use {@link View#setTag(int, Object)} with the window's decor view.")
    public open fun putExtraData(extraData: ExtraData) {
        extraDataMap.put(extraData.javaClass, extraData)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReportFragment.injectIfNeededIn(this)
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onSaveInstanceState(outState)
    }

    /**
     * Retrieves a previously set [ExtraData] by class name.
     *
     * @see putExtraData
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    @Deprecated("Use {@link View#getTag(int)} with the window's decor view.")
    public open fun <T : ExtraData> getExtraData(extraDataClass: Class<T>): T? {
        return extraDataMap[extraDataClass] as T?
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    /** @param event */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun superDispatchKeyEvent(event: KeyEvent): Boolean {
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent): Boolean {
        val decor = window.decorView
        return if (KeyEventDispatcher.dispatchBeforeHierarchy(decor, event)) {
            true
        } else super.dispatchKeyShortcutEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val decor = window.decorView
        return if (KeyEventDispatcher.dispatchBeforeHierarchy(decor, event)) {
            true
        } else KeyEventDispatcher.dispatchKeyEvent(this, decor, this, event)
    }

    /**
     * Checks if the internal state should be dump, as some special args are handled by [Activity]
     * itself.
     *
     * Subclasses implementing [Activity.dump] should typically start with:
     * ```
     * override fun dump(
     *   prefix: String,
     *   fd: FileDescriptor?,
     *   writer: PrintWriter,
     *   args: Array<out String>?
     * ) {
     *   super.dump(prefix, fd, writer, args)
     *
     *   if (!shouldDumpInternalState(args)) {
     *     return
     *   }
     *   // dump internal state
     * }
     * ```
     */
    protected fun shouldDumpInternalState(args: Array<String>?): Boolean {
        return !shouldSkipDump(args)
    }

    private fun shouldSkipDump(args: Array<String>?): Boolean {
        if (!args.isNullOrEmpty()) {
            // NOTE: values below arke hardcoded on framework's Activity (like dumpInner())
            when (args[0]) {
                "--autofill" -> return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                "--contentcapture" -> return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                "--translation" -> return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                "--list-dumpables",
                "--dump-dumpable" -> return Build.VERSION.SDK_INT >= 33
            }
        }
        return false
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated(
        """Store the object you want to save directly by using
      {@link View#setTag(int, Object)} with the window's decor view."""
    )
    public open class ExtraData
}
