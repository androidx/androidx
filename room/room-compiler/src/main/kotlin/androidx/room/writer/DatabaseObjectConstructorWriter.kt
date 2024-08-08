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

package androidx.room.writer

import androidx.room.compiler.codegen.toKotlinPoet
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.addOriginatingElement
import androidx.room.ext.RoomTypeNames.ROOM_DB_CONSTRUCTOR
import androidx.room.vo.Database
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

class DatabaseObjectConstructorWriter(
    private val database: Database,
    private val constructorObjectElement: XTypeElement
) {
    fun write(processingEnv: XProcessingEnv) {
        val databaseClassName = database.typeName.toKotlinPoet()
        val objectClassName = constructorObjectElement.asClassName().toKotlinPoet()
        val typeSpec =
            TypeSpec.objectBuilder(objectClassName)
                .apply {
                    addOriginatingElement(database.element)
                    addModifiers(KModifier.ACTUAL)
                    if (constructorObjectElement.isInternal()) {
                        addModifiers(KModifier.INTERNAL)
                    } else if (constructorObjectElement.isPublic()) {
                        addModifiers(KModifier.PUBLIC)
                    }
                    addSuperinterface(
                        ROOM_DB_CONSTRUCTOR.toKotlinPoet().parameterizedBy(databaseClassName)
                    )
                    addFunction(
                        FunSpec.builder("initialize")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(databaseClassName)
                            .addStatement("return %L()", database.implTypeName.toKotlinPoet())
                            .build()
                    )
                }
                .build()
        val fileSpec =
            FileSpec.builder(objectClassName.packageName, objectClassName.simpleName)
                .addType(typeSpec)
                .build()
        processingEnv.filer.write(fileSpec)
    }
}
