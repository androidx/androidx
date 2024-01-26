/*
 * Copyright (C) 2022 The Android Open Source Project
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

import java.lang.annotation.ElementType.CONSTRUCTOR
import java.lang.annotation.ElementType.FIELD
import java.lang.annotation.ElementType.METHOD
import java.lang.annotation.ElementType.PACKAGE
import java.lang.annotation.ElementType.TYPE

/**
 * Denotes that the annotated element should only be called if the given extension is at least the
 * given version.
 *
 * This annotation is repeatable, and if specified multiple times, you are required to have one of
 * (not all of) the specified extension versions.
 *
 * Example:
 * ```
 * @RequiresExtension(extension = Build.VERSION_CODES.R, version = 3)
 * fun methodUsingApisFromR() { ... }
 * ```
 *
 * For the special case of [extension] == 0, you can instead use the [RequiresApi] annotation;
 * `@RequiresApi(30)` is equivalent to `@RequiresExtension(extension=0, version=30)`.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.FILE
)
// Needed due to Kotlin's lack of PACKAGE annotation target
// https://youtrack.jetbrains.com/issue/KT-45921
@Suppress("DEPRECATED_JAVA_ANNOTATION", "SupportAnnotationUsage")
@java.lang.annotation.Target(TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE)
@Repeatable
public annotation class RequiresExtension(
    /**
     * The extension SDK ID. This corresponds to the extension id's allowed by
     * [android.os.ext.SdkExtensions.getExtensionVersion], and for id values less than 1_000_000 is
     * one of the [android.os.Build.VERSION_CODES].
     */
    @IntRange(from = 1) val extension: Int,
    /** The minimum version to require */
    @IntRange(from = 1) val version: Int
)
