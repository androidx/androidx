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

package androidx.compose.remote.player.compose.embedded.state

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.NanMap

/**
 * A document float field that holds either a literal value or a reference to a float variable.
 *
 * Remote Compose encodes a variable reference as a NaN whose payload is the variable id (see
 * [Utils.asNan]). A plain NaN, with id 0, means the field is unset.
 *
 * Every NaN with a non-zero id is a reference, including the system variables (time, touch,
 * sensors, density, font size, ...), as the core operations resolve them. [Utils.isVariable] is not
 * the same check: it excludes ids 10 to 40.
 */
@JvmInline
internal value class MaybeVariableFloat(val raw: Float) {
    /** True if [raw] is a literal value rather than a reference or unset. */
    val isLiteral: Boolean
        get() = !raw.isNaN()

    /** True if [raw] is a plain NaN, meaning the field has no value. */
    val isUnset: Boolean
        get() = raw.isNaN() && Utils.idFromNan(raw) == 0

    /** True if [raw] references the float variable [id]. */
    val isReference: Boolean
        get() = raw.isNaN() && Utils.idFromNan(raw) != 0

    /** The referenced variable id. Only meaningful when [isReference] is true. */
    val id: Int
        get() = Utils.idFromNan(raw)

    /** True if [raw] is a float expression operator, such as [AnimatedFloatExpression.ADD]. */
    val isOperator: Boolean
        get() = raw.isNaN() && AnimatedFloatExpression.isMathOperator(raw)

    /** True if [raw] references a float array (a data variable), as read by array operators. */
    val isArray: Boolean
        get() = raw.isNaN() && NanMap.isDataVariable(raw)

    /**
     * True if [raw], as a float expression token, references a scalar float variable: a NaN that is
     * neither an operator nor an array. As in the core evaluator, a plain NaN token reads
     * variable 0.
     */
    val isScalarReference: Boolean
        get() = raw.isNaN() && !isOperator && !isArray

    /**
     * The referenced variable's current value read from [context], or [raw] otherwise. A plain NaN
     * stays NaN, so callers can still tell an unset field apart.
     */
    fun resolve(context: RemoteContext): Float = if (isReference) context.getFloat(id) else raw

    /**
     * [literal] if [raw] is a literal, otherwise variable [id] read from [context]. As in the core
     * operations' `updateVariables`, a plain NaN reads variable 0, so an unset draw parameter (such
     * as a rotation pivot) resolves to 0.
     */
    fun resolveOr(literal: Float, context: RemoteContext): Float =
        if (isLiteral) literal else context.getFloat(id)
}

/**
 * Calls [action] with the id of each scalar variable referenced by this float expression, skipping
 * literals, operators and arrays.
 */
internal inline fun FloatArray.forEachScalarReference(action: (id: Int) -> Unit) {
    for (i in indices) {
        val token = MaybeVariableFloat(this[i])
        if (token.isScalarReference) action(token.id)
    }
}

/** True if [predicate] holds for the id of any scalar variable this float expression references. */
internal inline fun FloatArray.anyScalarReference(predicate: (id: Int) -> Boolean): Boolean {
    forEachScalarReference { if (predicate(it)) return true }
    return false
}
