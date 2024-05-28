/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.InternalXAnnotationValue
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.javac.kotlin.KmAnnotationArgumentContainer

internal class JavacKmAnnotationValue(
    private val method: XMethodElement,
    override val valueType: XType = method.returnType,
    private val kmAnnotationArgumentContainer: KmAnnotationArgumentContainer
) : InternalXAnnotationValue() {
    override val name: String = method.jvmName
    override val value: Any? by lazy { kmAnnotationArgumentContainer.getValue(method) }
}
