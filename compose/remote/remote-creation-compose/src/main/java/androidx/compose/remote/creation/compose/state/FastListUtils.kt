/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

internal inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> {
    val target = ArrayList<R>(size)
    var i = 0
    while (i < size) {
        target.add(transform(get(i)))
        i++
    }
    return target
}

internal inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    var i = 0
    while (i < size) {
        action(get(i))
        i++
    }
}

internal inline fun <T, R> List<T>.fastFold(initial: R, operation: (acc: R, T) -> R): R {
    var accumulator = initial
    var i = 0
    while (i < size) {
        accumulator = operation(accumulator, get(i))
        i++
    }
    return accumulator
}

internal inline fun <T> List<T>.fastAll(predicate: (T) -> Boolean): Boolean {
    var i = 0
    while (i < size) {
        if (!predicate(get(i))) return false
        i++
    }
    return true
}
