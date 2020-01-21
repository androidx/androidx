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

package androidx.serialization.compiler.schema

import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

/** Things that can access a message field. */
internal sealed class FieldAccessor {
    abstract val element: Element
}

/** Things that can read a message field. */
internal sealed class FieldReader : FieldAccessor()

/** Things that can set or update a message field. */
internal sealed class FieldWriter : FieldAccessor()

/** Things that can set a field at construction time. */
internal sealed class FieldCreator : FieldWriter()

/** Things that can update a field after construction. */
internal sealed class FieldUpdater : FieldWriter()

/** An open or final field. */
internal data class FieldFieldReader(override val element: VariableElement) : FieldReader()

/** A getter method. */
internal data class GetterFieldReader(override val element: ExecutableElement) : FieldReader()

/** An open field. */
internal data class FieldFieldUpdater(override val element: VariableElement) : FieldUpdater()

/** A setter method. */
internal data class SetterFieldUpdater(override val element: ExecutableElement) : FieldUpdater()

/** A constructor parameter. */
internal data class ConstructorFieldCreator(
    override val element: VariableElement,
    val position: Int,
    val constructorElement: ExecutableElement
) : FieldCreator()
