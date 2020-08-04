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

import androidx.contentaccess.compiler.vo.ContentDeleteVO
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.ProcessingEnvironment

class ContentDeleteMethodWriter(
    val processingEnv: ProcessingEnvironment,
    private val contentDelete: ContentDeleteVO
) {
    private val returnOrSet = if (contentDelete.isSuspend) "" else "return "

    @KotlinPoetMetadataPreview
    fun createContentDeleteMethod(): FunSpec {
        val methodBuilder = funSpecOverriding(contentDelete.method, processingEnv)

        if (contentDelete.isSuspend) {
            val withContext = MemberName("kotlinx.coroutines", "withContext")
            methodBuilder.beginControlFlow("return %M(_coroutineDispatcher)", withContext)
        }

        val selectionArgs = (contentDelete.where?.selectionArgs ?: emptyList())
            .joinToString { CodeBlock.of("%N.toString()", it).toString() }

        val whereCondition = if (contentDelete.where?.selection != null) {
            CodeBlock.of("%S", contentDelete.where.selection).toString()
        } else {
            "null"
        }

        methodBuilder.addStatement("val _where = $whereCondition")
        methodBuilder.addStatement("val _selectionArgs = arrayOf<String>($selectionArgs)")

        methodBuilder
            .addStatement(
                "$returnOrSet _contentResolver.delete(%T.parse(%S), %N, %N)",
                ClassName("android.net", "Uri"),
                contentDelete.uri,
                "_where",
                "_selectionArgs"
            )

        if (contentDelete.isSuspend) {
            methodBuilder.endControlFlow()
        }

        return methodBuilder.build()
    }
}