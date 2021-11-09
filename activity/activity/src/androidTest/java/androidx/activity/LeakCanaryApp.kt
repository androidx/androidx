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

import androidx.multidex.MultiDexApplication
import leakcanary.LeakCanary
import shark.AndroidReferenceMatchers
import shark.ReferenceMatcher

class LeakCanaryApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        @Suppress("UNCHECKED_CAST")
        LeakCanary.config = LeakCanary.config.copy(
            referenceMatchers = (
                AndroidReferenceMatchers.appDefaults - AndroidReferenceMatchers
                    .INPUT_METHOD_MANAGER_IS_TERRIBLE
                ) as List<ReferenceMatcher>
        )
    }
}
