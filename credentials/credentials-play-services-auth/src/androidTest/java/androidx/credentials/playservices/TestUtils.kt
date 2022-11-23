/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.playservices

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class TestUtils {
    companion object {
        @JvmStatic
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Suppress("deprecation")
        fun clearFragmentManager(fragmentManager: android.app.FragmentManager) {
            fragmentManager.fragments.forEach { f ->
                fragmentManager.beginTransaction().remove(f)
                    ?.commitAllowingStateLoss()
            }
            Log.i("Test", fragmentManager.fragments.toString())
        // Within fragmentManager.fragments, even after removal of all, this exists by default:
        // [ReportFragment{92dad5d #0 androidx.lifecycle.LifecycleDispatcher.report_fragment_tag}]
        // It will only be removed after an actual fragment is added.
        // This may be due to ActivityScenario simulations of Fragments and FragmentManagers.
        }

        const val EXPECTED_LIFECYCLE_TAG =
            "androidx.lifecycle.LifecycleDispatcher.report_fragment_tag"
    }
}