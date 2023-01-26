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
package androidx.activity

import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.core.os.BuildCompat
import androidx.core.util.Consumer
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Class for handling [OnBackPressedDispatcher.onBackPressed] callbacks without
 * strongly coupling that implementation to a subclass of [ComponentActivity].
 *
 * This class maintains its own [enabled state][isEnabled]. Only when this callback
 * is enabled will it receive callbacks to [handleOnBackPressed].
 *
 * Note that the enabled state is an additional layer on top of the
 * [androidx.lifecycle.LifecycleOwner] passed to
 * [OnBackPressedDispatcher.addCallback]
 * which controls when the callback is added and removed to the dispatcher.
 *
 * By calling [remove], this callback will be removed from any
 * [OnBackPressedDispatcher] it has been added to. It is strongly recommended
 * to instead disable this callback to handle temporary changes in state.
 *
 * @param enabled The default enabled state for this callback.
 *
 * @see ComponentActivity.getOnBackPressedDispatcher
 */
abstract class OnBackPressedCallback(enabled: Boolean) {
    /**
     * The enabled state of the callback. Only when this callback
     * is enabled will it receive callbacks to [handleOnBackPressed].
     *
     * Note that the enabled state is an additional layer on top of the
     * [androidx.lifecycle.LifecycleOwner] passed to
     * [OnBackPressedDispatcher.addCallback]
     * which controls when the callback is added and removed to the dispatcher.
     */
    @get:MainThread
    @set:MainThread
    @set:OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
    var isEnabled: Boolean = enabled
        set(value) {
            field = value
            if (enabledConsumer != null) {
                enabledConsumer!!.accept(field)
            }
        }

    private val cancellables = CopyOnWriteArrayList<Cancellable>()
    private var enabledConsumer: Consumer<Boolean>? = null

    /**
     * Removes this callback from any [OnBackPressedDispatcher] it is currently
     * added to.
     */
    @MainThread
    fun remove() = cancellables.forEach { it.cancel() }

    /**
     * Callback for handling the [OnBackPressedDispatcher.onBackPressed] event.
     */
    @MainThread
    abstract fun handleOnBackPressed()

    @JvmName("addCancellable")
    internal fun addCancellable(cancellable: Cancellable) {
        cancellables.add(cancellable)
    }

    @JvmName("removeCancellable")
    internal fun removeCancellable(cancellable: Cancellable) {
        cancellables.remove(cancellable)
    }

    @JvmName("setIsEnabledConsumer")
    internal fun setIsEnabledConsumer(isEnabled: Consumer<Boolean>?) {
        enabledConsumer = isEnabled
    }
}
