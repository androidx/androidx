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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.provider.FontRequest;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.FontsContractCompat.FontFamilyResult;
import android.support.v4.provider.FontsContractCompat.FontInfo;
import android.support.v4.util.Preconditions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * {@link EmojiCompat.Config} implementation that asynchronously fetches the required font and the
 * metadata using a {@link FontRequest}. FontRequest should be constructed to fetch an EmojiCompat
 * compatible emoji font.
 * <p/>
 */
public class FontRequestEmojiCompatConfig extends EmojiCompat.Config {
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
            @NonNull FontsContractDelegate fontsContract) {
        super(new FontRequestMetadataLoader(context, request, fontsContract));
    }


    /**
     * MetadataRepoLoader implementation that uses FontsContractCompat and TypefaceCompat to load a
     * given FontRequest.
     */
    private static class FontRequestMetadataLoader implements EmojiCompat.MetadataRepoLoader {
        private final Context mContext;
        private final FontRequest mRequest;
        private final FontsContractDelegate mFontsContract;

        FontRequestMetadataLoader(@NonNull Context context, @NonNull FontRequest request,
                @NonNull FontsContractDelegate fontsContract) {
            Preconditions.checkNotNull(context, "Context cannot be null");
            Preconditions.checkNotNull(request, "FontRequest cannot be null");
            mContext = context.getApplicationContext();
            mRequest = request;
            mFontsContract = fontsContract;
        }

        @Override
        @RequiresApi(19)
        public void load(@NonNull final EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            Preconditions.checkNotNull(loaderCallback, "LoaderCallback cannot be null");
            final InitRunnable runnable =
                    new InitRunnable(mContext, mRequest, mFontsContract, loaderCallback);
            final Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            thread.start();
        }
    }

    /**
     * Runnable used to create the Typeface and MetadataRepo from a given FontResult.
     */
    @RequiresApi(19)
    private static class InitRunnable implements Runnable {
        private final EmojiCompat.MetadataRepoLoaderCallback mLoaderCallback;
        private final Context mContext;
        private final FontsContractDelegate mFontsContract;
        private final FontRequest mFontRequest;

        private InitRunnable(final Context context,
                final FontRequest fontRequest,
                final FontsContractDelegate fontsContract,
                final EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            mContext = context;
            mFontRequest = fontRequest;
            mFontsContract = fontsContract;
            mLoaderCallback = loaderCallback;
        }

        @Override
        public void run() {
            try {
                FontFamilyResult result = null;
                try {
                    result = mFontsContract.fetchFonts(mContext, mFontRequest);
                } catch (NameNotFoundException e) {
                    throwException("provider not found");
                }
                if (result.getStatusCode() != FontFamilyResult.STATUS_OK) {
                    throwException("fetchFonts failed (" + result.getStatusCode() + ")");
                }
                final FontInfo[] fonts = result.getFonts();
                if (fonts == null || fonts.length == 0) {
                    throwException("fetchFonts failed (empty result)");
                }
                // Assuming the GMS Core provides only one font file.
                final FontInfo font = fonts[0];
                if (font.getResultCode() != FontsContractCompat.Columns.RESULT_CODE_OK) {
                    throwException("fetchFonts result is not OK. (" + font.getResultCode() + ")");
                }

                final ContentResolver resolver = mContext.getContentResolver();
                ByteBuffer buffer = null;
                try (ParcelFileDescriptor fd = resolver.openFileDescriptor(font.getUri(), "r");
                    FileInputStream inputStream = new FileInputStream(fd.getFileDescriptor())) {
                    final FileChannel fileChannel = inputStream.getChannel();
                    buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
                } catch (FileNotFoundException e) {
                    throwException("Unable to open file.");
                }

                // TODO(nona): Introduce public API to make Typeface from filedescriptor so that we
                // can stop opening file descriptor twice.
                final Typeface typeface = FontsContractCompat.buildTypeface(mContext,
                        null /* cancellation signal */, fonts);
                if (typeface == null) {
                    throwException("Failed to create Typeface.");
                }

                mLoaderCallback.onLoaded(MetadataRepo.create(typeface, buffer));
            } catch (Throwable t) {
                mLoaderCallback.onFailed(t);
            }
        }
    }

    private static void throwException(String msg) {
        throw new RuntimeException("Cannot load metadata: " + msg);
    }

    /**
     * Delegate class for mocking FontsContractCompat.fetchFonts.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class FontsContractDelegate {
        /** Calls FontsContractCompat.fetchFonts. */
        public FontFamilyResult fetchFonts(@NonNull Context context,
                @NonNull FontRequest request) throws NameNotFoundException {
            return FontsContractCompat.fetchFonts(context, null /* cancellation signal */, request);
        }
    };

    private static final FontsContractDelegate DEFAULT_FONTS_CONTRACT = new FontsContractDelegate();

}
