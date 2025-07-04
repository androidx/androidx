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

package androidx.annotation.keep

import kotlin.reflect.KClass

/**
 * The annotated code uses reflection to indirectly access a method of the specified
 * class/interface, or its subclasses / interface implementers.
 *
 * This annotation indicates to optimizers or shrinkers that the target method should be preserved
 * if the annotated code is reachable in the final application build.
 *
 * @see UsesReflectionToConstruct
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
public annotation class UsesReflectionToAccessMethod(
    /**
     * Class containing the field accessed by reflection.
     *
     * Mutually exclusive with [className].
     */
    @Suppress("KotlinDefaultParameterOrder") val classConstant: KClass<*> = Unspecified::class,

    /**
     * Class name (or pattern) containing the field accessed by reflection.
     *
     * Mutually exclusive with [classConstant].
     */
    @Suppress("KotlinDefaultParameterOrder") val className: String = "",

    /** Method name (or pattern) accessed by reflection. */
    val methodName: String,

    /**
     * Defines which method to keep by specifying set of parameter classes passed.
     *
     * Defaults to `[ Unspecified::class ]`, which will keep all methods.
     *
     * Mutually exclusive with [paramClassNames].
     */
    val params: Array<KClass<*>> = [Unspecified::class],

    /**
     * Defines which method to keep by specifying set of parameter classes passed.
     *
     * Defaults to `[""]`, which will keep all methods.
     *
     * Mutually exclusive with [params].
     */
    val paramClassNames: Array<String> = [""],

    /**
     * Class of field accessed by reflection.
     *
     * Ignored if not specified.
     *
     * Mutually exclusive with [returnClassName].
     */
    val returnClass: KClass<*> = Unspecified::class,

    /**
     * Class (or class pattern) of field accessed by reflection.
     *
     * Ignored if not specified.
     *
     * Mutually exclusive with [returnClass].
     */
    val returnClassName: String = "",
)
