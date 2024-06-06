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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Utility functions for dealing with intents.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Intents {

    /**
     * Private constructor to prevent instantiation
     */
    private Intents() {}
    private static final String TAG = Intents.class.getSimpleName();

    /** A safe version of {@link Context#startActivity} that handles exceptions. */
    public static boolean startActivity(@NonNull Context context, @NonNull String logTag,
            @NonNull Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     *
     */
    @NonNull
    public static String toLongString(@Nullable Intent intent) {
        StringBuilder builder = new StringBuilder();
        String separator = ", ";
        if (intent != null) {
            builder.append(intent.getAction());
            builder.append(separator);
            ComponentName component = intent.getComponent();
            if (component != null) {
                builder.append(intent.getComponent().getShortClassName());
                builder.append(separator);
                builder.append(intent.getComponent().getPackageName());
            } else {
                builder.append("component null");
            }
        }
        return builder.toString();
    }
}
