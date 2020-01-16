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

package androidx.serialization.compiler.codegen.java

import androidx.serialization.EnumValue
import androidx.serialization.compiler.codegen.originatingElement
import androidx.serialization.compiler.codegen.toLowerCamelCase
import androidx.serialization.schema.Enum
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

/** Get the name used for an enum coder for the supplied class name. */
internal fun enumCoderName(enumName: ClassName): ClassName {
    return ClassName.get(
        enumName.packageName(),
        enumName.simpleNames().joinToString(
            prefix = "\$Serialization",
            separator = "_",
            postfix = "EnumCoder"
        )
    )
}

/** Generate the Java source file for an enum coder. */
internal fun generateEnumCoder(enum: Enum, javaGenEnv: JavaGenEnvironment): JavaFile {
    val enumClass = enum.name.toClassName()
    val variableName = nameAllocatorOf("encode", "decode")
        .newName(enum.name.simpleName.toLowerCamelCase())

    val default = enum.values.first { it.id == EnumValue.DEFAULT }
    val values = enum.values.filter { it.id != EnumValue.DEFAULT }.sortedBy { it.id }

    return buildClass(enumCoderName(enumClass), javaGenEnv, enum.originatingElement) {
        addModifiers(PUBLIC, FINAL)
        addJavadoc("Serialization of enum {@link $T}.\n", enumClass)

        constructor { addModifiers(PRIVATE) }

        method("encode") {
            addModifiers(PUBLIC, STATIC)
            parameter(variableName, enumClass, javaGenEnv.nullable)
            returns(TypeName.INT)

            controlFlow("if ($N != null)", variableName) {
                controlFlow("switch ($N)", variableName) {
                    for (value in values) {
                        switchCase("\$N", value.name) {
                            addStatement("return $L", value.id)
                        }
                    }
                }
            }

            addCode("return $L; // $N\n", EnumValue.DEFAULT, default.name)
        }

        method("decode") {
            addModifiers(PUBLIC, STATIC)
            parameter("value", TypeName.INT)
            returns(enumClass, javaGenEnv.nonNull)

            controlFlow("switch (value)") {
                for (value in values) {
                    switchCase("\$L", value.id) {
                        addStatement("return $T.$N", enumClass, value.name)
                    }
                }

                switchDefault {
                    addStatement("return $T.$N", enumClass, default.name)
                }
            }
        }
    }
}
