/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.lifecycle

import androidx.lifecycle.model.EventMethod
import javax.lang.model.element.TypeElement

object ErrorMessages {
    const val TOO_MANY_ARGS = "callback method cannot have more than 2 parameters"
    const val TOO_MANY_ARGS_NOT_ON_ANY = "only callback annotated with ON_ANY " +
            "can have 2 parameters"
    const val INVALID_SECOND_ARGUMENT = "2nd argument of a callback method" +
            " must be Lifecycle.Event and represent the current event"
    const val INVALID_FIRST_ARGUMENT = "1st argument of a callback method must be " +
            "a LifecycleOwner which represents the source of the event"
    const val INVALID_METHOD_MODIFIER = "method marked with OnLifecycleEvent annotation can " +
            "not be private"
    const val INVALID_CLASS_MODIFIER = "class containing OnLifecycleEvent methods can not be " +
            "private"
    const val INVALID_STATE_OVERRIDE_METHOD = "overridden method must handle the same " +
            "onState changes as original method"
    const val INVALID_ENCLOSING_ELEMENT =
            "Parent of OnLifecycleEvent should be a class or interface"
    const val INVALID_ANNOTATED_ELEMENT = "OnLifecycleEvent can only be added to methods"

    fun failedToGenerateAdapter(type: TypeElement, failureReason: EventMethod) =
            """
             Failed to generate an Adapter for $type, because it needs to be able to access to
             package private method ${failureReason.method.name()} from ${failureReason.type}
            """.trim()
}
