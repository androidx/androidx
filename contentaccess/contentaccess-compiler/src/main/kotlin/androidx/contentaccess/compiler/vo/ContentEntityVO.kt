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

package androidx.contentaccess.compiler.vo

import javax.lang.model.type.DeclaredType

// Represents a column in a content provider
data class ContentEntityVO(
    val defaultUri: String,
    val type: DeclaredType,
    // TODO(obenabde): maybe eventually make this a map where the key is the field name for a
    // slight optimization. Trying to keep it simple for now. This should still contain the
    // primary key column.
    val columns: Map<String, ContentColumnVO>,
    val primaryKeyColumn: ContentColumnVO
)