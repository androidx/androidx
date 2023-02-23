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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Change
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.TestOnly

internal sealed class Operation(
    val ints: Int = 0,
    val objects: Int = 0
) {

    val name: String
        get() = javaClass.simpleName

    abstract fun OperationArgContainer.execute(
        applier: Applier<*>,
        slots: SlotWriter,
        rememberManager: RememberManager
    )

    open fun intParamName(parameter: IntParameter): String =
        "IntParameter(${parameter.offset})"

    open fun objectParamName(parameter: ObjectParameter<*>): String =
        "ObjectParameter(${parameter.offset})"

    override fun toString() = name

    @JvmInline
    value class IntParameter(val offset: Int)

    @JvmInline
    value class ObjectParameter<T>(val offset: Int)

    object BackwardsCompatOp : Operation(objects = 1) {
        val Change = ObjectParameter<Change>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) = when (parameter) {
            Change -> "change"
            else -> super.objectParamName(parameter)
        }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) = getObject(Change).invoke(applier, slots, rememberManager)
    }

    /**
     * Operation type used for tests. Operations can be created with arbitrary int and object
     * params, which lets us test [Operations] without relying on the implementation details of any
     * particular operation we use in production.
     */
    class TestOperation @TestOnly constructor(
        ints: Int = 0,
        objects: Int = 0,
        val block: (Applier<*>, SlotWriter, RememberManager) -> Unit = { _, _, _ -> }
    ) : Operation(ints, objects) {
        val intParams = List(ints) { index -> IntParameter(index) }
        val objParams = List(objects) { index -> ObjectParameter<Any?>(index) }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ): Unit = block(applier, slots, rememberManager)

        override fun toString() =
            "TestOperation(ints = $ints, objects = $objects)@${System.identityHashCode(this)}"
    }
}