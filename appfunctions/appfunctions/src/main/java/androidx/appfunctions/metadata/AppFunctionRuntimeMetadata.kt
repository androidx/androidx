/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.metadata

import androidx.annotation.RestrictTo
import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import com.android.extensions.appfunctions.AppFunctionManager

/** A mirror Document class of [android.app.appfunctions.AppFunctionRuntimeMetadata]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Document
public data class AppFunctionRuntimeMetadata(
    @Document.Id val id: String,
    @Document.Namespace val namespace: String,
    @Document.StringProperty val functionId: String,
    @Document.StringProperty val packageName: String,
    @Document.LongProperty @AppFunctionManager.EnabledState val enabled: Long,
    @Document.StringProperty(
        joinableValueType = AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID,
        indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS,
    )
    val appFunctionStaticMetadataQualifiedId: String,
) {
    public companion object {
        public const val STATIC_METADATA_JOIN_PROPERTY: String =
            "appFunctionStaticMetadataQualifiedId"
    }
}
