/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file    except in compliance with the License.
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

package androidx.pdf.service

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface PdfRendererWrapper : AutoCloseable {

    /** Caller should use [releasePage] to close the page resource reliably after usage. */
    fun openPage(pageNum: Int, useCache: Boolean): PdfPageWrapper

    /** Closes the page. Also removes and clears the cached instance, if held. */
    fun releasePage(page: PdfPageWrapper?, pageNum: Int)

    val documentLinearizationType: Int
    val pageCount: Int
    val documentFormType: Int
}
