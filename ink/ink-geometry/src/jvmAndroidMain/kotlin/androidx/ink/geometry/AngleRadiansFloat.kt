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
 * A signed angle in radians. A positive value represents rotation from the positive x-axis to the
 * positive y-axis. [Angle] class manages the conversion of angle values in degrees and radians with
 * [Angle.radiansToDegrees] and [Angle.degreesToRadians]. Most of Strokes API requires angle values
 * in radians.
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
