/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.lang.IllegalStateException

internal object WindowFinder {

    @UiThread
    fun findWindow(view: View): Window? {
        // Unwrap the ContextWrapper hierarchy to see if its root is an Activity.
        val rootContext = run {
            var current = view.context
            while (current !is Activity && current is ContextWrapper) current = current.baseContext
            current
        }
        if (rootContext is Activity) return rootContext.window

        // If the View is hosted inside a Fragment, it may have a wrapped Context which does not
        // necessarily have an Activity as a direct ancestor. However, the Activity can be accessed
        // through the Fragment itself.
        val fragment =
            try {
                // There is no version of this function that returns null when no Fragment is found
                // for the
                // view, it only throws in that case, so wrap it in a try-catch.
                FragmentManager.findFragment<Fragment>(view)
            } catch (ex: IllegalStateException) {
                // Normally it's a bad idea to catch a RuntimeException like this - do not imitate!
                null
            }
        return fragment?.activity?.window
    }
}
