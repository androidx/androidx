/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.app;

import android.app.GrammaticalInflectionManager;
import android.content.Context;

import androidx.annotation.AnyThread;
import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.os.BuildCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for accessing features in {@link android.app.GrammaticalInflectionManager}.
 */
public final class GrammaticalInflectionManagerCompat {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(flag = true, value = {
            GRAMMATICAL_GENDER_NOT_SPECIFIED,
            GRAMMATICAL_GENDER_NEUTRAL,
            GRAMMATICAL_GENDER_FEMININE,
            GRAMMATICAL_GENDER_MASCULINE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface GrammaticalGender {}

    /**
     * Constant for grammatical gender: to indicate the user has not specified the terms
     * of address for the application.
     *
     * @see android.content.res.Configuration#GRAMMATICAL_GENDER_NOT_SPECIFIED
     */
    public static final int GRAMMATICAL_GENDER_NOT_SPECIFIED = 0;

    /**
     * Constant for grammatical gender: to indicate the terms of address the user
     * preferred in an application is neuter.
     *
     * @see android.content.res.Configuration#GRAMMATICAL_GENDER_NEUTRAL
     */
    public static final int GRAMMATICAL_GENDER_NEUTRAL = 1;

    /**
     * Constant for grammatical gender: to indicate the terms of address the user
     * preferred in an application is feminine.
     *
     * @see android.content.res.Configuration#GRAMMATICAL_GENDER_FEMININE
     */
    public static final int GRAMMATICAL_GENDER_FEMININE = 2;

    /**
     * Constant for grammatical gender: to indicate the terms of address the user
     * preferred in an application is masculine.
     *
     * @see android.content.res.Configuration#GRAMMATICAL_GENDER_MASCULINE
     */
    public static final int GRAMMATICAL_GENDER_MASCULINE = 3;

    private GrammaticalInflectionManagerCompat() {}

   /**
    * Returns the current grammatical gender. No-op on versions prior to
    * {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE}.
    *
    * @param context Context to retrieve service from.
    * @return the grammatical gender if device API level is greater than 33, otherwise, return 0.
    */
    @OptIn(markerClass = androidx.core.os.BuildCompat.PrereleaseSdkCheck.class)
    @AnyThread
    public static int getApplicationGrammaticalGender(@NonNull Context context) {
        if (BuildCompat.isAtLeastU()) {
            return Api34Impl.getApplicationGrammaticalGender(context);
        } else {
            return 0;
        }
    }

    /**
     * Sets the current grammatical gender. No-op on versions prior to
     * {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE}.
     *
     * @param context Context to retrieve service from.
     * @param grammaticalGender the terms of address the user preferred in an application.
     */
    @OptIn(markerClass = androidx.core.os.BuildCompat.PrereleaseSdkCheck.class)
    @AnyThread
    public static void setRequestedApplicationGrammaticalGender(
            @NonNull Context context, @GrammaticalGender int grammaticalGender) {
        if (BuildCompat.isAtLeastU()) {
            Api34Impl.setRequestedApplicationGrammaticalGender(context, grammaticalGender);
        }
    }

    @RequiresApi(34)
    static class Api34Impl {
        private Api34Impl() {}

        @DoNotInline
        static int getApplicationGrammaticalGender(Context context) {
            return getGrammaticalInflectionManager(context).getApplicationGrammaticalGender();
        }

        @DoNotInline
        static void setRequestedApplicationGrammaticalGender(
                Context context, int grammaticalGender) {
            getGrammaticalInflectionManager(context)
                    .setRequestedApplicationGrammaticalGender(grammaticalGender);
        }

        private static GrammaticalInflectionManager getGrammaticalInflectionManager(
                Context context) {
            return context.getSystemService(GrammaticalInflectionManager.class);
        }
    }
}