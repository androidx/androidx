/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.testing

import androidx.room.processor.Context
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

data class TestInvocation(val processingEnv: ProcessingEnvironment,
                          val annotations: MutableSet<out TypeElement>,
                          val roundEnv: RoundEnvironment) {
    val context = Context(processingEnv)

    fun typeElement(qName: String): TypeElement {
        return processingEnv.elementUtils.getTypeElement(qName)
    }

    val typeUtils by lazy { processingEnv.typeUtils }
}
