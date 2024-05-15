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

package androidx.room.writer

import androidx.room.compiler.codegen.toKotlinPoet
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.addOriginatingElement
import androidx.room.ext.CommonTypeNames.KOTLIN_CLASS
import androidx.room.vo.Database
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * Generates the `instantiateImpl` function which returns the generated database implementation.
 */
class InstantiateImplWriter(
    val database: Database,
) {
    fun write(processingEnv: XProcessingEnv) {
        val databaseTypeName = database.typeName.toKotlinPoet()
        val fileName = "${databaseTypeName.simpleNames.joinToString("_")}_InstantiateImpl"
        val funSpec = FunSpec.builder("instantiateImpl")
            .addOriginatingElement(database.element)
            .addModifiers(KModifier.INTERNAL)
            .receiver(KOTLIN_CLASS.toKotlinPoet().parameterizedBy(databaseTypeName))
            .returns(databaseTypeName)
            .addStatement(
                "return %L()",
                database.implTypeName.toKotlinPoet()
            ).build()

        val fileSpec = FileSpec.builder(database.typeName.packageName, fileName)
            .addFunction(funSpec)
            .build()
        processingEnv.filer.write(fileSpec)
    }
}
