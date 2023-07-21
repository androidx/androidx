/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.provider;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.graphics.TypefaceCompatUtil;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Utility class to deal with Font ContentProviders.
 */
public class FontsContractCompat {
    private FontsContractCompat() { }

    /**
     * Build a Typeface from an array of {@link FontInfo}
     *
     * Results that are marked as not ready will be skipped.
     *
     * @param context A {@link Context} that will be used to fetch the font contents.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none. If
     *                           the operation is canceled, then {@link
     *                           android.os.OperationCanceledException} will be thrown.
     * @param fonts An array of {@link FontInfo} to be used to create a Typeface.
     * @return A Typeface object. Returns null if typeface creation fails.
     */
    @Nullable
    public static Typeface buildTypeface(
            @NonNull Context context,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull FontInfo[] fonts
    ) {
        return TypefaceCompat.createFromFontInfo(context, cancellationSignal, fonts,
                Typeface.NORMAL);
    }

    /**
     * Fetch fonts given a font request.
     *
     * @param context A {@link Context} to be used for fetching fonts.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none. If
     *                           the operation is canceled, then {@link
     *                           android.os.OperationCanceledException} will be thrown when the
     *                           query is executed.
     * @param request A {@link FontRequest} object that identifies the provider and query for the
     *                request.
     *
     * @return {@link FontFamilyResult}
     *
     * @throws PackageManager.NameNotFoundException If requested package or authority was not found
     *      in the system.
     */
    @NonNull
    public static FontFamilyResult fetchFonts(
            @NonNull Context context,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull FontRequest request
    ) throws PackageManager.NameNotFoundException {
        return FontProvider.getFontFamilyResult(context, request, cancellationSignal);
    }

    /**
     * Create a typeface object given a font request. The font will be asynchronously fetched,
     * therefore the result is delivered to the given callback. See {@link FontRequest}.
     * Only one of the methods in callback will be invoked, depending on whether the request
     * succeeds or fails. These calls will happen on the caller thread.
     * @param context A context to be used for fetching from font provider.
     * @param request A {@link FontRequest} object that identifies the provider and query for the
     *                request. May not be null.
     * @param callback A callback that will be triggered when results are obtained. May not be null.
     * @param handler A handler to be processed the font fetching.
     */
    public static void requestFont(
            final @NonNull Context context,
            final @NonNull FontRequest request,
            final @NonNull FontRequestCallback callback,
            final @NonNull Handler handler
    ) {
        CallbackWithHandler callbackWrapper = new CallbackWithHandler(callback);
        Executor executor = RequestExecutor.createHandlerExecutor(handler);
        FontRequestWorker.requestFontAsync(context.getApplicationContext(), request,
                Typeface.NORMAL, executor, callbackWrapper);
    }

    /**
     * Loads a Typeface. Based on the parameters isBlockingFetch, and timeoutInMillis, the fetch
     * is either sync or async.
     * - If timeoutInMillis is infinite, and isBlockingFetch is true -> sync
     * - If timeoutInMillis is NOT infinite, and isBlockingFetch is true -> sync with timeout
     * - else -> async without timeout.
     *
     * Used by TypefaceCompat and tests.
     *
     * @param context Context
     * @param request FontRequest that defines the font to be loaded.
     * @param style Typeface Style such as {@link Typeface#NORMAL}, {@link Typeface#BOLD}
     *              {@link Typeface#ITALIC}, {@link Typeface#BOLD_ITALIC}.
     * @param isBlockingFetch when true the call will be synchronous.
     * @param timeout timeout in milliseconds for the request. It is not used for async
     *                request.
     * @param handler the handler to call the callback on.
     * @param callback the callback to be called.
     *
     * @return the resulting Typeface if the requested font is in the cache or the request is a
     * sync request.
     *
     */
    @RestrictTo(LIBRARY)
    @Nullable
    public static Typeface requestFont(
            @NonNull final Context context,
            @NonNull final FontRequest request,
            final int style,
            boolean isBlockingFetch,
            @IntRange(from = 0) int timeout,
            @NonNull final Handler handler,
            @NonNull final FontRequestCallback callback
    ) {
        CallbackWithHandler callbackWrapper = new CallbackWithHandler(callback, handler);

        if (isBlockingFetch) {
            return FontRequestWorker.requestFontSync(context, request, callbackWrapper, style,
                    timeout);
        } else {
            return FontRequestWorker.requestFontAsync(context, request, style, null /*executor*/,
                    callbackWrapper);
        }
    }

    /** @hide */
    @VisibleForTesting
    public static void resetTypefaceCache() {
        FontRequestWorker.resetTypefaceCache();
    }

    /**
     * Defines the constants used in a response from a Font Provider. The cursor returned from the
     * query should have the ID column populated with the content uri ID for the resulting font.
     * This should point to a real file or shared memory, as the client will mmap the given file
     * descriptor. Pipes, sockets and other non-mmap-able file descriptors will fail to load in the
     * client application.
     */
    public static final class Columns implements BaseColumns {
        /**
         * Constant used to request data from a font provider. The cursor returned from the query
         * may populate this column with a long for the font file ID. The client will request a file
         * descriptor to "file/FILE_ID" with this ID immediately under the top-level content URI. If
         * not present, the client will request a file descriptor to the top-level URI with the
         * given base font ID. Note that several results may return the same file ID, e.g. for TTC
         * files with different indices.
         */
        public static final String FILE_ID = "file_id";
        /**
         * Constant used to request data from a font provider. The cursor returned from the query
         * should have this column populated with an int for the ttc index for the resulting font.
         */
        public static final String TTC_INDEX = android.provider.FontsContract.Columns.TTC_INDEX;
        /**
         * Constant used to request data from a font provider. The cursor returned from the query
         * may populate this column with the font variation settings String information for the
         * font.
         */
        public static final String VARIATION_SETTINGS =
                android.provider.FontsContract.Columns.VARIATION_SETTINGS;
        /**
         * Constant used to request data from a font provider. The cursor returned from the query
         * should have this column populated with the int weight for the resulting font. This value
         * should be between 100 and 900. The most common values are 400 for regular weight and 700
         * for bold weight.
         */
        public static final String WEIGHT = android.provider.FontsContract.Columns.WEIGHT;
        /**
         * Constant used to request data from a font provider. The cursor returned from the query
         * should have this column populated with the int italic for the resulting font. This should
         * be 0 for regular style and 1 for italic.
         */
        public static final String ITALIC = android.provider.FontsContract.Columns.ITALIC;
        /**
         * Constant used to request data from a font provider. The cursor returned from the query
         * should have this column populated to indicate the result status of the
         * query. This will be checked before any other data in the cursor. Possible values are
         * {@link #RESULT_CODE_OK}, {@link #RESULT_CODE_FONT_NOT_FOUND},
         * {@link #RESULT_CODE_MALFORMED_QUERY} and {@link #RESULT_CODE_FONT_UNAVAILABLE}. If not
         * present, {@link #RESULT_CODE_OK} will be assumed.
         */
        public static final String RESULT_CODE = android.provider.FontsContract.Columns.RESULT_CODE;

        /**
         * Constant used to represent a result was retrieved successfully. The given fonts will be
         * attempted to retrieve immediately via
         * {@link android.content.ContentProvider#openFile(Uri, String)}. See {@link #RESULT_CODE}.
         */
        public static final int RESULT_CODE_OK =
                android.provider.FontsContract.Columns.RESULT_CODE_OK;
        /**
         * Constant used to represent a result was not found. See {@link #RESULT_CODE}.
         */
        public static final int RESULT_CODE_FONT_NOT_FOUND =
                android.provider.FontsContract.Columns.RESULT_CODE_FONT_NOT_FOUND;
        /**
         * Constant used to represent a result was found, but cannot be provided at this moment. Use
         * this to indicate, for example, that a font needs to be fetched from the network. See
         * {@link #RESULT_CODE}.
         */
        public static final int RESULT_CODE_FONT_UNAVAILABLE =
                android.provider.FontsContract.Columns.RESULT_CODE_FONT_UNAVAILABLE;
        /**
         * Constant used to represent that the query was not in a supported format by the provider.
         * See {@link #RESULT_CODE}.
         */
        public static final int RESULT_CODE_MALFORMED_QUERY =
                android.provider.FontsContract.Columns.RESULT_CODE_MALFORMED_QUERY;
    }

    /**
     * Object represent a font entry in the family returned from {@link #fetchFonts}.
     */
    public static class FontInfo {
        private final Uri mUri;
        private final int mTtcIndex;
        private final int mWeight;
        private final boolean mItalic;
        private final int mResultCode;

        /**
         * Creates a Font with all the information needed about a provided font.
         * @param uri A URI associated to the font file.
         * @param ttcIndex If providing a TTC_INDEX file, the index to point to. Otherwise, 0.
         * @param weight An integer that indicates the font weight.
         * @param italic A boolean that indicates the font is italic style or not.
         * @param resultCode A boolean that indicates the font contents is ready.
         *
         * @deprecated Not being used by any cross library, and should not be used, internal
         * implementation detail.
         *
         */
        // TODO after removing from public API make package private.
        @Deprecated
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public FontInfo(
                @NonNull Uri uri,
                @IntRange(from = 0) int ttcIndex,
                @IntRange(from = 1, to = 1000) int weight,
                boolean italic,
                int resultCode
        ) {
            mUri = Preconditions.checkNotNull(uri);
            mTtcIndex = ttcIndex;
            mWeight = weight;
            mItalic = italic;
            mResultCode = resultCode;
        }

        @SuppressWarnings("deprecation")
        static FontInfo create(
                @NonNull Uri uri,
                @IntRange(from = 0) int ttcIndex,
                @IntRange(from = 1, to = 1000) int weight,
                boolean italic,
                int resultCode
        ) {
            return new FontInfo(uri, ttcIndex, weight, italic, resultCode);
        }

        /**
         * Returns a URI associated to this record.
         */
        public @NonNull Uri getUri() {
            return mUri;
        }

        /**
         * Returns the index to be used to access this font when accessing a TTC file.
         */
        public @IntRange(from = 0) int getTtcIndex() {
            return mTtcIndex;
        }

        /**
         * Returns the weight value for this font.
         */
        public @IntRange(from = 1, to = 1000) int getWeight() {
            return mWeight;
        }

        /**
         * Returns whether this font is italic.
         */
        public boolean isItalic() {
            return mItalic;
        }

        /**
         * Returns result code.
         *
         * {@link FontsContractCompat.Columns#RESULT_CODE}
         */
        public int getResultCode() {
            return mResultCode;
        }
    }

    /**
     * Object returned from {@link #fetchFonts}.
     */
    public static class FontFamilyResult {
        /**
         * Constant represents that the font was successfully retrieved. Note that when this value
         * is set and {@link #getFonts} returns an empty array, it means there were no fonts
         * matching the given query.
         */
        public static final int STATUS_OK = 0;

        /**
         * Constant represents that the given certificate was not matched with the provider's
         * signature. {@link #getFonts} returns null if this status was set.
         */
        public static final int STATUS_WRONG_CERTIFICATES = 1;

        /**
         * Constant represents that the provider returns unexpected data. {@link #getFonts} returns
         * null if this status was set. For example, this value is set when the font provider
         * gives invalid format of variation settings.
         */
        public static final int STATUS_UNEXPECTED_DATA_PROVIDED = 2;

        @RestrictTo(LIBRARY)
        @IntDef({STATUS_OK, STATUS_WRONG_CERTIFICATES, STATUS_UNEXPECTED_DATA_PROVIDED})
        @Retention(RetentionPolicy.SOURCE)
        @interface FontResultStatus {}

        private final @FontResultStatus int mStatusCode;
        private final FontInfo[] mFonts;

        /**
         * @deprecated Not being used by any cross library, and should not be used, internal
         * implementation detail.
         **/
        // TODO after removing from public API make package private.
        @Deprecated
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public FontFamilyResult(@FontResultStatus int statusCode, @Nullable FontInfo[] fonts) {
            mStatusCode = statusCode;
            mFonts = fonts;
        }

        public @FontResultStatus int getStatusCode() {
            return mStatusCode;
        }

        public FontInfo[] getFonts() {
            return mFonts;
        }

        @SuppressWarnings("deprecation")
        static FontFamilyResult create(
                @FontResultStatus int statusCode,
                @Nullable FontInfo[] fonts) {
            return new FontFamilyResult(statusCode, fonts);
        }
    }

    /**
     * Interface used to receive asynchronously fetched typefaces.
     */
    public static class FontRequestCallback {
        /**
         * @deprecated Not being used by any cross library, and should not be used, internal
         * implementation detail.
         */
        @Deprecated
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public static final int RESULT_OK = Columns.RESULT_CODE_OK;

        static final int RESULT_SUCCESS = Columns.RESULT_CODE_OK;

        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the given
         * provider was not found on the device.
         */
        public static final int FAIL_REASON_PROVIDER_NOT_FOUND = -1;

        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the given
         * provider must be authenticated and the given certificates do not match its signature.
         */
        public static final int FAIL_REASON_WRONG_CERTIFICATES = -2;

        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the font
         * returned by the provider was not loaded properly.
         */
        public static final int FAIL_REASON_FONT_LOAD_ERROR = -3;

        /**
         * Constant that signals that the font was not loaded due to security issues. This usually
         * means the font was attempted to load on a restricted context.
         */
        public static final int FAIL_REASON_SECURITY_VIOLATION = -4;

        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the font
         * provider did not return any results for the given query.
         */
        public static final int FAIL_REASON_FONT_NOT_FOUND = Columns.RESULT_CODE_FONT_NOT_FOUND;

        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the font
         * provider found the queried font, but it is currently unavailable.
         */
        public static final int FAIL_REASON_FONT_UNAVAILABLE = Columns.RESULT_CODE_FONT_UNAVAILABLE;

        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the given
         * query was not supported by the provider.
         */
        public static final int FAIL_REASON_MALFORMED_QUERY = Columns.RESULT_CODE_MALFORMED_QUERY;

        @SuppressWarnings("deprecation")
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @IntDef({ FAIL_REASON_PROVIDER_NOT_FOUND, FAIL_REASON_FONT_LOAD_ERROR,
                FAIL_REASON_FONT_NOT_FOUND, FAIL_REASON_FONT_UNAVAILABLE,
                FAIL_REASON_MALFORMED_QUERY, FAIL_REASON_WRONG_CERTIFICATES,
                FAIL_REASON_SECURITY_VIOLATION, RESULT_OK })
        @Retention(RetentionPolicy.SOURCE)
        public @interface FontRequestFailReason {}

        public FontRequestCallback() {}

        /**
         * Called then a Typeface request done via {@link #requestFont(Context, FontRequest,
         * FontRequestCallback, Handler)} is complete. Note that this method will not be called if
         * {@link #onTypefaceRequestFailed(int)} is called instead.
         * @param typeface  The Typeface object retrieved.
         */
        public void onTypefaceRetrieved(Typeface typeface) {}

        /**
         * Called when a Typeface request done via {@link #requestFont(Context, FontRequest,
         * FontRequestCallback, Handler)} fails.
         * @param reason May be one of {@link #FAIL_REASON_PROVIDER_NOT_FOUND},
         *               {@link #FAIL_REASON_FONT_NOT_FOUND},
         *               {@link #FAIL_REASON_FONT_LOAD_ERROR},
         *               {@link #FAIL_REASON_FONT_UNAVAILABLE},
         *               {@link #FAIL_REASON_MALFORMED_QUERY} or
         *               {@link #FAIL_REASON_WRONG_CERTIFICATES}, or a provider defined positive
         *               code number.
         */
        public void onTypefaceRequestFailed(@FontRequestFailReason int reason) {}
    }

    /**
     * Constant used to identify the List of {@link ParcelFileDescriptor} item in the Bundle
     * returned to the ResultReceiver in getFont.
     *
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     *
     */
    @Deprecated // unused
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final String PARCEL_FONT_RESULTS = "font_results";

    // Error codes internal to the system, which can not come from a provider. To keep the number
    // space open for new provider codes, these should all be negative numbers.
    /**
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     **/
    @Deprecated // unused
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    static final int RESULT_CODE_PROVIDER_NOT_FOUND = -1;

    /**
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     **/
    @Deprecated // unused
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    static final int RESULT_CODE_WRONG_CERTIFICATES = -2;
    // Note -3 is used by FontRequestCallback to indicate the font failed to load.

    /**
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     **/
    @Deprecated // unused
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static Typeface getFontSync(
            final Context context,
            final FontRequest request,
            final @Nullable ResourcesCompat.FontCallback fontCallback,
            final @Nullable Handler handler,
            boolean isBlockingFetch,
            int timeout,
            final int style
    ) {
        FontRequestCallback newCallback = new TypefaceCompat.ResourcesCallbackAdapter(fontCallback);
        Handler newHandler = ResourcesCompat.FontCallback.getHandler(handler);
        return requestFont(context, request, style, isBlockingFetch, timeout, newHandler,
                newCallback
        );
    }

    /**
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     **/
    @Deprecated // unused
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void resetCache() {
        FontRequestWorker.resetTypefaceCache();
    }

    /**
     * A helper function to create a mapping from {@link Uri} to {@link ByteBuffer}.
     *
     * Skip if the file contents is not ready to be read.
     *
     * @param context A {@link Context} to be used for resolving content URI in
     *                {@link FontInfo}.
     * @param fonts An array of {@link FontInfo}.
     * @return A map from {@link Uri} to {@link ByteBuffer}.
     *
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     *
     */
    @Deprecated // unused
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @RequiresApi(19)
    public static Map<Uri, ByteBuffer> prepareFontData(
            Context context,
            FontInfo[] fonts,
            CancellationSignal cancellationSignal
    ) {
        return TypefaceCompatUtil.readFontInfoIntoByteBuffer(context, fonts, cancellationSignal);
    }

    /**
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     **/
    @Deprecated // unused
    @VisibleForTesting
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Nullable
    public static ProviderInfo getProvider(
            @NonNull PackageManager packageManager,
            @NonNull FontRequest request,
            @Nullable Resources resources
    ) throws PackageManager.NameNotFoundException {
        return FontProvider.getProvider(packageManager, request, resources);
    }
}
