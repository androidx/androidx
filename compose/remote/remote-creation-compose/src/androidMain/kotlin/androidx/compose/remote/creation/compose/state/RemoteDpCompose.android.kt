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

@file:JvmName("RemoteDpComposeKt")

package androidx.compose.remote.creation.compose.state

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Creates a [RemoteDp] from a literal [Dp] value.
 *
 * @param value The [Dp] value.
 * @return A [RemoteDp] representing the constant Dp.
 */
@Stable public fun RemoteDp(value: Dp): RemoteDp = value.asRdp()

/**
 * Converts this [RemoteDp] to a [Dp] if it has a constant value, or null otherwise.
 *
 * @return the constant [Dp] value of this [RemoteDp], or `null` if it is dynamic
 */
@Stable public fun RemoteDp.toConstantDpOrNull(): Dp? = constantValueOrNull?.dp

/**
 * Converts this [RemoteDp] to a [Dp] if it has a constant value.
 *
 * @return the constant [Dp] value of this [RemoteDp]
 * @throws IllegalStateException if this state does not have a constant value.
 */
@Stable public fun RemoteDp.toConstantDp(): Dp = constantValue.dp

/**
 * Extension function to convert a [Dp] to a [RemoteDp].
 *
 * @return a [RemoteDp] representing this [Dp]
 */
@Stable
public fun Dp.asRdp(): RemoteDp {
    check(isSpecified) { "Dp conversion not possible for unspecified Dp" }
    return RemoteDp(this.value.rf)
}

/** Extension property to convert a [Dp] to a [RemoteDp]. */
@Stable
public val Dp.rdp: RemoteDp
    get() = asRdp()
