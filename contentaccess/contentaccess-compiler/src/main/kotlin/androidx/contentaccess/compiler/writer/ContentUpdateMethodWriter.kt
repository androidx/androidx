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

import androidx.contentaccess.compiler.vo.ContentUpdateVO
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.ProcessingEnvironment

class ContentUpdateMethodWriter(
    val processingEnv: ProcessingEnvironment,
    val contentUpdate: ContentUpdateVO
) {

    val returnOrSet = if (contentUpdate.isSuspend) "" else "return "
    @KotlinPoetMetadataPreview
    fun createContentUpdateMethod(): FunSpec? {
        val uriTypePlaceHolder = ClassName("android.net", "Uri")
        val contentValuesPlaceHolder = ClassName("android.content", "ContentValues")
        val methodBuilder = funSpecOverriding(contentUpdate.method, processingEnv)
        methodBuilder.annotations.add(
            AnnotationSpec.builder(Suppress::class).addMember
                ("%S", "USELESS_CAST").addMember("%S", "UNCHECKED_CAST").addMember("%S",
                "PLATFORM_CLASS_MAPPED_TO_KOTLIN").build())
        if (contentUpdate.isSuspend) {
            val withContext = MemberName("kotlinx.coroutines", "withContext")
            methodBuilder.beginControlFlow(
                "return %M(_queryExecutor)",
                withContext
            )
        }
        if (contentUpdate.uri.startsWith(":")) {
            methodBuilder.addStatement("val _uri = %T.parse(%L)", uriTypePlaceHolder.copy(),
                contentUpdate.uri.removePrefix(":"))
        } else {
            methodBuilder.addStatement("val _uri = %T.parse(%S)", uriTypePlaceHolder.copy(),
                contentUpdate.uri)
        }
        methodBuilder.addStatement("val _contentValues = %T(${contentUpdate.toUpdate
            .size})", contentValuesPlaceHolder)
        for (value in contentUpdate.toUpdate) {
            methodBuilder.addStatement("_contentValues.put(\"${value.first}\", ${value.second})")
        }
        var noSelectionArgs = true
        if (contentUpdate.where != null) {
            methodBuilder.addStatement("val _where = %S", contentUpdate.where.selection)
            val selectionArgs = contentUpdate.where.selectionArgs
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
            methodBuilder.addStatement("val _where = \"\"")
        }
        methodBuilder.addStatement("${returnOrSet}_contentResolver.update(_uri, _contentValues, " +
                "_where, ${if (noSelectionArgs) "null" else "_selectionArgs"})")

        if (contentUpdate.isSuspend) {
            methodBuilder.endControlFlow()
        }
        return methodBuilder.build()
    }
}