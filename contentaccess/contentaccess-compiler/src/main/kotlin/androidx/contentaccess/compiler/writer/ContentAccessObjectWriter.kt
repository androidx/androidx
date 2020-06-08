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
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.tag
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import androidx.contentaccess.compiler.utils.JvmSignatureUtil
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class ContentAccessObjectWriter(
    val contentAccessObject: ContentAccessObjectVO,
    val processingEnv:
    ProcessingEnvironment
) {
    @KotlinPoetMetadataPreview
    fun generateFile() {
        val generatedClassName =
            "_${ClassName.bestGuess(contentAccessObject.interfaceName).simpleName}Impl"

        val contentResolverTypePlaceholder = ClassName("android.content", "ContentResolver")
        val generatedClassBuilder = TypeSpec.classBuilder(generatedClassName)
            .addSuperinterface(ClassName.bestGuess(contentAccessObject.interfaceName))
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("contentResolver", contentResolverTypePlaceholder.copy())
                .build())
            .addProperty(PropertySpec.builder("_contentResolver", contentResolverTypePlaceholder
                .copy()).initializer("contentResolver")
                .build())
        for (contentQuery in contentAccessObject.queries) {
            generatedClassBuilder.addFunction(
                ContentQueryMethodWriter(processingEnv)
                    .createContentQueryMethod(contentQuery)!!
            )
        }
        val accessorFile = FileSpec.builder(contentAccessObject.packageName, generatedClassName)
            .addType(generatedClassBuilder.build()).build()
        accessorFile.writeTo(processingEnv.filer)
    }
}

@KotlinPoetMetadataPreview
fun funSpecOverriding(element: ExecutableElement, processingEnv: ProcessingEnvironment):
        FunSpec.Builder {
    val classInspector = ElementsClassInspector.create(processingEnv.elementUtils, processingEnv
        .typeUtils)
    val enclosingClass = element.enclosingElement as TypeElement

    val kotlinApi = enclosingClass.toTypeSpec(classInspector)
    val jvmSignature = JvmSignatureUtil.getMethodDescriptor(element)
    val funSpec = kotlinApi.funSpecs.find {
        it.tag<ImmutableKmFunction>()?.signature?.asString() == jvmSignature
    } ?: error("No matching funSpec found for $jvmSignature.")
    val builder = funSpec.toBuilder()
    builder.modifiers.remove(KModifier.ABSTRACT)
    builder.annotations.removeAll { true }
    builder.addModifiers(KModifier.OVERRIDE)
    return builder
}