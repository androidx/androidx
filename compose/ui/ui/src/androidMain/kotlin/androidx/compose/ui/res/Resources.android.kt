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

package androidx.compose.ui.res

import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.collection.MutableIntObjectMap

internal class ResourceIdCache {

    private val resIdPathMap = MutableIntObjectMap<TypedValue>()

    /**
     * Resolve the path of the provided resource identifier within the given resources object. This
     * first checks its internal cache before attempting to resolve the resource with the Android
     * resource system
     */
    fun resolveResourcePath(res: Resources, @DrawableRes id: Int): TypedValue {
        synchronized(this) {
            var value = resIdPathMap[id]
            if (value == null) {
                value = TypedValue()
                res.getValue(id, value, true)
                resIdPathMap.put(id, value)
            }
            return value
        }
    }

    fun clear() {
        synchronized(this) { resIdPathMap.clear() }
    }
}
