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

package android.support.text.emoji;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.graphics.TypefaceCompatUtil;
import android.support.v4.provider.FontRequest;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.FontsContractCompat.FontFamilyResult;
import android.support.v4.util.Preconditions;

import java.nio.ByteBuffer;

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
     * {@link FontRequestEmojiCompatConfig#setHandler}.
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
    };

    /**
     * @param context Context instance, cannot be {@code null}
     * @param request {@link FontRequest} to fetch the font asynchronously, cannot be {@code null}
     */
    public FontRequestEmojiCompatConfig(@NonNull Context context, @NonNull FontRequest request) {
        super(new FontRequestMetadataLoader(context, request, DEFAULT_FONTS_CONTRACT));
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FontRequestEmojiCompatConfig(@NonNull Context context, @NonNull FontRequest request,
            @NonNull FontProviderHelper fontProviderHelper) {
        super(new FontRequestMetadataLoader(context, request, fontProviderHelper));
    }

    /**
     * Sets the custom handler to be used for initialization.
     *
     * Since font fetch take longer time, the metadata loader will fetch the fonts on the background
     * thread. You can pass your own handler for this background fetching. This handler is also used
     * for retrying.
     *
     * @param handler A {@link Handler} to be used for initialization. Can be {@code null}. In case
     *               of {@code null}, the metadata loader creates own {@link HandlerThread} for
     *               initialization.
     */
    public FontRequestEmojiCompatConfig setHandler(Handler handler) {
        ((FontRequestMetadataLoader) getMetadataRepoLoader()).setHandler(handler);
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
    public FontRequestEmojiCompatConfig setRetryPolicy(RetryPolicy policy) {
        ((FontRequestMetadataLoader) getMetadataRepoLoader()).setRetryPolicy(policy);
        return this;
    }

    /**
     * MetadataRepoLoader implementation that uses FontsContractCompat and TypefaceCompat to load a
     * given FontRequest.
     */
    private static class FontRequestMetadataLoader implements EmojiCompat.MetadataRepoLoader {
        private final Context mContext;
        private final FontRequest mRequest;
        private final FontProviderHelper mFontProviderHelper;

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private Handler mHandler;
        @GuardedBy("mLock")
        private HandlerThread mThread;
        @GuardedBy("mLock")
        private @Nullable RetryPolicy mRetryPolicy;

        // Following three variables must be touched only on the thread associated with mHandler.
        private EmojiCompat.MetadataRepoLoaderCallback mCallback;
        private ContentObserver mObserver;
        private Runnable mHandleMetadataCreationRunner;

        FontRequestMetadataLoader(@NonNull Context context, @NonNull FontRequest request,
                @NonNull FontProviderHelper fontProviderHelper) {
            Preconditions.checkNotNull(context, "Context cannot be null");
            Preconditions.checkNotNull(request, "FontRequest cannot be null");
            mContext = context.getApplicationContext();
            mRequest = request;
            mFontProviderHelper = fontProviderHelper;
        }

        public void setHandler(Handler handler) {
            synchronized (mLock) {
                mHandler = handler;
            }
        }

        public void setRetryPolicy(RetryPolicy policy) {
            synchronized (mLock) {
                mRetryPolicy = policy;
            }
        }

        @Override
        @RequiresApi(19)
        public void load(@NonNull final EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            Preconditions.checkNotNull(loaderCallback, "LoaderCallback cannot be null");
            synchronized (mLock) {
                if (mHandler == null) {
                    // Developer didn't give a thread for fetching. Create our own one.
                    mThread = new HandlerThread("emojiCompat", Process.THREAD_PRIORITY_BACKGROUND);
                    mThread.start();
                    mHandler = new Handler(mThread.getLooper());
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback = loaderCallback;
                        createMetadata();
                    }
                });
            }
        }

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

        // Must be called on the mHandler.
        @RequiresApi(19)
        private void scheduleRetry(Uri uri, long waitMs) {
            synchronized (mLock) {
                if (mObserver == null) {
                    mObserver = new ContentObserver(mHandler) {
                        @Override
                        public void onChange(boolean selfChange, Uri uri) {
                            createMetadata();
                        }
                    };
                    mFontProviderHelper.registerObserver(mContext, uri, mObserver);
                }
                if (mHandleMetadataCreationRunner == null) {
                    mHandleMetadataCreationRunner = new Runnable() {
                        @Override
                        public void run() {
                            createMetadata();
                        }
                    };
                }
                mHandler.postDelayed(mHandleMetadataCreationRunner, waitMs);
            }
        }

        // Must be called on the mHandler.
        private void cleanUp() {
            mCallback = null;
            if (mObserver != null) {
                mFontProviderHelper.unregisterObserver(mContext, mObserver);
                mObserver = null;
            }
            synchronized (mLock) {
                mHandler.removeCallbacks(mHandleMetadataCreationRunner);
                if (mThread != null) {
                    mThread.quit();
                }
                mHandler = null;
                mThread = null;
            }
        }

        // Must be called on the mHandler.
        @RequiresApi(19)
        private void createMetadata() {
            if (mCallback == null) {
                return;  // Already handled or cancelled. Do nothing.
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

                // TODO: Good to add new API to create Typeface from FD not to open FD twice.
                final Typeface typeface = mFontProviderHelper.buildTypeface(mContext, font);
                final ByteBuffer buffer = TypefaceCompatUtil.mmap(mContext, null, font.getUri());
                if (buffer == null) {
                    throw new RuntimeException("Unable to open file.");
                }
                mCallback.onLoaded(MetadataRepo.create(typeface, buffer));
                cleanUp();
            } catch (Throwable t) {
                mCallback.onFailed(t);
                cleanUp();
            }
        }
    }

    /**
     * Delegate class for mocking FontsContractCompat.fetchFonts.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class FontProviderHelper {
        /** Calls FontsContractCompat.fetchFonts. */
        public FontFamilyResult fetchFonts(@NonNull Context context,
                @NonNull FontRequest request) throws NameNotFoundException {
            return FontsContractCompat.fetchFonts(context, null /* cancellation signal */, request);
        }

        /** Calls FontsContractCompat.buildTypeface. */
        public Typeface buildTypeface(@NonNull Context context,
                @NonNull FontsContractCompat.FontInfo font) throws NameNotFoundException {
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
    };

    private static final FontProviderHelper DEFAULT_FONTS_CONTRACT = new FontProviderHelper();

}
