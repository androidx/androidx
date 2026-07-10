/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.view

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Custom idling resource class which checks if a child view has been added to a View Group or not.
 * The added child view is identified using the childMatcher.
 */
class ChildViewAddedIdlingResource(
    private val parentView: ViewGroup,
    private val childMatcher: (View) -> Boolean,
) : IdlingResource {

    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private val isIdle = AtomicBoolean(false)

    override fun getName(): String = "${ChildViewAddedIdlingResource::class.java.simpleName}"

    override fun isIdleNow(): Boolean {
        if (isIdle.get()) return true
        var childFoundAndMatches = false

        parentView.post {
            for (i in 0 until parentView.childCount) {
                val child = parentView.getChildAt(i)
                if (childMatcher(child)) {
                    childFoundAndMatches = true
                    break
                }
            }

            if (childFoundAndMatches) {
                if (isIdle.compareAndSet(false, true)) {
                    resourceCallback?.onTransitionToIdle()
                }
            }
        }
        return isIdle.get()
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
    }
}
