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

package androidx.contentaccess.compiler.writer

import androidx.contentaccess.compiler.vo.ContentAccessObjectVO
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import androidx.contentaccess.ext.getKotlinFunspec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement

class ContentAccessObjectWriter(
    val contentAccessObject: ContentAccessObjectVO,
    val processingEnv: ProcessingEnvironment
) {
    @KotlinPoetMetadataPreview
    fun generateFile() {
        // We do this instead of getting the simple name of the class in case of nested classes.
        val generatedClassName =
            "${contentAccessObject.interfaceName.removePrefix(contentAccessObject.packageName)
                .replace(".", "_")}Impl"
        val fileSpecBuilder = FileSpec.builder(contentAccessObject.packageName, generatedClassName)
        val contentResolverTypePlaceholder = ClassName("android.content", "ContentResolver")
        val coroutineDispatcherTypePlaceholder = ClassName("kotlinx.coroutines",
            "CoroutineDispatcher")
        val generatedClassBuilder = TypeSpec.classBuilder(generatedClassName)
            .addSuperinterface(ClassName.bestGuess(contentAccessObject.interfaceName))
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("contentResolver", contentResolverTypePlaceholder)
                .addParameter("coroutineDispatcher", coroutineDispatcherTypePlaceholder)
                .build())
            .addProperty(PropertySpec.builder("_contentResolver",
                contentResolverTypePlaceholder).initializer("contentResolver").build())
            .addProperty(PropertySpec.builder("_coroutineDispatcher",
                coroutineDispatcherTypePlaceholder)
                .initializer("coroutineDispatcher").build())

        for (contentQuery in contentAccessObject.queries) {
            generatedClassBuilder.addFunction(
                ContentQueryMethodWriter(processingEnv, contentQuery)
                    .createContentQueryMethod()!!
            )
        }
        for (contentUpdate in contentAccessObject.updates) {
            generatedClassBuilder.addFunction(
                ContentUpdateMethodWriter(processingEnv, contentUpdate)
                    .createContentUpdateMethod()!!
            )
        }

        for (contentDelete in contentAccessObject.deletes) {
            generatedClassBuilder.addFunction(
                ContentDeleteMethodWriter(processingEnv, contentDelete)
                    .createContentDeleteMethod()
            )
        }

        for (contentInsert in contentAccessObject.inserts) {
            generatedClassBuilder.addFunction(
                ContentInsertMethodWriter(processingEnv, contentInsert)
                    .createContentDeleteMethod()
            )
        }

        val accessorFile = fileSpecBuilder.addType(generatedClassBuilder.build()).build()
        accessorFile.writeTo(processingEnv.filer)
    }
}

@KotlinPoetMetadataPreview
fun funSpecOverriding(element: ExecutableElement, processingEnv: ProcessingEnvironment):
        FunSpec.Builder {
    val builder = element.getKotlinFunspec(processingEnv).toBuilder()
    builder.modifiers.remove(KModifier.ABSTRACT)
    builder.annotations.removeAll { true }
    builder.addModifiers(KModifier.OVERRIDE)
    return builder
}