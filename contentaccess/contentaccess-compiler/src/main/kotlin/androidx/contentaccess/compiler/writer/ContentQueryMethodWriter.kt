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

import androidx.contentaccess.compiler.processor.PojoProcessor
import androidx.contentaccess.compiler.processor.warn
import androidx.contentaccess.compiler.vo.ContentColumnVO
import androidx.contentaccess.compiler.vo.ContentQueryVO
import androidx.contentaccess.ext.hasNonEmptyNonPrivateNonIgnoredConstructor
import asTypeElement
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
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

class ContentQueryMethodWriter(
    val processingEnv: ProcessingEnvironment,
    val contentQuery: ContentQueryVO
) {
    val returnOrSet = if (contentQuery.isSuspend) "" else "return "

    @KotlinPoetMetadataPreview
    fun createContentQueryMethod(): FunSpec? {
        val uriTypePlaceHolder = ClassName("android.net", "Uri")
        val methodBuilder = funSpecOverriding(contentQuery.method, processingEnv)
        methodBuilder.annotations.add(AnnotationSpec.builder(Suppress::class).addMember
            ("%S", "USELESS_CAST").addMember("%S", "UNCHECKED_CAST").addMember("%S",
            "PLATFORM_CLASS_MAPPED_TO_KOTLIN").build())
        if (contentQuery.isSuspend) {
            val withContext = MemberName("kotlinx.coroutines", "withContext")
            methodBuilder.beginControlFlow(
                "return %M(_coroutineDispatcher)",
                withContext
            )
        }
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
        if (contentQuery.selection != null) {
            methodBuilder.addStatement("val _selection = %S", contentQuery.selection.selection)
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
        } else {
            methodBuilder.addStatement("val _selection = \"\"")
        }
        callResolverAndFormulateReturn(methodBuilder, contentQuery.returnType,
            noSelectionArgs)

        if (contentQuery.isSuspend) {
            methodBuilder.endControlFlow()
        }
        return methodBuilder.build()
    }

    fun callResolverAndFormulateReturn(
        methodBuilder: FunSpec.Builder,
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
            val returnTypeInKotlin = methodBuilder.build().returnType.toString()
            if (returnType.isOptional()) {
                populateAndReturnOptionalFromCursor(methodBuilder, realReturnType,
                    checkIfTypeArgumentIsNullable(returnTypeInKotlin))
            } else if (returnType.isList()) {
                populateAndReturnListFromCursor(methodBuilder, realReturnType,
                    checkIfTypeArgumentIsNullable(returnTypeInKotlin))
            } else if (returnType.isSet()) {
                populateAndReturnSetFromCursor(methodBuilder, realReturnType,
                    checkIfTypeArgumentIsNullable(returnTypeInKotlin))
            }
        } else {
            returnNonPojoTypeFromCursor(methodBuilder, returnType)
        }
    }

    fun populateAndReturnListFromCursor(
        methodBuilder: FunSpec.Builder,
        realReturnType: TypeMirror,
        typeArgumentIsNullable: Boolean
    ) {
        methodBuilder.addStatement("val _returnList = %T()",
            ClassName("kotlin.collections", "ArrayList").parameterizedBy(realReturnType
                .toKotlinClassName().copy(typeArgumentIsNullable)))
        methodBuilder.beginControlFlow("while (_cursor.moveToNext())")
        createReturnTypeFromCursor(realReturnType, methodBuilder, contentQuery.toQueryFor)
        // Check !realReturnType.isSupportedColumnType() because POJOs cannot be null, their
        // fields can be, but that's a different check. We only check nullability when the return
        // type is a supported cursor type (long, int, String etc...) Same goes for other
        // collections.
        if (typeArgumentIsNullable || !realReturnType.isSupportedColumnType()) {
            methodBuilder.addStatement("_returnList.add($RETURN_OBJECT_NAME as %T)",
                realReturnType
                    .toKotlinClassName())
        } else {
            methodBuilder.beginControlFlow("if ($RETURN_OBJECT_NAME != null)")
            methodBuilder.addStatement("_returnList.add($RETURN_OBJECT_NAME as %T)",
                realReturnType
                    .toKotlinClassName())
            methodBuilder.endControlFlow()
        }
        methodBuilder.endControlFlow()
        methodBuilder.addStatement("$returnOrSet _returnList.toList() as ${methodBuilder.build()
            .returnType}")
    }

    fun populateAndReturnSetFromCursor(
        methodBuilder: FunSpec.Builder,
        realReturnType: TypeMirror,
        typeArgumentIsNullable: Boolean
    ) {
        methodBuilder.addStatement("val _returnSet = %T()",
            ClassName("kotlin.collections", "HashSet").parameterizedBy(realReturnType
                .toKotlinClassName().copy(typeArgumentIsNullable)).copy())
        methodBuilder.beginControlFlow("while (_cursor.moveToNext())")
        createReturnTypeFromCursor(realReturnType, methodBuilder, contentQuery.toQueryFor)
        if (typeArgumentIsNullable || !realReturnType.isSupportedColumnType()) {
            methodBuilder.addStatement("_returnSet.add($RETURN_OBJECT_NAME as %T)",
                realReturnType.toKotlinClassName())
        } else {
            methodBuilder.beginControlFlow("if ($RETURN_OBJECT_NAME != null)")
            methodBuilder.addStatement("_returnSet.add($RETURN_OBJECT_NAME as %T)",
                realReturnType.toKotlinClassName())
            methodBuilder.endControlFlow()
        }

        methodBuilder.endControlFlow()
        methodBuilder.addStatement("$returnOrSet _returnSet.toSet() as ${methodBuilder.build()
            .returnType}")
    }

    fun populateAndReturnOptionalFromCursor(
        methodBuilder: FunSpec.Builder,
        realReturnType: TypeMirror,
        typeArgumentIsNullable: Boolean
    ) {
        if (typeArgumentIsNullable) {
            processingEnv.warn("Type argument $realReturnType of java.util.Optional is marked as " +
                    "nullable, an Optional cannot contain a null reference, instead it will be " +
                    "empty.", contentQuery.method)
        }
        methodBuilder.beginControlFlow("if (_cursor.moveToNext())")
        createReturnTypeFromCursor(realReturnType, methodBuilder, contentQuery.toQueryFor)
        methodBuilder.addStatement("$returnOrSet %T.ofNullable($RETURN_OBJECT_NAME as %T) as %L",
            ClassName("java.util", "Optional").copy(), realReturnType.toKotlinClassName(),
            methodBuilder.build().returnType.toString())
        methodBuilder.nextControlFlow("else")
            .addStatement("$returnOrSet Optional.empty<%T>() as %L",
                realReturnType.toKotlinClassName(),
                methodBuilder.build().returnType.toString())
            .endControlFlow()
    }

    fun returnNonPojoTypeFromCursor(
        methodBuilder: FunSpec.Builder,
        returnType: TypeMirror
    ) {
        methodBuilder.beginControlFlow("if (_cursor.moveToNext())")
        createReturnTypeFromCursor(returnType, methodBuilder, contentQuery.toQueryFor)
        methodBuilder.addStatement("$returnOrSet $RETURN_OBJECT_NAME as ${methodBuilder.build()
            .returnType}")
        methodBuilder.nextControlFlow("else")
            .addStatement("$returnOrSet null")
            .endControlFlow()
    }

    fun createReturnTypeFromCursor(
        realReturnType: TypeMirror,
        methodBuilder: FunSpec.Builder,
        columns: List<ContentColumnVO>
    ) {
        if (realReturnType.isSupportedColumnType()) {
            // This isn't a pojo but a single column, get that type directly.
            methodBuilder.beginControlFlow("val %L = if (_cursor.isNull(0))", RETURN_OBJECT_NAME)
            methodBuilder.addStatement("null")
            methodBuilder.nextControlFlow("else")
            methodBuilder.addStatement("_cursor.${realReturnType.getCursorMethod()}(0)")
            methodBuilder.endControlFlow()
            return
        }
        val pojo = PojoProcessor(realReturnType).process()
        val pojoColumnsToFieldNames = pojo.pojoFields.map { it.columnName to it.name }.toMap()
        if (realReturnType.asTypeElement().hasNonEmptyNonPrivateNonIgnoredConstructor()) {
            val constructorParams = ArrayList<String>()

            val columnNamesBeingSelected = columns.map { it.columnName }
            val unPopulatedPojoFields = pojo.pojoFields.filter { it.columnName !in
                    columnNamesBeingSelected }.map { it.name }
            for ((currIndex, column) in columns.withIndex()) {
                if (column.isNullable) {
                    methodBuilder.beginControlFlow("val _${pojoColumnsToFieldNames
                        .get(column.columnName)}_value = if (_cursor.isNull($currIndex))")
                    methodBuilder.addStatement("null")
                    methodBuilder.nextControlFlow("else")
                    methodBuilder.addStatement("_cursor" +
                            ".${column.type.getCursorMethod()}($currIndex)")
                    methodBuilder.endControlFlow()
                } else {
                    methodBuilder.beginControlFlow("val _${pojoColumnsToFieldNames.get(column
                        .columnName)}_value" +
                            " = if (_cursor.isNull($currIndex))")
                    methodBuilder.addStatement("throw NullPointerException(%S)", "Column ${column
                        .columnName} associated with field ${column.name} in $realReturnType " +
                            "return null, however field ${column.name} is not nullable")
                    methodBuilder.nextControlFlow("else")
                    methodBuilder.addStatement("_cursor" +
                            ".${column.type.getCursorMethod()}($currIndex)")
                    methodBuilder.endControlFlow()
                }
                constructorParams.add("${pojoColumnsToFieldNames.get(column.columnName)}" +
                        " = _${pojoColumnsToFieldNames.get(column.columnName)}_value")
            }
            for (unPopoulatedPojoField in unPopulatedPojoFields) {
                constructorParams.add("$unPopoulatedPojoField = null")
            }
            methodBuilder.addStatement("val $RETURN_OBJECT_NAME = %T(%L)", realReturnType,
                constructorParams.joinToString(","))
        } else {
            // We should instead assign to public fields directly.
            methodBuilder.addStatement("val $RETURN_OBJECT_NAME = %T()", realReturnType)
            for ((currIndex, column) in columns.withIndex()) {
                if (column.isNullable) {
                    methodBuilder.beginControlFlow("if (!_cursor.isNull($currIndex))")
                    methodBuilder.addStatement("$RETURN_OBJECT_NAME.${pojoColumnsToFieldNames
                        .get(column.columnName)} =" +
                            " _cursor.${column.type.getCursorMethod()}($currIndex)")
                    methodBuilder.endControlFlow()
                } else {
                    methodBuilder.beginControlFlow("if (_cursor.isNull($currIndex))")
                    methodBuilder.addStatement("throw NullPointerException(%S)", "Column ${column
                        .columnName} associated with field ${column.name} in $realReturnType " +
                            "return null, however field ${column.name} is not nullable")
                    methodBuilder.nextControlFlow("else")
                    methodBuilder.addStatement("$RETURN_OBJECT_NAME.${pojoColumnsToFieldNames
                        .get(column.columnName)} = " +
                            "_cursor.${column.type.getCursorMethod()}($currIndex)")
                    methodBuilder.endControlFlow()
                }
            }
        }
    }

    fun checkIfTypeArgumentIsNullable(returnTypeInKotlin: String): Boolean {
        // TODO(obenabde): Hummmm beatiful code... is there a better way to do this.
        return returnTypeInKotlin.substringAfterLast("<").substringBeforeLast(">").endsWith("?")
    }
}

internal val RETURN_OBJECT_NAME = "_returnObject"