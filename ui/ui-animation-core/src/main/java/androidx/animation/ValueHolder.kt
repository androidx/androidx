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

package androidx.animation

/**
 * A value holder contains two fields: A mutable value that is expected to change throughout an
 * animation, and an immutable value converter
 */
interface ValueHolder<T> {
    /**
     * Value of the [ValueHolder]. This value will be updated by subclasses of [BaseAnimatedValue].
     */
    var value: T
}

/**
 * Creates a [ValueHolder] (of type [T]) with the initial value being [initValue].
 *
 * @param initValue The initial value of the value holder to be created.
 */
fun <T> ValueHolder(initValue: T): ValueHolder<T> =
    object : ValueHolder<T> {
        override var value: T = initValue
    }