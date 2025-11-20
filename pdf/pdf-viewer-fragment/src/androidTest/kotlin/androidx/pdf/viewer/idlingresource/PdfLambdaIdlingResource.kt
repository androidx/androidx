/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer.idlingresource

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An [IdlingResource] for counting or polling-based idleness.
 *
 * @param resourceName The resource's name.
 * @param isIdleCondition Optional. Condition to check for polling; `true` means idle.
 */
internal class PdfLambdaIdlingResource(
    private val resourceName: String,
    internal var isIdleCondition: () -> Boolean,
) : IdlingResource {
    private val isIdleNow = AtomicBoolean(false)
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean {
        val idle = isIdleCondition()

        if (idle && !isIdleNow.getAndSet(true)) {
            resourceCallback?.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        resourceCallback = callback
    }

    fun startPolling() {
        isIdleNow.set(false)
    }
}
