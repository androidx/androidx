/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.search.model

import androidx.annotation.RestrictTo

/** A model class that holds the index of a data element within [QueryResults]'s resultBounds. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class QueryResultsIndex(

    /** The page number of the document where the current search result is located. */
    public val pageNum: Int,

    /** The index of the search result on the page specified by [pageNum]. */
    public val resultBoundsIndex: Int,
)
