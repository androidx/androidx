/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted.sharing;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes a Web Share Target associated with a Trusted Web Activity.
 *
 * The structure of a ShareTarget object follows the specification [1] of the "share_target" object
 * within web manifest json, with the following exceptions:
 * - The "action" field specifies the full URL of the Share Target, and not only the path.
 * - There is no "url" field in the "params" object, since urls are not supplied separately from
 * text in Android's ACTION_SEND and ACTION_SEND_MULTIPLE intents.
 *
 * [1] https://wicg.github.io/web-share-target/level-2/
 */
public final class ShareTarget  {

    /** Bundle key for {@link #action}. */
    @SuppressLint("IntentName")
    public static final String KEY_ACTION = "androidx.browser.trusted.sharing.KEY_ACTION";

    /** Bundle key for {@link #method}. */
    public static final String KEY_METHOD = "androidx.browser.trusted.sharing.KEY_METHOD";

    /** Bundle key for {@link #encodingType}. */
    public static final String KEY_ENCTYPE = "androidx.browser.trusted.sharing.KEY_ENCTYPE";

    /** Bundle key for {@link #params}. */
    public static final String KEY_PARAMS = "androidx.browser.trusted.sharing.KEY_PARAMS";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef({METHOD_GET, METHOD_POST})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestMethod {}

    /** See {@link #method}. */
    public static final String METHOD_GET = "GET";

    /** See {@link #method}. */
    public static final String METHOD_POST = "POST";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef({ENCODING_TYPE_URL_ENCODED, ENCODING_TYPE_MULTIPART})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EncodingType {}

    /**
     * An encoding type to be used with POST requests (see {@link #encodingType}) corresponding to
     * {@code application/x-www-form-urlencoded} of the HTTP POST standard [1].
     *
     * [1]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST
     */
    public static final String ENCODING_TYPE_URL_ENCODED = "application/x-www-form-urlencoded";

    /**
     * An encoding type to be used with POST requests (see {@link #encodingType}) corresponding to
     * {@code multipart/form-data} of the HTTP POST standard [1].
     */
    public static final String ENCODING_TYPE_MULTIPART = "multipart/form-data";

    /**
     * URL of the Web Share Target. Must belong to an origin associated with the Trusted Web
     * Activity. For example, assuming the origin is "https://mypwa.com", the action could be
     * "https://mypwa.com/share.html".
     */
    @NonNull
    public final String action;

    /**
     * HTTP request method for the Web Share Target. Must be {@link #METHOD_GET} or
     * {@link #METHOD_POST}. Default is {@link #METHOD_GET}.
     */
    @Nullable
    @RequestMethod
    public final String method;

    /**
     * Specifies how the shared data should be encoded in the body of a POST request. Must be
     * {@link #ENCODING_TYPE_MULTIPART} or {@link #ENCODING_TYPE_URL_ENCODED}. Default is
     * {@link #ENCODING_TYPE_URL_ENCODED}.
     */
    @Nullable
    @EncodingType
    public final String encodingType;

    /**
     * Contains the parameter names, see {@link Params}.
     */
    @NonNull
    public final Params params;

    /**
     * Creates a {@link ShareTarget} with the given parameters.
     * @param action The {@link #action}.
     * @param method The {@link #method}.
     * @param encodingType The {@link #encodingType}.
     * @param params The {@link #params}.
     */
    public ShareTarget(@NonNull String action, @Nullable @RequestMethod String method,
            @Nullable @EncodingType String encodingType, @NonNull Params params) {
        this.action = action;
        this.method = method;
        this.encodingType = encodingType;
        this.params = params;
    }

    /** Packs the object into a {@link Bundle}. */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_METHOD, method);
        bundle.putString(KEY_ENCTYPE, encodingType);
        bundle.putBundle(KEY_PARAMS, params.toBundle());
        return bundle;
    }

    /** Unpacks the object from a {@link Bundle}. */
    @Nullable
    public static ShareTarget fromBundle(@NonNull Bundle bundle) {
        String action = bundle.getString(KEY_ACTION);
        String method = bundle.getString(KEY_METHOD);
        String encType = bundle.getString(KEY_ENCTYPE);
        Params params = Params.fromBundle(bundle.getBundle(KEY_PARAMS));
        if (action == null || params == null) {
            return null;
        }
        return new ShareTarget(action, method, encType, params);
    }

    /** Contains parameter names to be used for the data being shared. */
    public static class Params {
        /** Bundle key for {@link #title}. */
        public static final String KEY_TITLE = "androidx.browser.trusted.sharing.KEY_TITLE";

        /** Bundle key for {@link #text}. */
        public static final String KEY_TEXT = "androidx.browser.trusted.sharing.KEY_TEXT";

        /** Bundle key for {@link #files}. */
        public static final String KEY_FILES = "androidx.browser.trusted.sharing.KEY_FILES";

        /** The name of the query parameter used for the title of the message being shared. */
        @Nullable
        public final String title;

        /** The name of the query parameter used for the body of the message being shared. */
        @Nullable
        public final String text;

        /**
         * Defines form fields for the files being shared, see {@link FileFormField}.
         * Web Share Target can have multiple form fields associated with different MIME types.
         * If a file passes the MIME type filters of several {@link FileFormField}s,
         * the one that has the lowest index in this list is picked; see [1] for details.
         *
         * [1] https://wicg.github.io/web-share-target/level-2/#launching-the-web-share-target
         */
        @Nullable
        public final List<FileFormField> files;

        /**
         * Creates a {@link Params} with the given parameters.
         * @param title The {@link #title}.
         * @param text The {@link #text}.
         * @param files The {@link #files}.
         */
        public Params(@Nullable String title, @Nullable String text,
                @Nullable List<FileFormField> files) {
            this.title = title;
            this.text = text;
            this.files = files;
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @NonNull
        Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_TITLE, title);
            bundle.putString(KEY_TEXT, text);
            if (files != null) {
                ArrayList<Bundle> fileBundles = new ArrayList<>();
                for (FileFormField file : files) {
                    fileBundles.add(file.toBundle());
                }
                bundle.putParcelableArrayList(KEY_FILES, fileBundles);
            }

            return bundle;
        }

        /* synthetic access */
        @SuppressWarnings({"WeakerAccess", "deprecation"})
        @Nullable
        static Params fromBundle(@Nullable Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            List<FileFormField> files = null;
            List<Bundle> fileBundles = bundle.getParcelableArrayList(KEY_FILES);
            if (fileBundles != null) {
                files = new ArrayList<>();
                for (Bundle fileBundle : fileBundles) {
                    files.add(FileFormField.fromBundle(fileBundle));
                }
            }
            return new Params(bundle.getString(KEY_TITLE), bundle.getString(KEY_TEXT),
                    files);
        }
    }

    /** Defines a form field for sharing files. */
    public static final class FileFormField {
        /** Bundle key for {@link #name}. */
        public static final String KEY_NAME = "androidx.browser.trusted.sharing.KEY_FILE_NAME";

        /** Bundle key for {@link #acceptedTypes}. */
        public static final String KEY_ACCEPTED_TYPES =
                "androidx.browser.trusted.sharing.KEY_ACCEPTED_TYPES";

        /** Name of the form field. */
        @NonNull
        public final String name;

        /**
         * List of MIME types or file extensions to be sent in this field. The MIME type matching
         * algorithm is specified by
         * https://wicg.github.io/web-share-target/level-2/#determining-if-a-file-is-accepted.
         */
        @NonNull
        public final List<String> acceptedTypes;

        /**
         * Creates a {@link FileFormField} with the given parameters.
         * @param name The {@link #name}.
         * @param acceptedTypes The {@link #acceptedTypes}.
         */
        public FileFormField(@NonNull String name, @NonNull List<String> acceptedTypes) {
            this.name = name;
            this.acceptedTypes = Collections.unmodifiableList(acceptedTypes);
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @NonNull
        Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_NAME, name);
            bundle.putStringArrayList(KEY_ACCEPTED_TYPES, new ArrayList<>(acceptedTypes));
            return bundle;
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @Nullable
        static FileFormField fromBundle(@Nullable Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            String name = bundle.getString(KEY_NAME);
            ArrayList<String> acceptedTypes = bundle.getStringArrayList(KEY_ACCEPTED_TYPES);
            if (name == null || acceptedTypes == null) {
                return null;
            }
            return new FileFormField(name, acceptedTypes);
        }
    }
}
