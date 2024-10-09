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

package androidx.emoji2.text;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.graphics.TypefaceCompatUtil;
import androidx.core.os.TraceCompat;
import androidx.core.provider.FontRequest;
import androidx.core.provider.FontsContractCompat;
import androidx.core.provider.FontsContractCompat.FontFamilyResult;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * {@link EmojiCompat.Config} implementation that asynchronously fetches the required font and the
 * metadata using a {@link FontRequest}. FontRequest should be constructed to fetch an EmojiCompat
 * compatible emoji font.
 * <p/>
 */
public class FontRequestEmojiCompatConfig extends EmojiCompat.Config {

    /**
     * Retry policy used when the font provider is not ready to give the font file.
     *
     * To control the thread the retries are handled on, see
     * {@link FontRequestEmojiCompatConfig#setLoadingExecutor}.
     */
    public abstract static class RetryPolicy {
        /**
         * Called each time the metadata loading fails.
         *
         * This is primarily due to a pending download of the font.
         * If a value larger than zero is returned, metadata loader will retry after the given
         * milliseconds.
         * <br />
         * If {@code zero} is returned, metadata loader will retry immediately.
         * <br/>
         * If a value less than 0 is returned, the metadata loader will stop retrying and
         * EmojiCompat will get into {@link EmojiCompat#LOAD_STATE_FAILED} state.
         * <p/>
         * Note that the retry may happen earlier than you specified if the font provider notifies
         * that the download is completed.
         *
         * @return long milliseconds to wait until next retry
         */
        public abstract long getRetryDelay();
    }

    /**
     * A retry policy implementation that doubles the amount of time in between retries.
     *
     * If downloading hasn't finish within given amount of time, this policy give up and the
     * EmojiCompat will get into {@link EmojiCompat#LOAD_STATE_FAILED} state.
     */
    public static class ExponentialBackoffRetryPolicy extends RetryPolicy {
        private final long mTotalMs;
        private long mRetryOrigin;

        /**
         * @param totalMs A total amount of time to wait in milliseconds.
         */
        public ExponentialBackoffRetryPolicy(long totalMs) {
            mTotalMs = totalMs;
        }

        @Override
        public long getRetryDelay() {
            if (mRetryOrigin == 0) {
                mRetryOrigin = SystemClock.uptimeMillis();
                // Since download may be completed after getting query result and before registering
                // observer, requesting later at the same time.
                return 0;
            } else {
                // Retry periodically since we can't trust notify change event. Some font provider
                // may not notify us.
                final long elapsedMillis = SystemClock.uptimeMillis() - mRetryOrigin;
                if (elapsedMillis > mTotalMs) {
                    return -1;  // Give up since download hasn't finished in 10 min.
                }
                // Wait until the same amount of the time from the first scheduled time, but adjust
                // the minimum request interval is 1 sec and never exceeds 10 min in total.
                return Math.min(Math.max(elapsedMillis, 1000), mTotalMs - elapsedMillis);
            }
        }
    }

    /**
     * @param context Context instance, cannot be {@code null}
     * @param request {@link FontRequest} to fetch the font asynchronously, cannot be {@code null}
     */
    public FontRequestEmojiCompatConfig(@NonNull Context context, @NonNull FontRequest request) {
        super(new FontRequestMetadataLoader(context, request, DEFAULT_FONTS_CONTRACT));
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public FontRequestEmojiCompatConfig(@NonNull Context context, @NonNull FontRequest request,
            @NonNull FontProviderHelper fontProviderHelper) {
        super(new FontRequestMetadataLoader(context, request, fontProviderHelper));
    }

    /**
     * Sets the custom executor to be used for initialization.
     *
     * Since font loading is too slow for the main thread, the metadata loader will fetch the fonts
     * on a background thread. By default, FontRequestEmojiCompatConfig will create its own
     * single threaded Executor, which causes a thread to be created.
     *
     * You can pass your own executor to control which thread the font is loaded on, and avoid an
     * extra thread creation.
     *
     * @param executor background executor for performing font load
     */
    public @NonNull FontRequestEmojiCompatConfig setLoadingExecutor(@NonNull Executor executor) {
        ((FontRequestMetadataLoader) getMetadataRepoLoader()).setExecutor(executor);
        return this;
    }

    /**
     * Please us {@link #setLoadingExecutor(Executor)} instead to set background loading thread.
     *
     * This was deprecated in emoji2 1.0.0-alpha04.
     *
     * If migrating from androidx.emoji please prefer to use an existing background executor for
     * setLoadingExecutor.
     *
     * Note: This method will no longer have any effect if passed null, which is a breaking
     * change from androidx.emoji.
     *
     * @deprecated please call setLoadingExecutor instead
     *
     * @param handler background thread handler to wrap in an Executor, if null this method will
     *                do nothing
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public @NonNull FontRequestEmojiCompatConfig setHandler(@Nullable Handler handler) {
        if (handler == null) {
            // this is a breaking behavior change from androidx.emoji, we no longer support
            // clearing executors
            return this;
        }
        setLoadingExecutor(ConcurrencyHelpers.convertHandlerToExecutor(handler));
        return this;
    }

    /**
     * Sets the retry policy.
     *
     * {@see RetryPolicy}
     * @param policy The policy to be used when the font provider is not ready to give the font
     *              file. Can be {@code null}. In case of {@code null}, the metadata loader never
     *              retries.
     */
    public @NonNull FontRequestEmojiCompatConfig setRetryPolicy(@Nullable RetryPolicy policy) {
        ((FontRequestMetadataLoader) getMetadataRepoLoader()).setRetryPolicy(policy);
        return this;
    }

    /**
     * MetadataRepoLoader implementation that uses FontsContractCompat and TypefaceCompat to load a
     * given FontRequest.
     */
    private static class FontRequestMetadataLoader implements EmojiCompat.MetadataRepoLoader {
        private static final String S_TRACE_BUILD_TYPEFACE =
                "EmojiCompat.FontRequestEmojiCompatConfig.buildTypeface";
        private final @NonNull Context mContext;
        private final @NonNull FontRequest mRequest;
        private final @NonNull FontProviderHelper mFontProviderHelper;
        private final @NonNull Object mLock = new Object();

        @GuardedBy("mLock")
        private @Nullable Handler mMainHandler;
        @GuardedBy("mLock")
        private @Nullable Executor mExecutor;
        @GuardedBy("mLock")
        private @Nullable ThreadPoolExecutor mMyThreadPoolExecutor;
        @GuardedBy("mLock")
        private @Nullable RetryPolicy mRetryPolicy;

        @GuardedBy("mLock")
        EmojiCompat.@Nullable MetadataRepoLoaderCallback mCallback;
        @GuardedBy("mLock")
        private @Nullable ContentObserver mObserver;
        @GuardedBy("mLock")
        private @Nullable Runnable mMainHandlerLoadCallback;

        FontRequestMetadataLoader(@NonNull Context context, @NonNull FontRequest request,
                @NonNull FontProviderHelper fontProviderHelper) {
            Preconditions.checkNotNull(context, "Context cannot be null");
            Preconditions.checkNotNull(request, "FontRequest cannot be null");
            mContext = context.getApplicationContext();
            mRequest = request;
            mFontProviderHelper = fontProviderHelper;
        }

        public void setExecutor(@NonNull Executor executor) {
            synchronized (mLock) {
                mExecutor = executor;
            }
        }

        public void setRetryPolicy(@Nullable RetryPolicy policy) {
            synchronized (mLock) {
                mRetryPolicy = policy;
            }
        }

        @Override
        public void load(final EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback) {
            Preconditions.checkNotNull(loaderCallback, "LoaderCallback cannot be null");
            synchronized (mLock) {
                mCallback = loaderCallback;
            }
            loadInternal();
        }

        void loadInternal() {
            synchronized (mLock) {
                if (mCallback == null) {
                    // do nothing; loading is already complete
                    return;
                }
                if (mExecutor == null) {
                    mMyThreadPoolExecutor = ConcurrencyHelpers.createBackgroundPriorityExecutor(
                            "emojiCompat");
                    mExecutor = mMyThreadPoolExecutor;
                }
                mExecutor.execute(this::createMetadata);
            }
        }

        @WorkerThread
        private FontsContractCompat.FontInfo retrieveFontInfo() {
            final FontsContractCompat.FontFamilyResult result;
            try {
                result = mFontProviderHelper.fetchFonts(mContext, mRequest);
            } catch (NameNotFoundException e) {
                throw new RuntimeException("provider not found", e);
            }
            if (result.getStatusCode() != FontsContractCompat.FontFamilyResult.STATUS_OK) {
                throw new RuntimeException("fetchFonts failed (" + result.getStatusCode() + ")");
            }
            final FontsContractCompat.FontInfo[] fonts = result.getFonts();
            if (fonts == null || fonts.length == 0) {
                throw new RuntimeException("fetchFonts failed (empty result)");
            }
            return fonts[0];  // Assuming the GMS Core provides only one font file.
        }

        @WorkerThread
        private void scheduleRetry(Uri uri, long waitMs) {
            synchronized (mLock) {
                Handler handler = mMainHandler;
                if (handler == null) {
                    handler = ConcurrencyHelpers.mainHandlerAsync();
                    mMainHandler = handler;
                }
                if (mObserver == null) {
                    mObserver = new ContentObserver(handler) {
                        @Override
                        public void onChange(boolean selfChange, Uri uri) {
                            loadInternal();
                        }
                    };
                    mFontProviderHelper.registerObserver(mContext, uri, mObserver);
                }
                if (mMainHandlerLoadCallback == null) {
                    mMainHandlerLoadCallback = this::loadInternal;
                }
                handler.postDelayed(mMainHandlerLoadCallback, waitMs);
            }
        }

        // Must be called on the mHandler.
        private void cleanUp() {
            synchronized (mLock) {
                mCallback = null;
                if (mObserver != null) {
                    mFontProviderHelper.unregisterObserver(mContext, mObserver);
                    mObserver = null;
                }
                if (mMainHandler != null) {
                    mMainHandler.removeCallbacks(mMainHandlerLoadCallback);
                }
                mMainHandler = null;
                if (mMyThreadPoolExecutor != null) {
                    // if we made the executor, shut it down
                    mMyThreadPoolExecutor.shutdown();
                }
                mExecutor = null;
                mMyThreadPoolExecutor = null;
            }
        }

        // Must be called on the mHandler.
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @WorkerThread
        void createMetadata() {
            synchronized (mLock) {
                if (mCallback == null) {
                    return;  // Already handled or cancelled. Do nothing.
                }
            }
            try {
                final FontsContractCompat.FontInfo font = retrieveFontInfo();

                final int resultCode = font.getResultCode();
                if (resultCode == FontsContractCompat.Columns.RESULT_CODE_FONT_UNAVAILABLE) {
                    // The font provider is now downloading. Ask RetryPolicy for when to retry next.
                    synchronized (mLock) {
                        if (mRetryPolicy != null) {
                            final long delayMs = mRetryPolicy.getRetryDelay();
                            if (delayMs >= 0) {
                                scheduleRetry(font.getUri(), delayMs);
                                return;
                            }
                        }
                    }
                }

                if (resultCode != FontsContractCompat.Columns.RESULT_CODE_OK) {
                    throw new RuntimeException("fetchFonts result is not OK. (" + resultCode + ")");
                }

                final MetadataRepo metadataRepo;
                try {
                    TraceCompat.beginSection(S_TRACE_BUILD_TYPEFACE);
                    // TODO: Good to add new API to create Typeface from FD not to open FD twice.
                    final Typeface typeface = mFontProviderHelper.buildTypeface(mContext, font);
                    final ByteBuffer buffer = TypefaceCompatUtil.mmap(mContext, null,
                            font.getUri());
                    if (buffer == null || typeface == null) {
                        throw new RuntimeException("Unable to open file.");
                    }
                    metadataRepo = MetadataRepo.create(typeface, buffer);
                } finally {
                    TraceCompat.endSection();
                }
                synchronized (mLock) {
                    if (mCallback != null) {
                        mCallback.onLoaded(metadataRepo);
                    }
                }
                cleanUp();
            } catch (Throwable t) {
                synchronized (mLock) {
                    if (mCallback != null) {
                        mCallback.onFailed(t);
                    }
                }
                cleanUp();
            }
        }
    }

    /**
     * Delegate class for mocking FontsContractCompat.fetchFonts.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class FontProviderHelper {
        /** Calls FontsContractCompat.fetchFonts. */
        public @NonNull FontFamilyResult fetchFonts(@NonNull Context context,
                @NonNull FontRequest request) throws NameNotFoundException {
            return FontsContractCompat.fetchFonts(context, null /* cancellation signal */, request);
        }

        /** Calls FontsContractCompat.buildTypeface. */
        public @Nullable Typeface buildTypeface(@NonNull Context context,
                FontsContractCompat.@NonNull FontInfo font) throws NameNotFoundException {
            return FontsContractCompat.buildTypeface(context, null /* cancellation signal */,
                new FontsContractCompat.FontInfo[] { font });
        }

        /** Calls Context.getContentObserver().registerObserver */
        public void registerObserver(@NonNull Context context, @NonNull Uri uri,
                @NonNull ContentObserver observer) {
            context.getContentResolver().registerContentObserver(
                    uri, false /* notifyForDescendants */, observer);

        }
        /** Calls Context.getContentObserver().unregisterObserver */
        public void unregisterObserver(@NonNull Context context,
                @NonNull ContentObserver observer) {
            context.getContentResolver().unregisterContentObserver(observer);
        }
    }

    private static final FontProviderHelper DEFAULT_FONTS_CONTRACT = new FontProviderHelper();

}
