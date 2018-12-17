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

package androidx.ui.foundation.change_notifier

import androidx.ui.foundation.diagnostics.describeIdentity

/**
 * A [ChangeNotifier] that holds a single value.
 *
 * When [value] is replaced, this class notifies its listeners.
 */
open class ValueNotifier<T>(value: T) : ChangeNotifier(), ValueListenable<T> {
    override var value: T = value
        get() = field
        set(value) {
            if (field == value) {
                return
            }
            field = value
            notifyListeners()
        }

    override fun toString(): String = "${describeIdentity(this)}($value)"
}
