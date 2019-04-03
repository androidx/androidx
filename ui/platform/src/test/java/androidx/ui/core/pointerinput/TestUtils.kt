/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.pointerinput

import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange

open class MyPointerInputHandler() :
    Function2<PointerInputChange, PointerEventPass, PointerInputChange> {
    var modifyBlock: ((PointerInputChange, PointerEventPass) -> PointerInputChange)? = null
    override fun invoke(p1: PointerInputChange, p2: PointerEventPass): PointerInputChange {
        return modifyBlock?.invoke(p1, p2) ?: p1
    }
}

fun catchThrowable(lambda: () -> Unit): Throwable? {
    var exception: Throwable? = null

    try {
        lambda()
    } catch (theException: Throwable) {
        exception = theException
    }

    return exception
}