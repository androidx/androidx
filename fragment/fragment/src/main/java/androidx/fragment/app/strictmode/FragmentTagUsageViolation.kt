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
package androidx.fragment.app.strictmode

import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * See [FragmentStrictMode.Policy.Builder.detectFragmentTagUsage].
 */
class FragmentTagUsageViolation internal constructor(
    fragment: Fragment,
    val parentContainer: ViewGroup?
) : Violation(fragment) {
    /**
     * Gets the parent container that the [Fragment] causing the Violation
     * would have been added to.
     */
    override val message: String
        get() = "Attempting to use <fragment> tag to add fragment $fragment to container " +
            "$parentContainer"
}
