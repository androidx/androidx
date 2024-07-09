/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.text

import kotlin.jvm.JvmInline

/**
 * An [AnnotatedString.Annotation] class which holds a String [value].
 *
 * You can use it to provide a custom annotation to the [AnnotatedString].
 *
 * If you use the [AnnotatedString.Builder] methods like [withAnnotation] and provide a string
 * annotation, it will be automatically wrapped into this holder class.
 *
 * @see withAnnotation
 * @see getStringAnnotations
 */
@JvmInline value class StringAnnotation(val value: String) : AnnotatedString.Annotation

internal fun AnnotatedString.Range<out AnnotatedString.Annotation>.unbox():
    AnnotatedString.Range<String> =
    AnnotatedString.Range((item as StringAnnotation).value, start, end, tag)
