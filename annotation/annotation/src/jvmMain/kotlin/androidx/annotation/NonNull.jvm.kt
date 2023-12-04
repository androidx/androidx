/*
 * Copyright (C) 2013 The Android Open Source Project
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
import java.lang.annotation.ElementType.FIELD
import java.lang.annotation.ElementType.LOCAL_VARIABLE
import java.lang.annotation.ElementType.METHOD
import java.lang.annotation.ElementType.PACKAGE
import java.lang.annotation.ElementType.PARAMETER

/**
 * Denotes that a parameter, field or method return value can never be null.
 *
 * This is a marker annotation and it has no specific attributes.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FILE
)
// Needed due to Kotlin's lack of PACKAGE annotation target
// https://youtrack.jetbrains.com/issue/KT-45921
@Suppress("DEPRECATED_JAVA_ANNOTATION", "SupportAnnotationUsage")
@java.lang.annotation.Target(METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE)
public annotation class NonNull
