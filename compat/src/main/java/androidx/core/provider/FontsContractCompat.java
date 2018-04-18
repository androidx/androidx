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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.BaseColumns;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LruCache;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.graphics.TypefaceCompatUtil;
import androidx.core.provider.SelfDestructiveThread.ReplyCallback;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Utility class to deal with Font ContentProviders.
 */
public class FontsContractCompat {
    private static final String TAG = "FontsContractCompat";

    private FontsContractCompat() { }

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
     * Constant used to identify the List of {@link ParcelFileDescriptor} item in the Bundle
     * returned to the ResultReceiver in getFont.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String PARCEL_FONT_RESULTS = "font_results";

    // Error codes internal to the system, which can not come from a provider. To keep the number
    // space open for new provider codes, these should all be negative numbers.
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    /* package */ static final int RESULT_CODE_PROVIDER_NOT_FOUND = -1;
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    /* package */ static final int RESULT_CODE_WRONG_CERTIFICATES = -2;
    // Note -3 is used by FontRequestCallback to indicate the font failed to load.

    static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);

    private static final int BACKGROUND_THREAD_KEEP_ALIVE_DURATION_MS = 10000;
    private static final SelfDestructiveThread sBackgroundThread =
            new SelfDestructiveThread("fonts", Process.THREAD_PRIORITY_BACKGROUND,
                    BACKGROUND_THREAD_KEEP_ALIVE_DURATION_MS);

    @NonNull
    static TypefaceResult getFontInternal(final Context context, final FontRequest request,
            int style) {
        FontFamilyResult result;
        try {
            result = fetchFonts(context, null /* CancellationSignal */, request);
        } catch (PackageManager.NameNotFoundException e) {
            return new TypefaceResult(null, FontRequestCallback.FAIL_REASON_PROVIDER_NOT_FOUND);
        }
        if (result.getStatusCode() == FontFamilyResult.STATUS_OK) {
            final Typeface typeface = TypefaceCompat.createFromFontInfo(
                    context, null /* CancellationSignal */, result.getFonts(), style);
            return new TypefaceResult(typeface, typeface != null
                    ? FontRequestCallback.RESULT_OK
                    : FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR);
        }
        int resultCode = result.getStatusCode() == FontFamilyResult.STATUS_WRONG_CERTIFICATES
                ? FontRequestCallback.FAIL_REASON_WRONG_CERTIFICATES
                : FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR;
        return new TypefaceResult(null, resultCode);
    }

    static final Object sLock = new Object();
    @GuardedBy("sLock")
    static final SimpleArrayMap<String, ArrayList<ReplyCallback<TypefaceResult>>>
            sPendingReplies = new SimpleArrayMap<>();

    private static final class TypefaceResult {
        final Typeface mTypeface;
        @FontRequestCallback.FontRequestFailReason final int mResult;

        TypefaceResult(@Nullable Typeface typeface,
                @FontRequestCallback.FontRequestFailReason int result) {
            mTypeface = typeface;
            mResult = result;
        }
    }

    /**
     * Used for tests, should not be used otherwise.
     * @hide
     **/
    @RestrictTo(LIBRARY_GROUP)
    public static void resetCache() {
        sTypefaceCache.evictAll();
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public static Typeface getFontSync(final Context context, final FontRequest request,
            final @Nullable ResourcesCompat.FontCallback fontCallback,
            final @Nullable Handler handler, boolean isBlockingFetch, int timeout,
            final int style) {
        final String id = request.getIdentifier() + "-" + style;
        Typeface cached = sTypefaceCache.get(id);
        if (cached != null) {
            if (fontCallback != null) {
                fontCallback.onFontRetrieved(cached);
            }
            return cached;
        }

        if (isBlockingFetch && timeout == FontResourcesParserCompat.INFINITE_TIMEOUT_VALUE) {
            // Wait forever. No need to post to the thread.
            TypefaceResult typefaceResult = getFontInternal(context, request, style);
            if (fontCallback != null) {
                if (typefaceResult.mResult == FontFamilyResult.STATUS_OK) {
                    fontCallback.callbackSuccessAsync(typefaceResult.mTypeface, handler);
                } else {
                    fontCallback.callbackFailAsync(typefaceResult.mResult, handler);
                }
            }
            return typefaceResult.mTypeface;
        }

        final Callable<TypefaceResult> fetcher = new Callable<TypefaceResult>() {
            @Override
            public TypefaceResult call() throws Exception {
                TypefaceResult typeface = getFontInternal(context, request, style);
                if (typeface.mTypeface != null) {
                    sTypefaceCache.put(id, typeface.mTypeface);
                }
                return typeface;
            }
        };

        if (isBlockingFetch) {
            try {
                return sBackgroundThread.postAndWait(fetcher, timeout).mTypeface;
            } catch (InterruptedException e) {
                return null;
            }
        } else {
            final ReplyCallback<TypefaceResult> reply = fontCallback == null ? null
                    : new ReplyCallback<TypefaceResult>() {
                        @Override
                        public void onReply(final TypefaceResult typeface) {
                            if (typeface == null) {
                                fontCallback.callbackFailAsync(
                                        FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND, handler);
                            } else if (typeface.mResult == FontFamilyResult.STATUS_OK) {
                                fontCallback.callbackSuccessAsync(typeface.mTypeface, handler);
                            } else {
                                fontCallback.callbackFailAsync(typeface.mResult, handler);
                            }
                        }
                    };

            synchronized (sLock) {
                if (sPendingReplies.containsKey(id)) {
                    // Already requested. Do not request the same provider again and insert the
                    // reply to the queue instead.
                    if (reply != null) {
                        sPendingReplies.get(id).add(reply);
                    }
                    return null;
                }
                if (reply != null) {
                    ArrayList<ReplyCallback<TypefaceResult>> pendingReplies = new ArrayList<>();
                    pendingReplies.add(reply);
                    sPendingReplies.put(id, pendingReplies);
                }
            }
            sBackgroundThread.postAndReply(fetcher, new ReplyCallback<TypefaceResult>() {
                @Override
                public void onReply(final TypefaceResult typeface) {
                    final ArrayList<ReplyCallback<TypefaceResult>> replies;
                    synchronized (sLock) {
                        replies = sPendingReplies.get(id);
                        if (replies == null) {
                            return;  // Nobody requested replies. Do nothing.
                        }
                        sPendingReplies.remove(id);
                    }
                    for (int i = 0; i < replies.size(); ++i) {
                        replies.get(i).onReply(typeface);
                    }
                }
            });
            return null;
        }
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
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public FontInfo(@NonNull Uri uri, @IntRange(from = 0) int ttcIndex,
                @IntRange(from = 1, to = 1000) int weight,
                boolean italic, int resultCode) {
            mUri = Preconditions.checkNotNull(uri);
            mTtcIndex = ttcIndex;
            mWeight = weight;
            mItalic = italic;
            mResultCode = resultCode;
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

        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
        @IntDef({STATUS_OK, STATUS_WRONG_CERTIFICATES, STATUS_UNEXPECTED_DATA_PROVIDED})
        @Retention(RetentionPolicy.SOURCE)
        @interface FontResultStatus {}

        private final @FontResultStatus int mStatusCode;
        private final FontInfo[] mFonts;

        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
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
    }

    /**
     * Interface used to receive asynchronously fetched typefaces.
     */
    public static class FontRequestCallback {
        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
        public static final int RESULT_OK = Columns.RESULT_CODE_OK;
        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the given
         * provider was not found on the device.
         */
        public static final int FAIL_REASON_PROVIDER_NOT_FOUND = RESULT_CODE_PROVIDER_NOT_FOUND;
        /**
         * Constant returned by {@link #onTypefaceRequestFailed(int)} signaling that the given
         * provider must be authenticated and the given certificates do not match its signature.
         */
        public static final int FAIL_REASON_WRONG_CERTIFICATES = RESULT_CODE_WRONG_CERTIFICATES;
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

        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
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
    public static void requestFont(final @NonNull Context context,
            final @NonNull FontRequest request, final @NonNull FontRequestCallback callback,
            final @NonNull Handler handler) {
        final Handler callerThreadHandler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: Cache the result.
                FontFamilyResult result;
                try {
                    result = fetchFonts(context, null /* cancellation signal */, request);
                } catch (PackageManager.NameNotFoundException e) {
                    callerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTypefaceRequestFailed(
                                    FontRequestCallback.FAIL_REASON_PROVIDER_NOT_FOUND);
                        }
                    });
                    return;
                }

                if (result.getStatusCode() != FontFamilyResult.STATUS_OK) {
                    switch (result.getStatusCode()) {
                        case FontFamilyResult.STATUS_WRONG_CERTIFICATES:
                            callerThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onTypefaceRequestFailed(
                                            FontRequestCallback.FAIL_REASON_WRONG_CERTIFICATES);
                                }
                            });
                            return;
                        case FontFamilyResult.STATUS_UNEXPECTED_DATA_PROVIDED:
                            callerThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onTypefaceRequestFailed(
                                            FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR);
                                }
                            });
                            return;
                        default:
                            // fetchFont returns unexpected status type. Fallback to load error.
                            callerThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onTypefaceRequestFailed(
                                            FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR);
                                }
                            });
                            return;
                    }
                }

                final FontInfo[] fonts = result.getFonts();
                if (fonts == null || fonts.length == 0) {
                    callerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTypefaceRequestFailed(
                                    FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
                        }
                    });
                    return;
                }
                for (final FontInfo font : fonts) {
                    if (font.getResultCode() != Columns.RESULT_CODE_OK) {
                        // We proceed if all font entry is ready to use. Otherwise report the first
                        // error.
                        final int resultCode = font.getResultCode();
                        if (resultCode < 0) {
                            // Negative values are reserved for internal errors. Fallback to load
                            // error.
                            callerThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onTypefaceRequestFailed(
                                            FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR);
                                }
                            });
                        } else {
                            callerThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onTypefaceRequestFailed(resultCode);
                                }
                            });
                        }
                        return;
                    }
                }

                final Typeface typeface = buildTypeface(context, null /* cancellation signal */,
                        fonts);
                if (typeface == null) {
                    // Something went wrong during reading font files. This happens if the given
                    // font file is an unsupported font type.
                    callerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTypefaceRequestFailed(
                                    FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR);
                        }
                    });
                    return;
                }

                callerThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onTypefaceRetrieved(typeface);
                    }
                });
            }
        });
    }

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
    public static Typeface buildTypeface(@NonNull Context context,
            @Nullable CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts) {
        return TypefaceCompat.createFromFontInfo(context, cancellationSignal, fonts,
                Typeface.NORMAL);
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
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @RequiresApi(19)
    public static Map<Uri, ByteBuffer> prepareFontData(Context context, FontInfo[] fonts,
            CancellationSignal cancellationSignal) {
        final HashMap<Uri, ByteBuffer> out = new HashMap<>();

        for (FontInfo font : fonts) {
            if (font.getResultCode() != Columns.RESULT_CODE_OK) {
                continue;
            }

            final Uri uri = font.getUri();
            if (out.containsKey(uri)) {
                continue;
            }

            ByteBuffer buffer = TypefaceCompatUtil.mmap(context, cancellationSignal, uri);
            out.put(uri, buffer);
        }
        return Collections.unmodifiableMap(out);
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
    public static FontFamilyResult fetchFonts(@NonNull Context context,
            @Nullable CancellationSignal cancellationSignal, @NonNull FontRequest request)
            throws PackageManager.NameNotFoundException {
        ProviderInfo providerInfo = getProvider(
                context.getPackageManager(), request, context.getResources());
        if (providerInfo == null) {
            return new FontFamilyResult(FontFamilyResult.STATUS_WRONG_CERTIFICATES, null);

        }
        FontInfo[] fonts = getFontFromProvider(
                context, request, providerInfo.authority, cancellationSignal);
        return new FontFamilyResult(FontFamilyResult.STATUS_OK, fonts);
    }

    /** @hide */
    @VisibleForTesting
    @RestrictTo(LIBRARY_GROUP)
    public static @Nullable ProviderInfo getProvider(@NonNull PackageManager packageManager,
            @NonNull FontRequest request, @Nullable Resources resources)
            throws PackageManager.NameNotFoundException {
        String providerAuthority = request.getProviderAuthority();
        ProviderInfo info = packageManager.resolveContentProvider(providerAuthority, 0);
        if (info == null) {
            throw new PackageManager.NameNotFoundException("No package found for authority: "
                    + providerAuthority);
        }

        if (!info.packageName.equals(request.getProviderPackage())) {
            throw new PackageManager.NameNotFoundException("Found content provider "
                    + providerAuthority
                    + ", but package was not " + request.getProviderPackage());
        }

        List<byte[]> signatures;
        // We correctly check all signatures returned, as advised in the lint error.
        @SuppressLint("PackageManagerGetSignatures")
        PackageInfo packageInfo = packageManager.getPackageInfo(info.packageName,
                PackageManager.GET_SIGNATURES);
        signatures = convertToByteArrayList(packageInfo.signatures);
        Collections.sort(signatures, sByteArrayComparator);
        List<List<byte[]>> requestCertificatesList = getCertificates(request, resources);
        for (int i = 0; i < requestCertificatesList.size(); ++i) {
            // Make a copy so we can sort it without modifying the incoming data.
            List<byte[]> requestSignatures = new ArrayList<>(requestCertificatesList.get(i));
            Collections.sort(requestSignatures, sByteArrayComparator);
            if (equalsByteArrayList(signatures, requestSignatures)) {
                return info;
            }
        }
        return null;
    }

    private static List<List<byte[]>> getCertificates(FontRequest request, Resources resources) {
        if (request.getCertificates() != null) {
            return request.getCertificates();
        }
        int resourceId = request.getCertificatesArrayResId();
        return FontResourcesParserCompat.readCerts(resources, resourceId);
    }

    private static final Comparator<byte[]> sByteArrayComparator = new Comparator<byte[]>() {
        @Override
        public int compare(byte[] l, byte[] r) {
            if (l.length != r.length) {
                return l.length - r.length;
            }
            for (int i = 0; i < l.length; ++i) {
                if (l[i] != r[i]) {
                    return l[i] - r[i];
                }
            }
            return 0;
        }
    };

    private static boolean equalsByteArrayList(List<byte[]> signatures,
            List<byte[]> requestSignatures) {
        if (signatures.size() != requestSignatures.size()) {
            return false;
        }
        for (int i = 0; i < signatures.size(); ++i) {
            if (!Arrays.equals(signatures.get(i), requestSignatures.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<byte[]> convertToByteArrayList(Signature[] signatures) {
        List<byte[]> shas = new ArrayList<>();
        for (int i = 0; i < signatures.length; ++i) {
            shas.add(signatures[i].toByteArray());
        }
        return shas;
    }

    @VisibleForTesting
    @NonNull
    static FontInfo[] getFontFromProvider(Context context, FontRequest request, String authority,
            CancellationSignal cancellationSignal) {
        ArrayList<FontInfo> result = new ArrayList<>();
        final Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .build();
        final Uri fileBaseUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath("file")
                .build();
        Cursor cursor = null;
        try {
            if (Build.VERSION.SDK_INT > 16) {
                cursor = context.getContentResolver().query(uri, new String[] {
                        Columns._ID, Columns.FILE_ID, Columns.TTC_INDEX,
                        Columns.VARIATION_SETTINGS, Columns.WEIGHT, Columns.ITALIC,
                        Columns.RESULT_CODE },
                "query = ?", new String[] { request.getQuery() }, null, cancellationSignal);
            } else {
                // No cancellation signal.
                cursor = context.getContentResolver().query(uri, new String[] {
                        Columns._ID, Columns.FILE_ID, Columns.TTC_INDEX,
                        Columns.VARIATION_SETTINGS, Columns.WEIGHT, Columns.ITALIC,
                        Columns.RESULT_CODE },
                "query = ?", new String[] { request.getQuery() }, null);
            }
            if (cursor != null && cursor.getCount() > 0) {
                final int resultCodeColumnIndex = cursor.getColumnIndex(Columns.RESULT_CODE);
                result = new ArrayList<>();
                final int idColumnIndex = cursor.getColumnIndex(Columns._ID);
                final int fileIdColumnIndex = cursor.getColumnIndex(Columns.FILE_ID);
                final int ttcIndexColumnIndex = cursor.getColumnIndex(Columns.TTC_INDEX);
                final int weightColumnIndex = cursor.getColumnIndex(Columns.WEIGHT);
                final int italicColumnIndex = cursor.getColumnIndex(Columns.ITALIC);
                while (cursor.moveToNext()) {
                    int resultCode = resultCodeColumnIndex != -1
                            ? cursor.getInt(resultCodeColumnIndex) : Columns.RESULT_CODE_OK;
                    final int ttcIndex = ttcIndexColumnIndex != -1
                            ? cursor.getInt(ttcIndexColumnIndex) : 0;
                    Uri fileUri;
                    if (fileIdColumnIndex == -1) {
                        long id = cursor.getLong(idColumnIndex);
                        fileUri = ContentUris.withAppendedId(uri, id);
                    } else {
                        long id = cursor.getLong(fileIdColumnIndex);
                        fileUri = ContentUris.withAppendedId(fileBaseUri, id);
                    }

                    int weight = weightColumnIndex != -1 ? cursor.getInt(weightColumnIndex) : 400;
                    boolean italic = italicColumnIndex != -1 && cursor.getInt(italicColumnIndex)
                            == 1;
                    result.add(new FontInfo(fileUri, ttcIndex, weight, italic, resultCode));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result.toArray(new FontInfo[0]);
    }
}
