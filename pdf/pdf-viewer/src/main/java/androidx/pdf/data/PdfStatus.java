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

package androidx.pdf.data;

import androidx.annotation.RestrictTo;

// The loading status of a PDF file.
@RestrictTo(RestrictTo.Scope.LIBRARY)
public enum PdfStatus {
    NONE(0), REQUIRES_PASSWORD(1), LOADED(2), PDF_ERROR(3), FILE_ERROR(4), NEED_MORE_DATA(5),

    // Set if pdfClient chokes on one page of the document.
    PAGE_BROKEN(6);

    private final int mStatus;

    PdfStatus(int status) {
        this.mStatus = status;
    }

    public final int getNumber() {
        return mStatus;
    }
}
