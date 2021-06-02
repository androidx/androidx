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
package androidx.room.solver.binderprovider

import androidx.room.compiler.processing.XType
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.query.result.QueryResultBinder
import com.squareup.javapoet.TypeName

/**
 * Common functionality for binder providers that require an additional artifact
 */
fun QueryResultBinderProvider.requireArtifact(
    context: Context,
    requiredType: TypeName,
    missingArtifactErrorMsg: String
): QueryResultBinderProvider = QueryResultBinderProviderWithRequiredArtifact(
    context = context,
    requiredType = requiredType,
    missingArtifactErrorMsg = missingArtifactErrorMsg,
    delegate = this
)

private class QueryResultBinderProviderWithRequiredArtifact(
    val context: Context,
    val requiredType: TypeName,
    val missingArtifactErrorMsg: String,
    val delegate: QueryResultBinderProvider
) : QueryResultBinderProvider {
    private val hasRequiredArtifact by lazy(LazyThreadSafetyMode.NONE) {
        context.processingEnv.findTypeElement(requiredType) != null
    }

    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        return delegate.provide(declared, query)
    }

    override fun matches(declared: XType): Boolean {
        val result = delegate.matches(declared)
        if (result && !hasRequiredArtifact) {
            context.logger.e(missingArtifactErrorMsg)
        }
        return result
    }
}