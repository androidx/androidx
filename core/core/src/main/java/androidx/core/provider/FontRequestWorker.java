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
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.provider.FontsContractCompat.FontFamilyResult;
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FontRequestFailReason;
import androidx.core.util.Consumer;
import androidx.tracing.Trace;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Given a {@link FontRequest}, loads the Typeface. Handles the sync/async nature of the calls,
 * and also makes use of {@link RequestExecutor#createDefaultExecutor} to load Typeface
 * asynchronously.
 */
class FontRequestWorker {

    private FontRequestWorker() {}

    static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);

    private static final ExecutorService DEFAULT_EXECUTOR_SERVICE = RequestExecutor
            .createDefaultExecutor(
                    "fonts-androidx",
                    Process.THREAD_PRIORITY_BACKGROUND,
                    10000 /* keepAliveTime */
            );

    /** Package protected to prevent synthetic accessor */
    static final Object LOCK = new Object();

    /** Package protected to prevent synthetic accessor */
    @GuardedBy("LOCK")
    static final SimpleArrayMap<String, ArrayList<Consumer<TypefaceResult>>> PENDING_REPLIES =
            new SimpleArrayMap<>();

    static void resetTypefaceCache() {
        sTypefaceCache.evictAll();
    }

    /**
     * Requests a Font to be loaded synchronously.
     * - If timeoutInMillis is infinite -> calls in the same thread as callee.
     * - If timeoutInMillis is NOT infinite -> calls in a bg thread with the timeout, and waits
     * on the bg task.
     *
     * Before returning the result, callback is called with the request result.
     *
     * @param context
     * @param request FontRequest that defines the font to be loaded.
     * @param callback the callback to be called.
     * @param style Typeface Style such as {@link Typeface#NORMAL}, {@link Typeface#BOLD}
     *              {@link Typeface#ITALIC}, {@link Typeface#BOLD_ITALIC}.
     * @param timeoutInMillis timeout in milliseconds for the request.
     * @return
     */
    static Typeface requestFontSync(
            @NonNull final Context context,
            @NonNull final FontRequest request,
            @NonNull final CallbackWrapper callback,
            final int style,
            int timeoutInMillis
    ) {
        final String id = createCacheId(request, style);
        Typeface cached = sTypefaceCache.get(id);
        if (cached != null) {
            callback.onTypefaceResult(new TypefaceResult(cached));
            return cached;
        }

        // when timeout is infinite, do not post to bg thread, since it will block other requests
        if (timeoutInMillis == FontResourcesParserCompat.INFINITE_TIMEOUT_VALUE) {
            // Wait forever. No need to post to the thread.
            TypefaceResult typefaceResult = getFontSync(id, context, request, style);
            callback.onTypefaceResult(typefaceResult);
            return typefaceResult.mTypeface;
        }

        final Callable<TypefaceResult> fetcher = new Callable<TypefaceResult>() {
            @Override
            public TypefaceResult call() {
                return getFontSync(id, context, request, style);
            }
        };

        try {
            TypefaceResult typefaceResult = RequestExecutor.submit(
                    DEFAULT_EXECUTOR_SERVICE,
                    fetcher,
                    timeoutInMillis
            );
            callback.onTypefaceResult(typefaceResult);
            return typefaceResult.mTypeface;
        } catch (InterruptedException e) {
            callback.onTypefaceResult(new TypefaceResult(FAIL_REASON_FONT_LOAD_ERROR));
            return null;
        }
    }

    /**
     * Request a Font to be loaded async.
     *
     * The {@link FontRequest} is executed on executor, and the callback is called on the
     * {@link Handler} that is contained in {@link CallbackWrapper}.
     *
     *
     * @param context
     * @param request FontRequest for the font to be loaded.
     * @param style Typeface Style such as {@link Typeface#NORMAL}, {@link Typeface#BOLD} ads asd
     *             {@link Typeface#ITALIC}, {@link Typeface#BOLD_ITALIC}.
     * @param executor Executor instance to execute the request. If null is provided
     *                 DEFAULT_EXECUTOR_SERVICE will be used.
     * @param callback callback to be called for async FontRequest result.
     *                 {@link CallbackWrapper} contains the Handler to call the
     *                 callback on.
     * @return
     */
    static Typeface requestFontAsync(
            @NonNull final Context context,
            @NonNull final FontRequest request,
            final int style,
            @Nullable final Executor executor,
            @NonNull final CallbackWrapper callback
    ) {
        final String id = createCacheId(request, style);
        Typeface cached = sTypefaceCache.get(id);
        if (cached != null) {
            callback.onTypefaceResult(new TypefaceResult(cached));
            return cached;
        }

        final Consumer<TypefaceResult> reply = new Consumer<TypefaceResult>() {
            @Override
            public void accept(TypefaceResult typefaceResult) {
                if (typefaceResult == null) {
                    typefaceResult = new TypefaceResult(FAIL_REASON_FONT_LOAD_ERROR);
                }
                callback.onTypefaceResult(typefaceResult);
            }
        };

        synchronized (LOCK) {
            ArrayList<Consumer<TypefaceResult>> pendingReplies = PENDING_REPLIES.get(id);
            if (pendingReplies != null) {
                // Already requested. Do not request the same provider again and insert the
                // reply to the queue instead.
                pendingReplies.add(reply);
                return null;
            }
            pendingReplies = new ArrayList<>();
            pendingReplies.add(reply);
            PENDING_REPLIES.put(id, pendingReplies);
        }

        final Callable<TypefaceResult> fetcher = new Callable<TypefaceResult>() {
            @Override
            public TypefaceResult call() {
                try {
                    return getFontSync(id, context, request, style);
                } catch (Throwable t) {
                    return new TypefaceResult(FAIL_REASON_FONT_LOAD_ERROR);
                }
            }
        };
        Executor finalExecutor = executor == null ? DEFAULT_EXECUTOR_SERVICE : executor;

        RequestExecutor.execute(finalExecutor, fetcher, new Consumer<TypefaceResult>() {
            @Override
            public void accept(TypefaceResult typefaceResult) {
                final ArrayList<Consumer<TypefaceResult>> replies;
                synchronized (LOCK) {
                    replies = PENDING_REPLIES.get(id);
                    if (replies == null) {
                        return;  // Nobody requested replies. Do nothing.
                    }
                    PENDING_REPLIES.remove(id);
                }
                for (int i = 0; i < replies.size(); ++i) {
                    replies.get(i).accept(typefaceResult);
                }
            }
        });

        return null;
    }

    private static String createCacheId(@NonNull FontRequest request, int style) {
        return request.getId() + "-" + style;
    }

    /** Package protected to prevent synthetic accessor */
    @NonNull
    static TypefaceResult getFontSync(
            @NonNull final String cacheId,
            @NonNull final Context context,
            @NonNull final FontRequest request,
            int style
    ) {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("getFontSync");
        }
        try {
            Typeface cached = sTypefaceCache.get(cacheId);
            if (cached != null) {
                return new TypefaceResult(cached);
            }

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
        } finally {
            if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                Trace.endSection();
            }
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
