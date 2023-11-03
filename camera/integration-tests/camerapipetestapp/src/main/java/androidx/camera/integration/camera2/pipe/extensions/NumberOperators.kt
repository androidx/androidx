/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe.extensions

/** Necessary for calculations */
operator fun Number.minus(otherNum: Number): Number {
    return when (this) {
        is Long -> this - otherNum.toLong()
        is Int -> this - otherNum.toInt()
        is Short -> this - otherNum.toShort()
        is Double -> this - otherNum.toDouble()
        is Float -> this - otherNum.toFloat()
        else -> throw RuntimeException(
            "Subtraction between ${this::class.java.simpleName} and " +
                "${otherNum.javaClass.simpleName} is not supported"
        )
    }
}

operator fun Number.compareTo(otherNum: Number): Int {
    return when (this) {
        is Long -> this.compareTo(otherNum.toLong())
        is Int -> this.compareTo(otherNum.toInt())
        is Short -> this.compareTo(otherNum.toShort())
        is Double -> this.compareTo(otherNum.toDouble())
        is Float -> this.compareTo(otherNum.toFloat())
        else -> throw RuntimeException(
            "Comparison between ${this::class.java.simpleName} and " +
                "${otherNum.javaClass.simpleName} is not supported"
        )
    }
}
