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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.ResReference

fun resolveArguments(rootDestination: Destination): Destination {
    val destinations = mutableMapOf<ResReference, Destination>()

    fun dfs(dest: Destination): Destination {
        val nested = dest.nested.filter { it.id != null }.associateBy { it.id!! }
        destinations.putAll(nested)
        val resolvedActions = dest.actions.map { action ->
            val actionDestination = destinations[action.destination]
            if (actionDestination != null) {
                action.copy(args = mergeArguments(action.args, actionDestination.args))
            } else {
                action
            }
        }
        val result = dest.copy(nested = dest.nested.map(::dfs), actions = resolvedActions)
        nested.keys.forEach { id -> destinations.remove(id) }
        return result
    }

    return dfs(rootDestination)
}

private fun mergeArguments(args1: List<Argument>, args2: List<Argument>) =
        args2.fold(args1) { result, arg ->
            val duplicate = result.find { it.name == arg.name }
            if (duplicate != null) {
                if (duplicate.type != arg.type) {
                    // TODO: needs context to print better exception
                    throw IllegalArgumentException("Incompatible types ${duplicate.type} and " +
                            "${arg.type} of argument ${arg.name}")
                }
                result
            } else {
                result + arg
            }
        }
