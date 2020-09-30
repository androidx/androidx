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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.jetbrains.kotlin.ksp.processing.Resolver

class TestInvocation(
    val processingEnv: XProcessingEnv
) {
    val isKsp = processingEnv is KspProcessingEnv

    val kspResolver: Resolver
        get() = (processingEnv as KspProcessingEnv).resolver

    val types by lazy {
        if (processingEnv is KspProcessingEnv) {
            Types(
                string = KotlinTypeNames.STRING_CLASS_NAME,
                voidOrUnit = KotlinTypeNames.UNIT_CLASS_NAME,
                objectOrAny = KotlinTypeNames.ANY_CLASS_NAME,
                boxedInt = KotlinTypeNames.INT_CLASS_NAME,
                int = KotlinTypeNames.INT_CLASS_NAME,
                long = KotlinTypeNames.LONG_CLASS_NAME,
                list = KotlinTypeNames.LIST_CLASS_NAME
            )
        } else {
            Types(
                string = ClassName.get("java.lang", "String"),
                voidOrUnit = TypeName.VOID,
                objectOrAny = TypeName.OBJECT,
                boxedInt = TypeName.INT.box(),
                int = TypeName.INT,
                long = TypeName.LONG,
                list = ClassName.get("java.util", "List")
            )
        }
    }

    /**
     * Helper class to hold types that change between KSP and Javap.
     * e.g. Kotlin.String vs java.lang.String
     */
    class Types(
        val string: ClassName,
        val voidOrUnit: TypeName,
        val objectOrAny: ClassName,
        val boxedInt: TypeName,
        val int: TypeName,
        val long: TypeName,
        val list: ClassName
    )
}
