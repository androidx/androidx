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

package androidx.pdf.viewer.loader;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.viewer.loader.PdfPageLoader.GetPageLinksTask;
import androidx.pdf.viewer.loader.PdfPageLoader.GetPageTextTask;

/**
 * Utility to disable tasks that are causing crashes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class TaskDenyList {

    public static boolean sDisableLinks = false;

    public static boolean sDisableAltText = false;

    private TaskDenyList() {
        // Static utility.
    }

    /**
     * Disable some of the less important features if they are causing crashes.
     */
    public static void maybeDenyListTask(@NonNull String task) {
        if (GetPageLinksTask.class.getSimpleName().equals(task)) {
            sDisableLinks = true;
        } else if (GetPageTextTask.class.getSimpleName().equals(task)) {
            sDisableAltText = true;
        }
    }
}
