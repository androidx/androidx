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

package androidx.glance.appwidget

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

inline fun <reified T : View> View.findChild(noinline pred: (T) -> Boolean) =
    findChild(pred, T::class.java)

inline fun <reified T : View> View.findViewByType() =
    findChild({ true }, T::class.java)

fun <T : View> View.findChild(predicate: (T) -> Boolean, klass: Class<T>): T? {
    try {
        val castView = klass.cast(this)!!
        if (predicate(castView)) {
            return castView
        }
    } catch (e: ClassCastException) {
        // Nothing to do
    }
    if (this !is ViewGroup) {
        return null
    }
    return children.mapNotNull { it.findChild(predicate, klass) }.firstOrNull()
}
