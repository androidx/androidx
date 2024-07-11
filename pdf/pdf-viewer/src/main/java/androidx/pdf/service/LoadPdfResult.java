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

package androidx.pdf.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.util.Preconditions;

/**
 * A struct that holds either a successfully loaded PdfDocument, or the reason why it failed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LoadPdfResult {

    private final PdfStatus mStatus;
    @Nullable
    private final PdfDocument mPdfDocument;

    public LoadPdfResult(int status, @Nullable PdfDocument pdfDocument) {
        if (status == PdfStatus.LOADED.getNumber()) {
            Preconditions.checkArgument(pdfDocument != null, "Missing pdfDocument");
        } else {
            Preconditions.checkArgument(pdfDocument == null,
                    "Shouldn't construct " + "broken pdfDocument");
        }
        this.mStatus = PdfStatus.values()[status];
        this.mPdfDocument = pdfDocument;
    }

    @NonNull
    public PdfStatus getStatus() {
        return mStatus;
    }

    @Nullable
    public PdfDocument getPdfDocument() {
        return mPdfDocument;
    }

    public boolean isLoaded() {
        return mStatus == PdfStatus.LOADED;
    }
}
