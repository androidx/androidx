/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.annotation

import java.lang.annotation.ElementType.ANNOTATION_TYPE
import java.lang.annotation.ElementType.CONSTRUCTOR
import java.lang.annotation.ElementType.FIELD
import java.lang.annotation.ElementType.METHOD
import java.lang.annotation.ElementType.PACKAGE
import java.lang.annotation.ElementType.TYPE

/**
 * Denotes that the annotated element should not be removed when the code is minified at build time.
 * This is typically used on methods and classes that are accessed only via reflection so a compiler
 * may think that the code is unused.
 *
 * Do not use this within library code. As a best practice, minification should be able to remove
 * all library code for libraries that are added as dependencies but not used. For library code,
 * consider instead using conditional keep rules. E.g.: -if ... -keep ...
 *
 * Example:
 * ```
 * @Keep
 * public void foo() {
 *     ...
 * }
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD
)
// Needed due to Kotlin's lack of PACKAGE annotation target
// https://youtrack.jetbrains.com/issue/KT-45921
@Suppress("DEPRECATED_JAVA_ANNOTATION", "SupportAnnotationUsage")
@java.lang.annotation.Target(PACKAGE, TYPE, ANNOTATION_TYPE, CONSTRUCTOR, METHOD, FIELD)
public annotation class Keep
