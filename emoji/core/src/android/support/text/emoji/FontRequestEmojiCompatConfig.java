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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.graphics.TypefaceCompat;
import android.support.v4.graphics.TypefaceCompat.FontRequestCallback;
import android.support.v4.graphics.fonts.FontRequest;
import android.support.v4.graphics.fonts.FontResult;
import android.support.v4.os.ResultReceiver;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.FontsContractInternal;
import android.support.v4.util.Preconditions;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

/**
 * {@link EmojiCompat.Config} implementation that asynchronously fetches the required font and the
 * metadata using a {@link FontRequest}. FontRequest should be constructed to fetch an EmojiCompat
 * compatible emoji font.
 * <p/>
 * See {@link FontsContractCompat.FontRequestCallback#onTypefaceRequestFailed(int)} for more
 * information about the cases where the font loading can fail.
 */
public class FontRequestEmojiCompatConfig extends EmojiCompat.Config {

    /**
     * @param context Context instance, cannot be {@code null}
     * @param request {@link FontRequest} to fetch the font asynchronously, cannot be {@code null}
     */
    public FontRequestEmojiCompatConfig(@NonNull Context context, @NonNull FontRequest request) {
        super(new FontRequestMetadataLoader(context, request));
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FontRequestEmojiCompatConfig(@NonNull Context context, @NonNull FontRequest request,
            @NonNull FontsContractInternal fontsContract) {
        super(new FontRequestMetadataLoader(context, request, fontsContract));
    }


    /**
     * MetadataLoader implementation that uses FontsContractInternal and TypefaceCompat to load a
     * given FontRequest.
     */
    private static class FontRequestMetadataLoader implements EmojiCompat.MetadataLoader {
        private final Context mContext;
        private final FontRequest mRequest;
        private final FontsContractInternal mFontsContract;

        FontRequestMetadataLoader(@NonNull Context context, @NonNull FontRequest request) {
            this(context, request, new FontsContractInternal(context));
        }

        FontRequestMetadataLoader(@NonNull Context context, @NonNull FontRequest request,
                @NonNull FontsContractInternal fontsContract) {
            Preconditions.checkNotNull(context, "Context cannot be null");
            Preconditions.checkNotNull(request, "FontRequest cannot be null");
            mContext = context.getApplicationContext();
            mRequest = request;
            mFontsContract = fontsContract;
        }

        @Override
        public void load(@NonNull final EmojiCompat.LoaderCallback loaderCallback) {
            Preconditions.checkNotNull(loaderCallback, "LoaderCallback cannot be null");
            final ResultReceiver receiver = new ResultReceiver(null) {
                @Override
                public void onReceiveResult(final int resultCode, final Bundle resultData) {
                    receiveResult(loaderCallback, resultCode, resultData);
                }
            };
            try {
                mFontsContract.getFont(mRequest, receiver);
            } catch (Throwable throwable) {
                loaderCallback.onFailed(throwable);
            }
        }

        private void receiveResult(final EmojiCompat.LoaderCallback loaderCallback,
                final int resultCode, final Bundle resultData) {
            try {
                if (resultCode != FontsContractCompat.Columns.RESULT_CODE_OK) {
                    throwException(resultCode);
                }

                if (resultData == null) {
                    throwException(FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
                }

                final List<FontResult> fontResults = resultData.getParcelableArrayList(
                        FontsContractInternal.PARCEL_FONT_RESULTS);
                if (fontResults == null || fontResults.isEmpty()) {
                    throwException(FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
                }

                final InitRunnable runnable = new InitRunnable(mContext, fontResults.get(0),
                        loaderCallback);
                final Thread thread = new Thread(runnable);
                thread.setDaemon(false);
                thread.start();
            } catch (Throwable t) {
                loaderCallback.onFailed(t);
            }
        }
    }

    /**
     * Runnable used to create the Typeface and MetadataRepo from a given FontResult.
     */
    private static class InitRunnable implements Runnable {
        private final EmojiCompat.LoaderCallback mLoaderCallback;
        private final Context mContext;
        private final FontResult mFontResult;

        private InitRunnable(final Context context,
                final FontResult fontResult,
                final EmojiCompat.LoaderCallback loaderCallback) {
            mContext = context;
            mFontResult = fontResult;
            mLoaderCallback = loaderCallback;
        }

        @Override
        public void run() {
            try {
                final ParcelFileDescriptor dupFd = mFontResult.getFileDescriptor().dup();
                // this one will close fd that is in mFontResult
                final Typeface typeface = TypefaceCompat.createTypeface(mContext,
                        Arrays.asList(mFontResult));
                if (typeface == null) {
                    throwException(FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR);
                }
                // this one will close dupFd
                final MetadataRepo metadataRepo = createMetadataRepo(typeface, dupFd);
                mLoaderCallback.onLoaded(metadataRepo);
            } catch (Throwable t) {
                mLoaderCallback.onFailed(t);
            }
        }

        private MetadataRepo createMetadataRepo(final Typeface typeface,
                final ParcelFileDescriptor parcelFileDescriptor) throws IOException {
            try (ParcelFileDescriptor pfd = parcelFileDescriptor;
                 FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor())) {
                final FileChannel fileChannel = inputStream.getChannel();
                final ByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0,
                        fileChannel.size());
                final MetadataRepo metadataRepo = MetadataRepo.create(typeface, buffer);
                return metadataRepo;
            }
        }
    }

    private static void throwException(int code) {
        throw new RuntimeException("Cannot load metadata, error code:" + Integer.toString(code));
    }
}
