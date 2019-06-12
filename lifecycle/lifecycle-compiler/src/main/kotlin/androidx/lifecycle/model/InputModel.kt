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

package androidx.lifecycle.model

import androidx.lifecycle.name
import androidx.lifecycle.syntheticName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

data class InputModel(
        // all java files with lifecycle annotations excluding classes from classpath
        private val rootTypes: Set<TypeElement>,
        // info about all lifecycle observers including classes from classpath
        val observersInfo: Map<TypeElement, LifecycleObserverInfo>,
        // info about generated adapters from class path
        val generatedAdapters: Map<TypeElement, List<ExecutableElement>>) {

    /**
     *  Root class is class defined in currently processed module, not in classpath
     */
    fun isRootType(type: TypeElement) = type in rootTypes

    fun hasSyntheticAccessorFor(eventMethod: EventMethod): Boolean {
        val syntheticMethods = generatedAdapters[eventMethod.type] ?: return false
        return syntheticMethods.any { executable ->
            executable.name() == syntheticName(eventMethod.method)
                    // same number + receiver object
                    && (eventMethod.method.parameters.size + 1) == executable.parameters.size
        }
    }
}