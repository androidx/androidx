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
 * Denotes that the annotated element should be a float or double in the given range
 *
 * Example:
 * ```
 * @FloatRange(from=0.0,to=1.0)
 * public float getAlpha() {
 *     ...
 * }
 * ```
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
    AnnotationTarget.ANNOTATION_CLASS
)
public annotation class FloatRange(
    /** Smallest value. Whether it is inclusive or not is determined by [.fromInclusive] */
    val from: Double = Double.NEGATIVE_INFINITY,
    /** Largest value. Whether it is inclusive or not is determined by [.toInclusive] */
    val to: Double = Double.POSITIVE_INFINITY,
    /** Whether the from value is included in the range */
    val fromInclusive: Boolean = true,
    /** Whether the to value is included in the range */
    val toInclusive: Boolean = true
)
