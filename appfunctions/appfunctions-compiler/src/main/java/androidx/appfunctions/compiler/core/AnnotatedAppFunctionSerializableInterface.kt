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

package androidx.appfunctions.compiler.core

import com.google.devtools.ksp.symbol.KSClassDeclaration

/** Represents a class annotated with [androidx.appfunctions.AppFunctionSerializableInterface]. */
class AnnotatedAppFunctionSerializableInterface(private val classDeclaration: KSClassDeclaration) :
    AnnotatedAppFunctionSerializable(classDeclaration) {

    override fun validate(
        allowSerializableInterfaceTypes: Boolean
    ): AnnotatedAppFunctionSerializable {
        val validator = AppFunctionSerializableValidateHelper(this)
        validator.validateParameters(allowSerializableInterfaceTypes)
        return this
    }

    override fun getProperties(): List<AppFunctionPropertyDeclaration> {
        return classDeclaration
            .getAllProperties()
            .map {
                AppFunctionPropertyDeclaration(
                    it,
                    isDescribedByKdoc,
                    // Property from interface is always required as there is no existing API
                    // to tell if the interface property has default value or not.
                    isRequired = true,
                )
            }
            .toList()
    }
}
