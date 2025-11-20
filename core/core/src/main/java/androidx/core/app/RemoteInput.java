/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper for using the {@link android.app.RemoteInput}.
 */
public final class RemoteInput {

    /** Label used to denote the clip data type used for remote input transport */
    public static final String RESULTS_CLIP_LABEL = "android.remoteinput.results";

    /** Extra added to a clip data intent object to hold the text results bundle. */
    public static final String EXTRA_RESULTS_DATA = "android.remoteinput.resultsData";

    /** Extra added to a clip data intent object to hold the data results bundle. */
    private static final String EXTRA_DATA_TYPE_RESULTS_DATA =
            "android.remoteinput.dataTypeResultsData";

    /** Extra added to a clip data intent object identifying the {@link Source} of the results. */
    private static final String EXTRA_RESULTS_SOURCE = "android.remoteinput.resultsSource";

    @IntDef({SOURCE_FREE_FORM_INPUT, SOURCE_CHOICE})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Source {}

    /** The user manually entered the data. */
    public static final int SOURCE_FREE_FORM_INPUT = 0;

    /** The user selected one of the choices from {@link #getChoices}. */
    public static final int SOURCE_CHOICE = 1;

    @IntDef(value = {EDIT_CHOICES_BEFORE_SENDING_AUTO, EDIT_CHOICES_BEFORE_SENDING_DISABLED,
            EDIT_CHOICES_BEFORE_SENDING_ENABLED})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Retention(RetentionPolicy.SOURCE)
    public @interface EditChoicesBeforeSending {}

    /** The platform will determine whether choices will be edited before being sent to the app. */
    public static final int EDIT_CHOICES_BEFORE_SENDING_AUTO = 0;

    /** Tapping on a choice should send the input immediately, without letting the user edit it. */
    public static final int EDIT_CHOICES_BEFORE_SENDING_DISABLED = 1;

    /** Tapping on a choice should let the user edit the input before it is sent to the app. */
    public static final int EDIT_CHOICES_BEFORE_SENDING_ENABLED = 2;

    private final String mResultKey;
    private final CharSequence mLabel;
    private final CharSequence[] mChoices;
    private final boolean mAllowFreeFormTextInput;
    @EditChoicesBeforeSending private final int mEditChoicesBeforeSending;
    private final Bundle mExtras;
    private final Set<String> mAllowedDataTypes;

    RemoteInput(String resultKey, CharSequence label, CharSequence[] choices,
            boolean allowFreeFormTextInput, @EditChoicesBeforeSending int editChoicesBeforeSending,
            Bundle extras, Set<String> allowedDataTypes) {
        this.mResultKey = resultKey;
        this.mLabel = label;
        this.mChoices = choices;
        this.mAllowFreeFormTextInput = allowFreeFormTextInput;
        this.mEditChoicesBeforeSending = editChoicesBeforeSending;
        this.mExtras = extras;
        this.mAllowedDataTypes = allowedDataTypes;
        if (getEditChoicesBeforeSending() == EDIT_CHOICES_BEFORE_SENDING_ENABLED
                && !getAllowFreeFormInput()) {
            throw new IllegalArgumentException(
                "setEditChoicesBeforeSending requires setAllowFreeFormInput");
        }
    }

    /**
     * Get the key that the result of this input will be set in from the Bundle returned by
     * {@link #getResultsFromIntent} when the {@link android.app.PendingIntent} is sent.
     */
    public @NonNull String getResultKey() {
        return mResultKey;
    }

    /**
     * Get the label to display to users when collecting this input.
     */
    public @Nullable CharSequence getLabel() {
        return mLabel;
    }

    /**
     * Get possible input choices. This can be {@code null} if there are no choices to present.
     */
    @SuppressWarnings("NullableCollection") // Look, it's not the best API.
    public CharSequence @Nullable [] getChoices() {
        return mChoices;
    }

    @SuppressWarnings("NullableCollection") // That's just how it was defined.
    public @Nullable Set<String> getAllowedDataTypes() {
        return mAllowedDataTypes;
    }

    /**
     * Returns true if the input only accepts data, meaning {@link #getAllowFreeFormInput}
     * is false, {@link #getChoices} is null or empty, and {@link #getAllowedDataTypes} is
     * non-null and not empty.
     */
    public boolean isDataOnly() {
        return !getAllowFreeFormInput()
                && (getChoices() == null || getChoices().length == 0)
                && getAllowedDataTypes() != null
                && !getAllowedDataTypes().isEmpty();
    }

    /**
     * Get whether or not users can provide an arbitrary value for
     * input. If you set this to {@code false}, users must select one of the
     * choices in {@link #getChoices}. An {@link IllegalArgumentException} is thrown
     * if you set this to false and {@link #getChoices} returns {@code null} or empty.
     */
    public boolean getAllowFreeFormInput() {
        return mAllowFreeFormTextInput;
    }

    /**
     * Gets whether tapping on a choice should let the user edit the input before it is sent to the
     * app.
     */
    @EditChoicesBeforeSending public int getEditChoicesBeforeSending() {
        return mEditChoicesBeforeSending;
    }

    /**
     * Get additional metadata carried around with this remote input.
     */
    public @NonNull Bundle getExtras() {
        return mExtras;
    }

    /**
     * Builder class for {@link androidx.core.app.RemoteInput} objects.
     */
    public static final class Builder {
        private final String mResultKey;
        private final Set<String> mAllowedDataTypes = new HashSet<>();
        private final Bundle mExtras = new Bundle();
        private CharSequence mLabel;
        private CharSequence[] mChoices;
        private boolean mAllowFreeFormTextInput = true;
        @EditChoicesBeforeSending
        private int mEditChoicesBeforeSending = EDIT_CHOICES_BEFORE_SENDING_AUTO;

        /**
         * Create a builder object for {@link androidx.core.app.RemoteInput} objects.
         *
         * @param resultKey the Bundle key that refers to this input when collected from the user
         */
        public Builder(@NonNull String resultKey) {
            if (resultKey == null) {
                throw new IllegalArgumentException("Result key can't be null");
            }
            mResultKey = resultKey;
        }

        /**
         * Set a label to be displayed to the user when collecting this input.
         *
         * @param label The label to show to users when they input a response
         * @return this object for method chaining
         */
        public @NonNull Builder setLabel(@Nullable CharSequence label) {
            mLabel = label;
            return this;
        }

        /**
         * Specifies choices available to the user to satisfy this input.
         *
         * <p>Note: Starting in Android P, these choices will always be shown on phones if the app's
         * target SDK is >= P. However, these choices may also be rendered on other types of devices
         * regardless of target SDK.
         *
         * @param choices an array of pre-defined choices for users input.
         *        You must provide a non-null and non-empty array if
         *        you disabled free form input using {@link #setAllowFreeFormInput}
         * @return this object for method chaining
         */
        public @NonNull Builder setChoices(CharSequence @Nullable [] choices) {
            mChoices = choices;
            return this;
        }

        /**
         * Specifies whether the user can provide arbitrary values.
         *
         * @param mimeType A mime type that results are allowed to come in.
         *         Be aware that text results (see {@link #setAllowFreeFormInput}
         *         are allowed by default. If you do not want text results you will have to
         *         pass false to {@code setAllowFreeFormInput}
         * @param doAllow Whether the mime type should be allowed or not
         * @return this object for method chaining
         */
        public @NonNull Builder setAllowDataType(@NonNull String mimeType, boolean doAllow) {
            if (doAllow) {
                mAllowedDataTypes.add(mimeType);
            } else {
                mAllowedDataTypes.remove(mimeType);
            }
            return this;
        }

        /**
         * Specifies whether the user can provide arbitrary text values.
         *
         * @param allowFreeFormTextInput The default is {@code true}.
         *         If you specify {@code false}, you must either provide a non-null
         *         and non-empty array to {@link #setChoices}, or enable a data result
         *         in {@code setAllowDataType}. Otherwise an
         *         {@link IllegalArgumentException} is thrown
         * @return this object for method chaining
         */
        public @NonNull Builder setAllowFreeFormInput(boolean allowFreeFormTextInput) {
            mAllowFreeFormTextInput = allowFreeFormTextInput;
            return this;
        }

        /**
         * Specifies whether tapping on a choice should let the user edit the input before it is
         * sent to the app. The default is {@link #EDIT_CHOICES_BEFORE_SENDING_AUTO}.
         *
         * It cannot be used if {@link #setAllowFreeFormInput} has been set to false.
         */
        public @NonNull Builder setEditChoicesBeforeSending(
                @EditChoicesBeforeSending int editChoicesBeforeSending) {
            mEditChoicesBeforeSending = editChoicesBeforeSending;
            return this;
        }

        /**
         * Merge additional metadata into this builder.
         *
         * <p>Values within the Bundle will replace existing extras values in this Builder.
         *
         * @see RemoteInput#getExtras
         */
        public @NonNull Builder addExtras(@NonNull Bundle extras) {
            if (extras != null) {
                mExtras.putAll(extras);
            }
            return this;
        }

        /**
         * Get the metadata Bundle used by this Builder.
         *
         * <p>The returned Bundle is shared with this Builder.
         */
        public @NonNull Bundle getExtras() {
            return mExtras;
        }

        /**
         * Combine all of the options that have been set and return a new {@link
         * androidx.core.app.RemoteInput} object.
         */
        public @NonNull RemoteInput build() {
            return new RemoteInput(
                    mResultKey,
                    mLabel,
                    mChoices,
                    mAllowFreeFormTextInput,
                    mEditChoicesBeforeSending,
                    mExtras,
                    mAllowedDataTypes);
        }
    }

    /**
     * Similar as {@link #getResultsFromIntent} but retrieves data results for a
     * specific RemoteInput result. To retrieve a value use:
     * <pre>
     * {@code
     * Map<String, Uri> results =
     *     RemoteInput.getDataResultsFromIntent(intent, REMOTE_INPUT_KEY);
     * if (results != null) {
     *   Uri data = results.get(MIME_TYPE_OF_INTEREST);
     * }
     * }
     * </pre>
     * @param intent The intent object that fired in response to an action or content intent
     *               which also had one or more remote input requested.
     * @param remoteInputResultKey The result key for the RemoteInput you want results for.
     */
    @SuppressWarnings("NullableCollection") // This is what the platform API does.
    public static @Nullable Map<String, Uri> getDataResultsFromIntent(
            @NonNull Intent intent, @NonNull String remoteInputResultKey) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getDataResultsFromIntent(intent, remoteInputResultKey);
        } else {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                return null;
            }
            Map<String, Uri> results = new HashMap<>();
            Bundle extras = clipDataIntent.getExtras();
            for (String key : extras.keySet()) {
                if (key.startsWith(EXTRA_DATA_TYPE_RESULTS_DATA)) {
                    String mimeType = key.substring(EXTRA_DATA_TYPE_RESULTS_DATA.length());
                    if (mimeType.isEmpty()) {
                        continue;
                    }
                    Bundle bundle = clipDataIntent.getBundleExtra(key);
                    String uriStr = bundle.getString(remoteInputResultKey);
                    if (uriStr == null || uriStr.isEmpty()) {
                        continue;
                    }
                    results.put(mimeType, Uri.parse(uriStr));
                }
            }
            return results.isEmpty() ? null : results;
        }
    }

    /**
     * Get the remote input text results bundle from an intent. The returned Bundle will
     * contain a key/value for every result key populated by remote input collector.
     * Use the {@link Bundle#getCharSequence(String)} method to retrieve a value. For data results
     * use {@link #getDataResultsFromIntent}.
     * @param intent The intent object that fired in response to an action or content intent
     *               which also had one or more remote input requested.
     */
    // This is on purpose.
    @SuppressWarnings({"NullableCollection", "deprecation"})
    public static @Nullable Bundle getResultsFromIntent(@NonNull Intent intent) {
        return android.app.RemoteInput.getResultsFromIntent(intent);
    }

    /**
     * Populate an intent object with the results gathered from remote input. This method
     * should only be called by remote input collection services when sending results to a
     * pending intent.
     * @param remoteInputs The remote inputs for which results are being provided
     * @param intent The intent to add remote inputs to. The {@link android.content.ClipData}
     *               field of the intent will be modified to contain the results.
     * @param results A bundle holding the remote input results. This bundle should
     *                be populated with keys matching the result keys specified in
     *                {@code remoteInputs} with values being the result per key.
     */
    @SuppressWarnings("deprecation")
    public static void addResultsToIntent(RemoteInput @NonNull [] remoteInputs,
            @NonNull Intent intent, @NonNull Bundle results) {
        if (Build.VERSION.SDK_INT >= 26) {
            android.app.RemoteInput.addResultsToIntent(fromCompat(remoteInputs), intent, results);
        } else {
            // Implementations of RemoteInput#addResultsToIntent prior to SDK 26 don't actually add
            // results, they wipe out old results and insert the new one. Work around that by
            // preserving old results.
            Bundle existingTextResults =
                    RemoteInput.getResultsFromIntent(intent);

            // We also need to preserve the results source, as it is also cleared.
            int resultsSource = getResultsSource(intent);

            if (existingTextResults == null) {
                existingTextResults = results;
            } else {
                existingTextResults.putAll(results);
            }
            for (RemoteInput input : remoteInputs) {
                // Data results are also wiped out. So grab them and add them back in.
                Map<String, Uri> existingDataResults =
                        RemoteInput.getDataResultsFromIntent(
                                intent, input.getResultKey());
                RemoteInput[] arr = new RemoteInput[1];
                arr[0] = input;
                android.app.RemoteInput.addResultsToIntent(
                        fromCompat(arr), intent, existingTextResults);
                if (existingDataResults != null) {
                    RemoteInput.addDataResultToIntent(input, intent, existingDataResults);
                }
            }

            // Now restore the results source.
            setResultsSource(intent, resultsSource);
        }
    }

    /**
     * Same as {@link #addResultsToIntent} but for setting data results.
     * @param remoteInput The remote input for which results are being provided
     * @param intent The intent to add remote input results to. The
     *               {@link ClipData} field of the intent will be
     *               modified to contain the results.
     * @param results A map of mime type to the Uri result for that mime type.
     */
    public static void addDataResultToIntent(@NonNull RemoteInput remoteInput,
            @NonNull Intent intent, @NonNull Map<String, Uri> results) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.addDataResultToIntent(remoteInput, intent, results);
        } else {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                clipDataIntent = new Intent();  // First time we've added a result.
            }
            for (Map.Entry<String, Uri> entry : results.entrySet()) {
                String mimeType = entry.getKey();
                Uri uri = entry.getValue();
                if (mimeType == null) {
                    continue;
                }
                Bundle resultsBundle =
                        clipDataIntent.getBundleExtra(getExtraResultsKeyForData(mimeType));
                if (resultsBundle == null) {
                    resultsBundle = new Bundle();
                }
                resultsBundle.putString(remoteInput.getResultKey(), uri.toString());
                clipDataIntent.putExtra(getExtraResultsKeyForData(mimeType), resultsBundle);
            }
            intent.setClipData(ClipData.newIntent(RemoteInput.RESULTS_CLIP_LABEL, clipDataIntent));
        }
    }

    /**
     * Set the source of the RemoteInput results. This method should only be called by remote
     * input collection services (e.g.
     * {@link android.service.notification.NotificationListenerService})
     * when sending results to a pending intent.
     *
     * @see #SOURCE_FREE_FORM_INPUT
     * @see #SOURCE_CHOICE
     *
     * @param intent The intent to add remote input source to. The {@link ClipData}
     *               field of the intent will be modified to contain the source.
     * @param source The source of the results.
     */
    public static void setResultsSource(@NonNull Intent intent, @Source int source) {
        if (Build.VERSION.SDK_INT >= 28) {
            Api28Impl.setResultsSource(intent, source);
        } else {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                clipDataIntent = new Intent();  // First time we've added a result.
            }
            clipDataIntent.putExtra(EXTRA_RESULTS_SOURCE, source);
            intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
        }
    }

    /**
     * Get the source of the RemoteInput results.
     *
     * @see #SOURCE_FREE_FORM_INPUT
     * @see #SOURCE_CHOICE
     *
     * @param intent The intent object that fired in response to an action or content intent
     *               which also had one or more remote input requested.
     * @return The source of the results. If no source was set, {@link #SOURCE_FREE_FORM_INPUT} will
     * be returned.
     */
    @Source
    public static int getResultsSource(@NonNull Intent intent) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getResultsSource(intent);
        } else {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                return SOURCE_FREE_FORM_INPUT;
            }
            return clipDataIntent.getExtras().getInt(EXTRA_RESULTS_SOURCE, SOURCE_FREE_FORM_INPUT);
        }
    }

    private static String getExtraResultsKeyForData(String mimeType) {
        return EXTRA_DATA_TYPE_RESULTS_DATA + mimeType;
    }

    static android.app.RemoteInput[] fromCompat(RemoteInput[] srcArray) {
        if (srcArray == null) {
            return null;
        }
        android.app.RemoteInput[] result = new android.app.RemoteInput[srcArray.length];
        for (int i = 0; i < srcArray.length; i++) {
            result[i] = fromCompat(srcArray[i]);
        }
        return result;
    }

    static android.app.RemoteInput fromCompat(RemoteInput src) {
        return Api20Impl.fromCompat(src);
    }

    static RemoteInput fromPlatform(android.app.RemoteInput src) {
        return Api20Impl.fromPlatform(src);
    }

    private static Intent getClipDataIntentFromIntent(Intent intent) {
        ClipData clipData = intent.getClipData();
        if (clipData == null) {
            return null;
        }
        ClipDescription clipDescription = clipData.getDescription();
        if (!clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT)) {
            return null;
        }
        if (!clipDescription.getLabel().toString().contentEquals(RemoteInput.RESULTS_CLIP_LABEL)) {
            return null;
        }
        return clipData.getItemAt(0).getIntent();
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        static Map<String, Uri> getDataResultsFromIntent(Intent intent,
                String remoteInputResultKey) {
            return android.app.RemoteInput.getDataResultsFromIntent(intent, remoteInputResultKey);
        }

        static Set<String> getAllowedDataTypes(Object remoteInput) {
            return ((android.app.RemoteInput) remoteInput).getAllowedDataTypes();
        }

        static void addDataResultToIntent(RemoteInput remoteInput, Intent intent,
                Map<String, Uri> results) {
            android.app.RemoteInput.addDataResultToIntent(fromCompat(remoteInput), intent, results);
        }

        static android.app.RemoteInput.Builder setAllowDataType(
                android.app.RemoteInput.Builder builder, String mimeType, boolean doAllow) {
            return builder.setAllowDataType(mimeType, doAllow);
        }
    }

    static class Api20Impl {
        private Api20Impl() {
            // This class is not instantiable.
        }

        static RemoteInput fromPlatform(Object srcObj) {
            android.app.RemoteInput src = (android.app.RemoteInput) srcObj;
            Builder builder =
                    new Builder(src.getResultKey())
                            .setLabel(src.getLabel())
                            .setChoices(src.getChoices())
                            .setAllowFreeFormInput(src.getAllowFreeFormInput())
                            .addExtras(src.getExtras());
            if (Build.VERSION.SDK_INT >= 26) {
                Set<String> allowedDataTypes = Api26Impl.getAllowedDataTypes(src);
                if (allowedDataTypes != null) {
                    for (String allowedDataType : allowedDataTypes) {
                        builder.setAllowDataType(allowedDataType, true);
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 29) {
                builder.setEditChoicesBeforeSending(Api29Impl.getEditChoicesBeforeSending(src));
            }
            return builder.build();
        }

        public static android.app.RemoteInput fromCompat(RemoteInput src) {
            android.app.RemoteInput.Builder builder =
                    new android.app.RemoteInput.Builder(src.getResultKey())
                            .setLabel(src.getLabel())
                            .setChoices(src.getChoices())
                            .setAllowFreeFormInput(src.getAllowFreeFormInput())
                            .addExtras(src.getExtras());
            if (Build.VERSION.SDK_INT >= 26) {
                Set<String> allowedDataTypes = src.getAllowedDataTypes();
                if (allowedDataTypes != null) {
                    for (String allowedDataType : allowedDataTypes) {
                        Api26Impl.setAllowDataType(builder, allowedDataType, true);
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 29) {
                Api29Impl.setEditChoicesBeforeSending(builder, src.getEditChoicesBeforeSending());
            }
            return builder.build();
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static int getEditChoicesBeforeSending(Object remoteInput) {
            return ((android.app.RemoteInput) remoteInput).getEditChoicesBeforeSending();
        }

        static android.app.RemoteInput.Builder setEditChoicesBeforeSending(
                android.app.RemoteInput.Builder builder, int editChoicesBeforeSending) {
            return builder.setEditChoicesBeforeSending(editChoicesBeforeSending);
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static void setResultsSource(Intent intent, int source) {
            android.app.RemoteInput.setResultsSource(intent, source);
        }

        static int getResultsSource(Intent intent) {
            return android.app.RemoteInput.getResultsSource(intent);
        }
    }
}
