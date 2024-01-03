/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * Denotes that the annotated element represents a packed color long. If applied to a long array,
 * every element in the array represents a color long. For more information on how colors are packed
 * in a long, please refer to the documentation of the [android.graphics.Color] class.
 *
 * Example:
 * ```
 * public void setFillColor(@ColorLong long color);
 * ```
 *
 * @see android.graphics.Color
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FIELD
)
public annotation class ColorLong
