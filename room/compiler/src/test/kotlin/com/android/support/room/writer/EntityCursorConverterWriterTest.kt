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

package com.android.support.room.writer

import com.android.support.room.processor.BaseEntityParserTest
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.element.Modifier

@RunWith(JUnit4::class)
class EntityCursorConverterWriterTest : BaseEntityParserTest() {
    companion object {
        val OUT_PREFIX = """
            package foo.bar;
            import android.database.Cursor;
            import java.lang.String;
            public class MyContainerClass {
            """.trimIndent()
        const val OUT_SUFFIX = "}"
    }

    @Test
    fun generateSimple() {
        generateAndMatch(
                """
                @PrimaryKey
                private int id;
                String name;
                String lastName;
                int age;
                public int getId() { return id; }
                public void setId(int id) { this.id = id; }
                """,
                """
                private MyEntity __entityCursorConverter_fooBarMyEntity(Cursor cursor) {
                  MyEntity _entity = new MyEntity();
                  int _columnIndex = 0;
                  for (String _columnName : cursor.getColumnNames()) {
                    switch(_columnName.hashCode()) {
                     case ${"id".hashCode()}: {
                        if ("id".equals(_columnName)) {
                          final int _tmpId;
                          _tmpId = cursor.getInt(_columnIndex);
                          _entity.setId(_tmpId);
                        }
                      }
                      case ${"name".hashCode()}: {
                        if ("name".equals(_columnName)) {
                          _entity.name = cursor.getString(_columnIndex);
                        }
                      }
                      case ${"lastName".hashCode()}: {
                        if ("lastName".equals(_columnName)) {
                          _entity.lastName = cursor.getString(_columnIndex);
                        }
                      }
                      case ${"age".hashCode()}: {
                        if ("age".equals(_columnName)) {
                          _entity.age = cursor.getInt(_columnIndex);
                        }
                      }
                    }
                    _columnIndex ++;
                  }
                  return _entity;
                }
                """.trimIndent())
    }

    fun generateAndMatch(input: String, output : String,
                         attributes: Map<String, String> = mapOf()) {
        generate(input, attributes)
                .compilesWithoutError()
                .and()
                .generatesSources(JavaFileObjects.forSourceString(
                        "foo.bar.MyEntity_CursorConverter",
                        listOf(OUT_PREFIX,output,OUT_SUFFIX).joinToString("\n")))
    }

    fun generate(input: String, attributes: Map<String, String> = mapOf()) : CompileTester {
        return singleEntity(input, attributes) { entity, invocation ->
            val className = ClassName.get("foo.bar","MyContainerClass")
            val writer = object : ClassWriter(className){
                override fun createTypeSpecBuilder(): TypeSpec.Builder {
                    getOrCreateMethod(EntityCursorConverterWriter(entity))
                    return TypeSpec.classBuilder(className).apply {
                        addModifiers(Modifier.PUBLIC)
                    }
                }
            }
            writer.write(invocation.processingEnv)
        }
    }
}
