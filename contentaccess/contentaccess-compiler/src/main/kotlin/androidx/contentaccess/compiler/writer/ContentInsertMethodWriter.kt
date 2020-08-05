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

import androidx.contentaccess.compiler.vo.ContentInsertVO
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.ProcessingEnvironment

class ContentInsertMethodWriter(
    val processingEnv: ProcessingEnvironment,
    private val contentInsert: ContentInsertVO
) {
    // TODO(yrezgui): Handle no return
    // TODO(yrezgui): Raise error if set return type isn't URI?
    private val returnOrSet = if (contentInsert.isSuspend) "" else "return "

    @KotlinPoetMetadataPreview
    fun createContentDeleteMethod(): FunSpec {
        val methodBuilder = funSpecOverriding(contentInsert.method, processingEnv)

        if (contentInsert.isSuspend) {
            val withContext = MemberName("kotlinx.coroutines", "withContext")
            methodBuilder.beginControlFlow("return %M(_coroutineDispatcher)", withContext)
        }

        methodBuilder.beginControlFlow(
            "val newValues = %T().apply",
            ClassName("android.content", "ContentValues")
        )

        for (column in contentInsert.columns) {
            if (!column.isNullable) {
                methodBuilder.addStatement(
                    "put(%2S, %1N.%3N)\n",
                    contentInsert.parameterName,
                    column.columnName,
                    column.name
                )
            } else {
                methodBuilder.addStatement(
                    "if (%1N.%3N != null) put(%2S, %1N.%3N) else putNull(%2S)\n",
                    contentInsert.parameterName,
                    column.columnName,
                    column.name
                )
            }
        }

        methodBuilder.endControlFlow()

        methodBuilder
            .addStatement(
                "$returnOrSet _contentResolver.insert(%T.parse(%S), %N)",
                ClassName("android.net", "Uri"),
                contentInsert.uri,
                "newValues"
            )

        if (contentInsert.isSuspend) {
            methodBuilder.endControlFlow()
        }

        return methodBuilder.build()
    }
}