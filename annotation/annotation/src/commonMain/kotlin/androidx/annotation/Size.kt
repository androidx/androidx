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

/**
 * Denotes that the annotated element should have a given size or length. Note that "-1" means
 * "unset". Typically used with a parameter or return value of type array or collection.
 *
 * Example:
 * ```
 * public void getLocationInWindow(@Size(2) int[] location) {
 *     ...
 * }
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.ANNOTATION_CLASS
)
public annotation class Size(
    /** An exact size (or -1 if not specified) */
    val value: Long = -1,
    /** A minimum size, inclusive */
    val min: Long = Long.MIN_VALUE,
    /** A maximum size, inclusive */
    val max: Long = Long.MAX_VALUE,
    /** The size must be a multiple of this factor */
    val multiple: Long = 1
)
