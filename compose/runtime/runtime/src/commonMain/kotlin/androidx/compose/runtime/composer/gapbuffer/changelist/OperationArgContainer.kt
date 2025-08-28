/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.composer.gapbuffer.changelist

/** A container of parameters assigned to the arguments of an [Operation]. */
internal interface OperationArgContainer {
    /** Returns the assigned value of [parameter] for the current operation. */
    fun getInt(parameter: Int): Int

    /** Returns the assigned value of [parameter] for the current operation. */
    fun <T> getObject(parameter: Operation.ObjectParameter<T>): T
}
