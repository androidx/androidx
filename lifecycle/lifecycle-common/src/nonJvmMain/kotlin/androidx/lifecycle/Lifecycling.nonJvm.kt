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
package androidx.lifecycle

import androidx.annotation.RestrictTo

/**
 * Internal class to handle lifecycle conversion etc.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public actual object Lifecycling {
    public actual fun lifecycleEventObserver(`object`: Any): LifecycleEventObserver {
        val isLifecycleEventObserver = `object` is LifecycleEventObserver
        val isDefaultLifecycleObserver = `object` is DefaultLifecycleObserver
        if (isLifecycleEventObserver && isDefaultLifecycleObserver) {
            return DefaultLifecycleObserverAdapter(
                `object` as DefaultLifecycleObserver,
                `object` as LifecycleEventObserver
            )
        }
        if (isDefaultLifecycleObserver) {
            return DefaultLifecycleObserverAdapter(`object` as DefaultLifecycleObserver, null)
        }
        if (isLifecycleEventObserver) {
            return `object` as LifecycleEventObserver
        }
        throw IllegalArgumentException()
    }

    /**
     * Create a name for an adapter class.
     */
    public actual fun getAdapterName(className: String): String {
        return className.replace(".", "_") + "_LifecycleAdapter"
    }
}
