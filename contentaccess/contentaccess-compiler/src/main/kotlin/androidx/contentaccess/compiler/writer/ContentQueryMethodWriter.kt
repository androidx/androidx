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

import androidx.contentaccess.compiler.vo.ContentColumnVO
import androidx.contentaccess.compiler.vo.ContentQueryVO
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import extractIntendedReturnType
import getCursorMethod
import isList
import isOptional
import isSet
import isSupportedColumnType
import isSupportedGenericType
import toKotlinClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeMirror

class ContentQueryMethodWriter(val processingEnv: ProcessingEnvironment) {

    @KotlinPoetMetadataPreview
    fun createContentQueryMethod(contentQuery: ContentQueryVO): FunSpec? {
        val uriTypePlaceHolder = ClassName("android.net", "Uri")
        val methodBuilder = funSpecOverriding(contentQuery.method, processingEnv)
        methodBuilder.annotations.add(AnnotationSpec.builder(Suppress::class).addMember
            ("%S", "USELESS_CAST").build())
        if (contentQuery.uri.startsWith(":")) {
            methodBuilder.addStatement("val _uri = %T.parse(%L)", uriTypePlaceHolder.copy(),
                contentQuery.uri.removePrefix(":"))
        } else {
            methodBuilder.addStatement("val _uri = %T.parse(%S)", uriTypePlaceHolder.copy(),
                contentQuery.uri)
        }
        methodBuilder.addStatement("val _projection = %T(${contentQuery.toQueryFor.size}, {" +
                "\"\"})",
            ClassName("kotlin", "Array").parameterizedBy(ClassName("kotlin", "String")))
        for (i in contentQuery.toQueryFor.indices) {
            methodBuilder
                .addStatement("_projection[$i] = %S", contentQuery.toQueryFor[i].columnName)
        }
        var noSelectionArgs = true
        methodBuilder.addStatement("var _selection : String")
        if (contentQuery.selection != null) {
            methodBuilder.addStatement("_selection = %S", contentQuery.selection.selection)
            val selectionArgs = contentQuery.selection.selectionArgs
            val joinedSelectionArgs = if (selectionArgs.size == 1) {
                "${selectionArgs[0]}.toString()"
            } else {
                selectionArgs.map { r -> "$r.toString()" }.joinToString(",")
            }
            if (selectionArgs.isNotEmpty()) {
                noSelectionArgs = false
                methodBuilder.addStatement("val _selectionArgs = arrayOf($joinedSelectionArgs)")
            }
        }
        callResolverAndFormulateReturn(methodBuilder, contentQuery, contentQuery.returnType,
            noSelectionArgs)

        return methodBuilder.build()
    }

    fun callResolverAndFormulateReturn(
        methodBuilder: FunSpec.Builder,
        contentQuery:
            ContentQueryVO,
        returnType: TypeMirror,
        noSelectionArgs: Boolean
    ) {
        methodBuilder.addStatement("val _cursor = _contentResolver.query(_uri, _projection, " +
                "_selection, ${if (noSelectionArgs) "null" else "_selectionArgs"}, %S)",
            contentQuery.orderBy)
        methodBuilder.beginControlFlow("if (_cursor == null)")
        methodBuilder.addStatement("throw NullPointerException(%S)", "Cursor returned by the " +
                "content provider was null!")
        methodBuilder.endControlFlow()
        if (returnType.isSupportedGenericType()) {
            val realReturnType = returnType.extractIntendedReturnType()
            if (returnType.isOptional()) {
                populateAndReturnOptionalFromCursor(methodBuilder, realReturnType, contentQuery)
            } else if (returnType.isList()) {
                populateAndReturnListFromCursor(methodBuilder, realReturnType, contentQuery)
            } else if (returnType.isSet()) {
                populateAndReturnSetFromCursor(methodBuilder, realReturnType, contentQuery)
            }
        } else {
            returnNonPojoTypeFromCursor(methodBuilder, returnType, contentQuery)
        }
    }

    fun populateAndReturnListFromCursor(
        methodBuilder: FunSpec.Builder,
        realReturnType:
            TypeMirror,
        contentQuery: ContentQueryVO
    ) {
        methodBuilder.addStatement("val _returnList = %T()",
            ClassName("kotlin.collections", "ArrayList").parameterizedBy(realReturnType
                .toKotlinClassName()))
        methodBuilder.beginControlFlow("while (_cursor.moveToNext())")
        createReturnTypeFromCursor(realReturnType, methodBuilder, contentQuery.toQueryFor)
        methodBuilder.addStatement("_returnList.add($RETURN_OBJECT_NAME)")
        methodBuilder.endControlFlow()
        methodBuilder.addStatement("return _returnList.toList() as ${methodBuilder.build()
            .returnType}")
    }

    fun populateAndReturnSetFromCursor(
        methodBuilder: FunSpec.Builder,
        realReturnType:
            TypeMirror,
        contentQuery: ContentQueryVO
    ) {
        methodBuilder.addStatement("val _returnSet = %T()",
            ClassName("kotlin.collections", "HashSet").parameterizedBy(ClassName.bestGuess
                (realReturnType
                .toString())).copy())
        methodBuilder.beginControlFlow("while (_cursor.moveToNext())")
        createReturnTypeFromCursor(realReturnType, methodBuilder, contentQuery.toQueryFor)
        methodBuilder.addStatement("_returnSet.add($RETURN_OBJECT_NAME)")
        methodBuilder.endControlFlow()
        methodBuilder.addStatement("return _returnSet.toSet() as ${methodBuilder.build()
            .returnType}")
    }

    fun populateAndReturnOptionalFromCursor(
        methodBuilder: FunSpec.Builder,
        realReturnType:
            TypeMirror,
        contentQuery: ContentQueryVO
    ) {
        methodBuilder.beginControlFlow("if (_cursor.moveToNext())")
        createReturnTypeFromCursor(realReturnType, methodBuilder, contentQuery.toQueryFor)
        methodBuilder.addStatement("return %T.of($RETURN_OBJECT_NAME)", ClassName("java" +
                ".util", "Optional").copy())
        methodBuilder.nextControlFlow("else")
            .addStatement("return Optional.empty()")
            .endControlFlow()
    }

    fun returnNonPojoTypeFromCursor(
        methodBuilder: FunSpec.Builder,
        returnType:
            TypeMirror,
        contentQuery: ContentQueryVO
    ) {
        methodBuilder.beginControlFlow("if (_cursor!!.moveToNext())")
        createReturnTypeFromCursor(returnType, methodBuilder, contentQuery.toQueryFor)
        methodBuilder.addStatement("return $RETURN_OBJECT_NAME as ${methodBuilder.build()
            .returnType}")
        methodBuilder.nextControlFlow("else")
            .addStatement("return null")
            .endControlFlow()
    }

    fun createReturnTypeFromCursor(
        returnType: TypeMirror,
        methodBuilder: FunSpec.Builder,
        columns: List<ContentColumnVO>
    ) {
        if (returnType.isSupportedColumnType()) {
            // This isn't a pojo but a single column, get that type directly.
            methodBuilder.addStatement("val %L : %T = %L", RETURN_OBJECT_NAME,
                returnType.toKotlinClassName(), "_cursor.${returnType.getCursorMethod()}(0)")
            return
        }
        val constructorParams = ArrayList<String>()
        for ((currIndex, column) in columns.withIndex()) {
            constructorParams.add("${column.name} = _cursor.${column.type
                .getCursorMethod()}($currIndex)")
        }
        methodBuilder.addStatement("val $RETURN_OBJECT_NAME = %T(%L)", returnType,
            constructorParams.joinToString(","))
    }
}

internal val RETURN_OBJECT_NAME = "_returnObject"