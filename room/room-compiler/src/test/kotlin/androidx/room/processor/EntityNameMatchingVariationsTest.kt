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

package androidx.room.processor

import androidx.kruth.assertThat
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XTypeName
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.CallType
import androidx.room.vo.Field
import androidx.room.vo.FieldGetter
import androidx.room.vo.FieldSetter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class EntityNameMatchingVariationsTest(triple: Triple<String, String, String>) :
    BaseEntityParserTest() {
    val fieldName = triple.first
    val getterName = triple.second
    val setterName = triple.third

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): List<Triple<String, String, String>> {
            val result = arrayListOf<Triple<String, String, String>>()
            arrayListOf("x", "_x", "mX").forEach { field ->
                arrayListOf("getX", "x").forEach { getter ->
                    arrayListOf("setX", "x").forEach { setter ->
                        result.add(Triple(field, getter, setter))
                    }
                }
            }
            return result
        }
    }

    @Test
    fun testSuccessfulParamToMethodMatching() {
        singleEntity(
            """
                @PrimaryKey
                private int $fieldName;
                public int $getterName() { return $fieldName; }
                public void $setterName(int id) { this.$fieldName = id; }
            """
        ) { entity, invocation ->
            assertThat(entity.type.asTypeName().toString(CodeLanguage.JAVA))
                .isEqualTo("foo.bar.MyEntity")
            assertThat(entity.fields.size).isEqualTo(1)
            val field = entity.fields.first()
            val intType = invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT)
            assertThat(field).isEqualTo(
                    Field(
                        element = field.element,
                        name = fieldName,
                        type = intType,
                        columnName = fieldName,
                        affinity = SQLTypeAffinity.INTEGER
                    )
                )
            assertThat(field.setter)
                .isEqualTo(FieldSetter(field.name, setterName, intType, CallType.METHOD))
            assertThat(field.getter)
                .isEqualTo(FieldGetter(field.name, getterName, intType, CallType.METHOD))
        }
    }
}
