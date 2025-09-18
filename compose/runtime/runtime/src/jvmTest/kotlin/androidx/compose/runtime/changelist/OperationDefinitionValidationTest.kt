/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime.changelist

import androidx.compose.runtime.changelist.Operation.ObjectParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class OperationDefinitionValidationTest<T : Operation>(private val operation: T) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> =
            Operation::class.sealedSubclasses.mapNotNull { it.objectInstance }.toTypedArray()
    }

    @Test
    fun validateOperationDefinition() {
        val intParams = mutableListOf<Pair<String, IntParameter>>()
        val objParams = mutableListOf<Pair<String, ObjectParameter<*>>>()
        val errors = mutableListOf<String>()

        operation::class
            .declaredMemberProperties
            .map { @Suppress("UNCHECKED_CAST") (it as KProperty1<T, Any?>) }
            .forEach { property ->
                when (val propertyValue = property.getter.invoke(operation)) {
                    is IntParameter -> intParams += property.name to propertyValue
                    is ObjectParameter<*> -> objParams += property.name to propertyValue
                    else -> {
                        println("Ignoring unexpected property $property")
                    }
                }
            }

        if (operation.ints < 0) {
            errors += "The `Operation.ints` property cannot be assigned a negative value."
        }

        if (operation.objects < 0) {
            errors += "The `Operation.objects` property cannot be assigned a negative value."
        }

        if (intParams.size != operation.ints) {
            errors +=
                "Operation declared a different number of int parameters than it " +
                    "reports having. Either set ${operation.name}'s `ints` property to " +
                    "${intParams.size} or update its parameter definitions so that there are " +
                    "${operation.ints} IntParameter properties."
        }

        if (objParams.size != operation.objects) {
            errors +=
                "Operation declared a different number of object parameters than it " +
                    "reports having. Either set ${operation.name}'s `objects` property to " +
                    "${objParams.size} or update its parameter definitions so that there are " +
                    "${operation.objects} ObjectParameter properties."
        }

        errors += checkNoDuplicateOffsets(intParams, objParams)
        errors += checkValidOffsetRange(intParams, objParams)

        if (errors.isNotEmpty()) {
            fail(
                "Operation ${operation.name} appears to be defined incorrectly. Its errors are:" +
                    errors.joinToString(separator = "") { "\n    - $it" }
            )
        }
    }

    private fun checkNoDuplicateOffsets(
        intParams: List<Pair<String, IntParameter>>,
        objParams: List<Pair<String, ObjectParameter<*>>>,
    ): List<String> {
        val errors = mutableListOf<String>()
        val duplicateIntOffsets =
            intParams
                .groupBy(
                    keySelector = { (_, param) -> param },
                    valueTransform = { (name, _) -> name },
                )
                .filterValues { it.size != 1 }

        if (duplicateIntOffsets.isNotEmpty()) {
            errors +=
                "All int parameters must have unique offsets. " +
                    "The offending pairs are: " +
                    duplicateIntOffsets.values.joinToString {
                        it.joinToString(prefix = "[", postfix = "]")
                    }
        }

        val duplicateObjOffsets =
            objParams
                .groupBy(
                    keySelector = { (_, param) -> param.offset },
                    valueTransform = { (name, _) -> name },
                )
                .filterValues { it.size != 1 }

        if (duplicateObjOffsets.isNotEmpty()) {
            errors +=
                "All object parameters must have unique offsets. " +
                    "The offending pairs are: " +
                    duplicateObjOffsets.values.joinToString {
                        it.joinToString(prefix = "[", postfix = "]")
                    }
        }

        return errors
    }

    private fun checkValidOffsetRange(
        intParams: List<Pair<String, IntParameter>>,
        objParams: List<Pair<String, ObjectParameter<*>>>,
    ): List<String> {
        val errors = mutableListOf<String>()

        val outOfRangeInts =
            intParams.mapNotNull { (name, param) ->
                name
                    .takeIf { param < 0 || param >= intParams.size }
                    ?.let { paramName -> "$paramName (offset = ${param})" }
            }
        if (outOfRangeInts.isNotEmpty()) {
            errors +=
                "All int parameter offsets must be in the range of " +
                    "0..${intParams.size - 1}. The offending parameters are: " +
                    outOfRangeInts.joinToString()
        }

        val outOfRangeObjects =
            objParams.mapNotNull { (name, param) ->
                name
                    .takeIf { param.offset < 0 || param.offset >= objParams.size }
                    ?.let { paramName -> "$paramName (offset = ${param.offset})" }
            }
        if (outOfRangeObjects.isNotEmpty()) {
            errors +=
                "All object parameter offsets must be in the range of " +
                    "0..${objParams.size - 1}. The offending parameters are: " +
                    outOfRangeObjects.joinToString()
        }

        return errors
    }
}
