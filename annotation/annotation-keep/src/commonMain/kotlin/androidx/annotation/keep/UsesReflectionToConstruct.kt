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

// ***********************************************************************************
// GENERATED FILE. DO NOT EDIT! See KeepItemAnnotationGenerator.java.
// ***********************************************************************************

// ***********************************************************************************
// MAINTAINED AND TESTED IN THE R8 REPO. PLEASE MAKE CHANGES THERE AND REPLICATE.
// ***********************************************************************************

package androidx.annotation.keep

import kotlin.reflect.KClass

/**
 * The annotated code uses reflection to indirectly invoke a constructor of a class/interface, or
 * its subclasses / interface implementers.
 *
 * This annotation indicates to optimizers or shrinkers that the target constructor should be kept
 * if the annotated code is reachable in the final application build.
 *
 * `@UsesReflectionToConstruct()` is a convenience for `@UsesReflectionToAccessMethod(methodName =
 * "<init>")`
 *
 * @see UsesReflectionToAccessMethod
 * @see UsesReflectionToAccessField
 */
@Retention(AnnotationRetention.BINARY)
@Repeatable
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class UsesReflectionToConstruct(

    /**
     * Class to be instantiated.
     *
     * Mutually exclusive with [className].
     */
    val classConstant: KClass<*> = Unspecified::class,

    /**
     * Class to be instantiated.
     *
     * Mutually exclusive with [classConstant].
     */
    val className: String = "",

    /**
     * Defines which constructor to keep by specifying the parameter list types.
     *
     * If neither `parameterTypes` nor `parameterTypeNames` is specified then constructors with all
     * parameter lists are kept.
     *
     * Mutually exclusive with [parameterTypeNames].
     */
    val parameterTypes: Array<KClass<*>> = [Unspecified::class],

    /**
     * Defines which constructor to keep by specifying the parameter list types.
     *
     * If neither `parameterTypes` nor `parameterTypeNames` is specified then constructors with all
     * parameter lists are kept.
     *
     * Mutually exclusive with [parameterTypes].
     */
    val parameterTypeNames: Array<String> = [""],
)
