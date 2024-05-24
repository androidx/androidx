/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.MainThread
import androidx.core.app.ActivityOptionsCompat

/**
 * A launcher for a previously-[prepared call][ActivityResultCaller.registerForActivityResult] to
 * start the process of executing an [ActivityResultContract] that takes an [I] as its required
 * input.
 */
abstract class ActivityResultLauncher<I> {
    /**
     * Executes an [ActivityResultContract] given the required [input].
     *
     * This method throws [android.content.ActivityNotFoundException] if there was no Activity found
     * to run the given Intent.
     *
     * @throws android.content.ActivityNotFoundException
     */
    open fun launch(input: I) {
        launch(input, null)
    }

    /**
     * Executes an [ActivityResultContract] given the required [input] and optional [options] for
     * how the Activity should be started.
     *
     * This method throws [android.content.ActivityNotFoundException] if there was no Activity found
     * to run the given Intent.
     *
     * @throws android.content.ActivityNotFoundException
     */
    abstract fun launch(input: I, options: ActivityOptionsCompat?)

    /**
     * Unregisters this launcher, releasing the underlying result callback, and any references
     * captured within it.
     *
     * You should call this if the registry may live longer than the callback registered for this
     * launcher.
     */
    @MainThread abstract fun unregister()

    /** Returns the [ActivityResultContract] that was used to create this launcher. */
    abstract val contract: ActivityResultContract<I, *>
}

/** Convenience method to launch a no-argument registered call without needing to pass in `null`. */
fun ActivityResultLauncher<Void?>.launch(options: ActivityOptionsCompat? = null) {
    launch(null, options)
}

/** Convenience method to launch a no-argument registered call without needing to pass in `Unit`. */
@JvmName("launchUnit")
fun ActivityResultLauncher<Unit>.launch(options: ActivityOptionsCompat? = null) {
    launch(Unit, options)
}
