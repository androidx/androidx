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

package androidx.pdf.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.Objects;

/**
 * Utility class for enabling the functionality of Edit FAB.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AnnotationUtils {
    @VisibleForTesting
    public static final String ACTION_ANNOTATE_PDF = "android.intent.action.ANNOTATE";

    @VisibleForTesting
    public static final String PDF_MIME_TYPE = "application/pdf";

    private AnnotationUtils() {}

    /** Returns true if there is an activity that can resolve the annotation intent else false. */
    public static boolean resolveAnnotationIntent(@NonNull Context context, @NonNull Uri uri) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(uri);
        Intent intent = getAnnotationIntent(uri);
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    /** Returns an instance of intent from the annotation action for a given uri. */
    @NonNull
    public static Intent getAnnotationIntent(@NonNull Uri uri) {
        Objects.requireNonNull(uri);

        Intent intent = new Intent(ACTION_ANNOTATE_PDF);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, PDF_MIME_TYPE);
        return intent;
    }
}
