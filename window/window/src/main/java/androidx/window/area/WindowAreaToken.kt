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

package androidx.window.area

import android.os.IBinder
import androidx.annotation.RestrictTo

/** Identifier wrapper around [IBinder] to identify [WindowArea]s. */
public class WindowAreaToken internal constructor(internal val binder: IBinder) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WindowAreaToken

        return binder.interfaceDescriptor == other.binder.interfaceDescriptor
    }

    override fun hashCode(): Int {
        return binder.hashCode()
    }

    override fun toString(): String {
        return "WindowAreaInfoToken: Descriptor: ${binder.interfaceDescriptor}"
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromBinder(binder: IBinder): WindowAreaToken = WindowAreaToken(binder)
    }
}
