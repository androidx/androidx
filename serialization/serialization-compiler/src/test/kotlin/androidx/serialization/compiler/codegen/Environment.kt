/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.compiler.codegen

import androidx.serialization.compiler.codegen.CodeGenEnvironment.Generated
import androidx.serialization.compiler.codegen.java.JavaGenEnvironment
import androidx.serialization.compiler.nullability.Nullability
import kotlin.reflect.KClass

internal fun codeGenEnv(
    testClass: KClass<*>,
    generated: Generated? = DEFAULT_GENERATED,
    nullability: Nullability? = DEFAULT_NULLABILITY
): CodeGenEnvironment {
    return CodeGenEnvironment(testClass.qualifiedName, generated, nullability)
}

internal fun javaGenEnv(
    testClass: KClass<*>,
    generated: Generated? = DEFAULT_GENERATED,
    nullability: Nullability? = DEFAULT_NULLABILITY
): JavaGenEnvironment {
    return JavaGenEnvironment(codeGenEnv(testClass, generated, nullability))
}

internal val DEFAULT_GENERATED = Generated(packageName = "javax.annotation")
internal val DEFAULT_NULLABILITY = Nullability("androidx.annotation")
