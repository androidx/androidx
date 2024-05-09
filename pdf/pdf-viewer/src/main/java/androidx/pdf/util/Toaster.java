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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Pops a toast. Avoids using static Toast.makeText which is harder to test.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Toaster {
    /** Use this one for a long toast. */
    public static final Toaster LONG = new Toaster(Toast.LENGTH_LONG);

    /** Use this one for a short toast. */
    public static final Toaster SHORT = new Toaster(Toast.LENGTH_SHORT);

    private final int mDuration;

    private Toaster(int duration) {
        this.mDuration = duration;
    }

    /**
     *
     */
    public void popToast(@NonNull Context context, int resId, @NonNull Object... args) {
        popToast(context, context.getString(resId, args));
    }

    /**
     *
     */
    public void popToast(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, message, mDuration).show();
    }
}
