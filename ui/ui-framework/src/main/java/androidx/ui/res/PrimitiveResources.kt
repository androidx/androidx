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

package androidx.ui.res

import androidx.annotation.ArrayRes
import androidx.annotation.BoolRes
import androidx.annotation.CheckResult
import androidx.annotation.DimenRes
import androidx.annotation.IntegerRes
import androidx.compose.ambient
import androidx.compose.effectOf
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Dp
import androidx.ui.core.ambientDensity

/**
 * Load an integer resource.
 *
 * @param id the resource identifier
 * @return the integer associated with the resource
 */
@CheckResult(suggest = "+")
fun integerResource(@IntegerRes id: Int) = effectOf<Int> {
    val context = +ambient(ContextAmbient)
    context.resources.getInteger(id)
}

/**
 * Load an array of integer resource.
 *
 * @param id the resource identifier
 * @return the integer array associated with the resource
 */
@CheckResult(suggest = "+")
fun integerArrayResource(@ArrayRes id: Int) = effectOf<IntArray> {
    val context = +ambient(ContextAmbient)
    context.resources.getIntArray(id)
}

/**
 * Load a boolean resource.
 *
 * @param id the resource identifier
 * @return the boolean associated with the resource
 */
@CheckResult(suggest = "+")
fun booleanResource(@BoolRes id: Int) = effectOf<Boolean> {
    val context = +ambient(ContextAmbient)
    context.resources.getBoolean(id)
}

/**
 * Load a boolean resource.
 *
 * @param id the resource identifier
 * @return the dimension value associated with the resource
 */
@CheckResult(suggest = "+")
fun dimensionResource(@DimenRes id: Int) = effectOf<Dp> {
    val context = +ambient(ContextAmbient)
    val density = +ambientDensity()
    val pxValue = context.resources.getDimension(id)
    Dp(pxValue / density.density)
}
