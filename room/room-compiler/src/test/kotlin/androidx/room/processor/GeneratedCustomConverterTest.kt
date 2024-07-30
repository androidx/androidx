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

package androidx.room.processor

import androidx.room.RoomKspProcessor
import androidx.room.RoomProcessor
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.addOriginatingElement
import androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor
import androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room.compiler.processing.util.Source
import androidx.room.runProcessorTestWithK1
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GeneratedCustomConverterTest {

    @Test
    fun generatedConverter() {
        val src =
            Source.kotlin(
                "Sources.kt",
                """
            import androidx.room.*

            @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
            @TypeConverters(Generated_CustomConverters::class)
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getMyDao(): MyDao
            }

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getMyEntity(): MyEntity
            }

            @Entity
            @GenConverter
            data class MyEntity(
              @PrimaryKey val id: Long,
              val data: Foo
            )

            class Foo

            annotation class GenConverter
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(
            sources = listOf(src),
            javacProcessors = listOf(RoomProcessor(), JavacCustomConverter()),
            symbolProcessorProviders =
                listOf(RoomKspProcessor.Provider(), KspCustomConverter.Provider())
        ) {
            it.hasNoWarnings()
        }
    }

    class CustomConverterGenerator : XProcessingStep {
        override fun annotations() = setOf("GenConverter")

        override fun process(
            env: XProcessingEnv,
            elementsByAnnotation: Map<String, Set<XElement>>,
            isLastRound: Boolean
        ): Set<XElement> {
            val elements = elementsByAnnotation.getOrDefault("GenConverter", emptySet())
            val element = elements.singleOrNull() ?: return emptySet()
            val typeSpec =
                TypeSpec.classBuilder("Generated_CustomConverters")
                    .addOriginatingElement(element)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(
                        MethodSpec.methodBuilder("fooToString")
                            .addAnnotation(ClassName.get("androidx.room", "TypeConverter"))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(ClassName.get("java.lang", "String"))
                            .addParameter(ClassName.get("", "Foo"), "f")
                            .addStatement("return \$S", "")
                            .build()
                    )
                    .addMethod(
                        MethodSpec.methodBuilder("stringToFoo")
                            .addAnnotation(ClassName.get("androidx.room", "TypeConverter"))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(ClassName.get("", "Foo"))
                            .addParameter(ClassName.get("java.lang", "String"), "s")
                            .addStatement("return new Foo()")
                            .build()
                    )
                    .build()
            val javaFile = JavaFile.builder("", typeSpec).build()
            env.filer.write(javaFile, XFiler.Mode.Isolating)
            return emptySet()
        }
    }

    class JavacCustomConverter : JavacBasicAnnotationProcessor() {
        override fun processingSteps() = listOf(CustomConverterGenerator())

        override fun getSupportedSourceVersion() = SourceVersion.latest()
    }

    class KspCustomConverter(env: SymbolProcessorEnvironment) : KspBasicAnnotationProcessor(env) {
        override fun processingSteps() = listOf(CustomConverterGenerator())

        class Provider : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment) =
                KspCustomConverter(environment)
        }
    }
}
