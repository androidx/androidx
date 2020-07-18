/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing

import androidx.annotation.VisibleForTesting
import androidx.room.processing.javac.JavacProcessingEnv
import androidx.room.processing.javac.JavacRoundEnv
import javax.annotation.processing.RoundEnvironment

// only used in tests of Room
@VisibleForTesting
interface XRoundEnv {

    val rootElements: Set<XElement>

    fun getElementsAnnotatedWith(klass: Class<out Annotation>): Set<XElement>

    companion object {
        fun create(
            processingEnv: XProcessingEnv,
            roundEnvironment: RoundEnvironment
        ): XRoundEnv {
            check(processingEnv is JavacProcessingEnv)
            return JavacRoundEnv(processingEnv, roundEnvironment)
        }
    }
}
