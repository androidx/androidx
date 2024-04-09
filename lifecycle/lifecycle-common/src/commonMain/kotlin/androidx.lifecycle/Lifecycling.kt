/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.annotation.RestrictTo

/**
 * Internal class to handle lifecycle conversion etc.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public expect object Lifecycling {

    public fun lifecycleEventObserver(`object`: Any): LifecycleEventObserver

    /**
     * Create a name for an adapter class.
     */
    public fun getAdapterName(className: String): String
}
