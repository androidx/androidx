/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.MustBeDocumented
import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * A signed angle in radians. A positive value represents rotation in the direction from the
 * positive x-axis towards the positive y-axis.
 *
 * Ink generally provides and accepts angle values in degrees. This annotation can be used to
 * distinguish float angle values that are in radians instead. The [Angle] class provides the
 * conversion methods [Angle.radiansToDegrees] and [Angle.degreesToRadians], which can be used to
 * convert to and from the radian values used by trigonometric functions.
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FIELD,
)
public annotation class AngleRadiansFloat
