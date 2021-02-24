/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.core.provider.FontsContractCompat.FontFamilyResult.STATUS_WRONG_CERTIFICATES;
import static androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR;
import static androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND;
import static androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_PROVIDER_NOT_FOUND;
import static androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_WRONG_CERTIFICATES;
import static androidx.core.provider.FontsContractCompat.FontRequestCallback.RESULT_SUCCESS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Process;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.provider.FontRequestThreadPool.ReplyCallback;
import androidx.core.provider.FontsContractCompat.FontFamilyResult;
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FontRequestFailReason;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Given a {@link FontRequest}, loads the Typeface. Handles the sync/async nature of the calls,
 * and also makes use of {@link FontRequestThreadPool} to load Typeface asynchronously.
 */
class FontRequestWorker {

    private FontRequestWorker() {}

    static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);

    private static final FontRequestThreadPool BACKGROUND_THREAD = new FontRequestThreadPool(
            "fonts-androidx",
            Process.THREAD_PRIORITY_BACKGROUND,
            10000 /* keepAliveTime */
    );

    /** Package protected to prevent synthetic accessor */
    static final Object LOCK = new Object();

    /** Package protected to prevent synthetic accessor */
    @GuardedBy("LOCK")
    static final SimpleArrayMap<String, ArrayList<ReplyCallback<TypefaceResult>>> PENDING_REPLIES =
            new SimpleArrayMap<>();

    static void resetTypefaceCache() {
        sTypefaceCache.evictAll();
    }

    /**
     * Loads a Typeface on the given executorHandler.
     *
     * @param appContext Context
     * @param request FontRequest to fetch the font file from {@link FontProvider}
     * @param callback callback to call when Typeface is loaded.
     * @param executorHandler the handler to fetch the font
     */
    static void requestFont(
            final @NonNull Context appContext,
            final @NonNull FontRequest request,
            final @NonNull FontRequestCallbackWithHandler callback,
            final @NonNull Handler executorHandler
    ) {
        final int defaultStyle = Typeface.NORMAL;

        final String id = createCacheId(request, defaultStyle);
        Typeface cached = sTypefaceCache.get(id);
        if (cached != null) {
            callback.onTypefaceResult(new TypefaceResult(cached));
        }

        executorHandler.post(new Runnable() {
            @Override
            public void run() {
                TypefaceResult typefaceResult = getFontInternal(id, appContext, request,
                        defaultStyle);
                callback.onTypefaceResult(typefaceResult);
            }
        });
    }

    /**
     * Loads a Typeface. Based on the parameters isBlockingFetch, and timeoutInMillis, the fetch
     * is either sync or async.
     * - If timeoutInMillis is infinite, and isBlockingFetch is true -> sync
     * - If timeoutInMillis is NOT infinite, and isBlockingFetch is true -> sync with timeout
     * - else -> async without timeout.
     *
     * @param context Context
     * @param request FontRequest that defines the font to be loaded.
     * @param callback the callback to be called.
     * @param isBlockingFetch when true the call will be synchronous.
     * @param timeoutInMillis timeout in milliseconds for the request. It is not used for async
     *                        request.
     * @param style Typeface Style such as {@link Typeface#NORMAL}, {@link Typeface#BOLD}
     *              {@link Typeface#ITALIC}, {@link Typeface#BOLD_ITALIC}.
     *
     * @return the resulting Typeface if it is not an async request.
     */
    static Typeface requestFont(
            @NonNull final Context context,
            @NonNull final FontRequest request,
            @NonNull final FontRequestCallbackWithHandler callback,
            boolean isBlockingFetch,
            int timeoutInMillis,
            final int style) {
        final String id = createCacheId(request, style);
        Typeface cached = sTypefaceCache.get(id);
        if (cached != null) {
            callback.onTypefaceResult(new TypefaceResult(cached));
            return cached;
        }

        // when timeout is infinite, do not post to bg thread, since it will block other requests
        if (isBlockingFetch
                && timeoutInMillis == FontResourcesParserCompat.INFINITE_TIMEOUT_VALUE) {
            // Wait forever. No need to post to the thread.
            TypefaceResult typefaceResult = getFontInternal(id, context, request, style);
            callback.onTypefaceResult(typefaceResult);
            return typefaceResult.mTypeface;
        }

        final Callable<TypefaceResult> fetcher = new Callable<TypefaceResult>() {
            @Override
            public TypefaceResult call() {
                TypefaceResult typeface = getFontInternal(id, context, request, style);
                return typeface;
            }
        };

        if (isBlockingFetch) {
            try {
                TypefaceResult typefaceResult = BACKGROUND_THREAD.postAndWait(fetcher,
                        timeoutInMillis);
                callback.onTypefaceResult(typefaceResult);
                return typefaceResult.mTypeface;
            } catch (InterruptedException e) {
                callback.onTypefaceResult(new TypefaceResult(FAIL_REASON_FONT_LOAD_ERROR));
                return null;
            }
        } else {
            final ReplyCallback<TypefaceResult> reply = new ReplyCallback<TypefaceResult>() {
                @Override
                public void onReply(final TypefaceResult typefaceResult) {
                    callback.onTypefaceResult(typefaceResult);
                }
            };

            synchronized (LOCK) {
                ArrayList<ReplyCallback<TypefaceResult>> pendingReplies = PENDING_REPLIES.get(id);
                if (pendingReplies != null) {
                    // Already requested. Do not request the same provider again and insert the
                    // reply to the queue instead.
                    if (reply != null) {
                        pendingReplies.add(reply);
                    }
                    return null;
                }
                if (reply != null) {
                    pendingReplies = new ArrayList<>();
                    pendingReplies.add(reply);
                    PENDING_REPLIES.put(id, pendingReplies);
                }
            }
            BACKGROUND_THREAD.postAndReply(fetcher, new ReplyCallback<TypefaceResult>() {
                @Override
                public void onReply(final TypefaceResult typeface) {
                    final ArrayList<ReplyCallback<TypefaceResult>> replies;
                    synchronized (LOCK) {
                        replies = PENDING_REPLIES.get(id);
                        if (replies == null) {
                            return;  // Nobody requested replies. Do nothing.
                        }
                        PENDING_REPLIES.remove(id);
                    }
                    for (int i = 0; i < replies.size(); ++i) {
                        replies.get(i).onReply(typeface);
                    }
                }
            });
            return null;
        }
    }

    private static String createCacheId(@NonNull FontRequest request, int style) {
        return request.getId() + "-" + style;
    }

    /** Package protected to prevent synthetic accessor */
    @NonNull
    static TypefaceResult getFontInternal(
            @NonNull final String cacheId,
            @NonNull final Context context,
            @NonNull final FontRequest request,
            int style
    ) {
        FontFamilyResult result;
        try {
            result = FontProvider.getFontFamilyResult(context, request, null);
        } catch (PackageManager.NameNotFoundException e) {
            return new TypefaceResult(FAIL_REASON_PROVIDER_NOT_FOUND);
        }

        int fontFamilyResultStatus = getFontFamilyResultStatus(result);
        if (fontFamilyResultStatus != RESULT_SUCCESS) {
            return new TypefaceResult(fontFamilyResultStatus);
        }

        final Typeface typeface = TypefaceCompat.createFromFontInfo(
                context, null /* CancellationSignal */, result.getFonts(), style);

        if (typeface != null) {
            sTypefaceCache.put(cacheId, typeface);
            return new TypefaceResult(typeface);
        } else {
            return new TypefaceResult(FAIL_REASON_FONT_LOAD_ERROR);
        }
    }

    @SuppressLint("WrongConstant")
    @FontRequestFailReason
    private static int getFontFamilyResultStatus(@NonNull FontFamilyResult fontFamilyResult) {
        if (fontFamilyResult.getStatusCode() != FontFamilyResult.STATUS_OK) {
            switch (fontFamilyResult.getStatusCode()) {
                case STATUS_WRONG_CERTIFICATES:
                    return FAIL_REASON_WRONG_CERTIFICATES;
                default:
                    return FAIL_REASON_FONT_LOAD_ERROR;
            }
        } else {
            final FontsContractCompat.FontInfo[] fonts = fontFamilyResult.getFonts();
            if (fonts == null || fonts.length == 0) {
                return FAIL_REASON_FONT_NOT_FOUND;
            }

            for (final FontsContractCompat.FontInfo font : fonts) {
                // We proceed if all font entry is ready to use. Otherwise report the first
                // error.
                final int resultCode = font.getResultCode();
                if (resultCode != FontsContractCompat.Columns.RESULT_CODE_OK) {
                    // Negative values are reserved for internal errors. Fallback to load
                    // error.
                    return resultCode < 0 ? FAIL_REASON_FONT_LOAD_ERROR : resultCode;
                }
            }

            return RESULT_SUCCESS;
        }
    }

    static final class TypefaceResult {
        final Typeface mTypeface;
        @FontRequestFailReason final int mResult;

        TypefaceResult(@FontRequestFailReason int result) {
            mTypeface = null;
            mResult = result;
        }

        @SuppressLint("WrongConstant")
        TypefaceResult(@NonNull Typeface typeface) {
            mTypeface = typeface;
            mResult = RESULT_SUCCESS;
        }

        @SuppressLint("WrongConstant")
        boolean isSuccess() {
            return mResult == RESULT_SUCCESS;
        }
    }

}
