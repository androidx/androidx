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
import androidx.contentaccess.compiler.vo.ContentQueryVO
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier

class ContentAccessObjectWriter(
    val contentAccessObject: ContentAccessObjectVO,
    val processingEnv:
    ProcessingEnvironment
) {

    // TODO(obenabde) This entire class assumes verifications have been done!
    // TODO(obenabde) Factor some of the stuff here into ext etc... once we start supporting
    // more stuff and need to reuse some of the logic/classes here.
    fun generateFile() {
        val generatedClassName =
            "_${ClassName.bestGuess(contentAccessObject.interfaceName).simpleName()}Impl"

        val contentResolverTypePlaceholder = ClassName.get("android.content", "ContentResolver")
        val generatedClassBuilder = TypeSpec.classBuilder(generatedClassName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(contentAccessObject.interfaceType)!!
            .addField(FieldSpec.builder(contentResolverTypePlaceholder.box(), "_contentResolver")
                .build())
            .addMethod(MethodSpec.constructorBuilder().addParameter(ParameterSpec.builder
                (contentResolverTypePlaceholder.box(), "contentResolver").build()).addStatement
                ("_contentResolver = contentResolver").build())

        for (contentQuery in contentAccessObject.queries) {
            generatedClassBuilder.addMethod(createContentQueryMethod(contentQuery))
        }
        val accessorFile = JavaFile.builder(contentAccessObject.packageName,
            generatedClassBuilder.build()).build()
        accessorFile.writeTo(processingEnv.filer)
    }

    fun createContentQueryMethod(contentQuery: ContentQueryVO):
            MethodSpec? {
        val uriTypePlaceHolder = ClassName.get("android.net", "Uri")
        val cursorTypePlaceHolder = ClassName.get("android.database", "Cursor")
        val methodBuilder = MethodSpec.methodBuilder(contentQuery.name)
            .addModifiers(Modifier.PUBLIC)
        for (param in contentQuery.parameters) {
            methodBuilder.addParameter(ParameterSpec.builder(
                TypeName.get(param
                    .asType()), param
                .simpleName.toString()).build())
        }
        methodBuilder.returns(TypeName.get(contentQuery.returnType))
        methodBuilder.addStatement("\$T uri = \$T.parse(\$S)", uriTypePlaceHolder,
            uriTypePlaceHolder, contentQuery.uri)
        methodBuilder.addStatement("\$T projection = new String[${contentQuery.toQueryFor.size}]",
            Array<String>::class.java)
        for (i in contentQuery.toQueryFor.indices) {
            methodBuilder
                .addStatement("projection[$i] = \$S", contentQuery.toQueryFor[i].columnName)
        }
        methodBuilder.addStatement("\$T selection = null", String::class.java)
        methodBuilder.addStatement("\$T selectionArgs = null", Array<String>::class.java)
        if (contentQuery.selection != null) {
            methodBuilder.addStatement("selection = \$S", contentQuery.selection.selection)
            val selectionArgs = contentQuery.selection.selectionArgs
            if (selectionArgs.isNotEmpty()) {
                methodBuilder.addStatement("selectionArgs = new String[${selectionArgs.size}]")
                for (i in selectionArgs.indices) {
                    methodBuilder.addStatement("selectionArgs[$i] = String.valueOf" +
                            "(${selectionArgs[i]})")
                }
            }
        }
        methodBuilder.addStatement("\$T cursor = _contentResolver.query(uri, projection, " +
                "selection, selectionArgs, null)",
            cursorTypePlaceHolder)
//        if (contentQuery.returnTypeIsDeclared) {
//
//        } else {
//            if (contentQuery.returnType.isSupportedWrapper()) {
//
//            } else {
//                methodBuilder.addStatement("cursor.moveToNext()")
//                methodBuilder.addStatement("return cursor.${nonDeclaredTypeCursorMethod(contentQuery
//                    .returnType)}(0)")
//            }
//        }
        return methodBuilder.build()
    }
}
