/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.emoji.text;

import static android.content.res.AssetManager.ACCESS_BUFFER;

import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_FONT_NOT_FOUND;
import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_FONT_UNAVAILABLE;
import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_MALFORMED_QUERY;
import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_OK;
import static androidx.core.provider.FontsContractCompat.FontFamilyResult.STATUS_OK;
import static androidx.core.provider.FontsContractCompat.FontFamilyResult.STATUS_WRONG_CERTIFICATES;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.FontRequest;
import androidx.core.provider.FontsContractCompat.FontFamilyResult;
import androidx.core.provider.FontsContractCompat.FontInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class FontRequestEmojiCompatConfigTest {
    private static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    private Context mContext;
    private FontRequest mFontRequest;
    private FontRequestEmojiCompatConfig.FontProviderHelper mFontProviderHelper;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mFontRequest = new FontRequest("authority", "package", "query",
                new ArrayList<List<byte[]>>());
        mFontProviderHelper = mock(FontRequestEmojiCompatConfig.FontProviderHelper.class);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_withNullContext() {
        new FontRequestEmojiCompatConfig(null, mFontRequest);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_withNullFontRequest() {
        new FontRequestEmojiCompatConfig(mContext, null);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_whenGetFontThrowsException() throws NameNotFoundException {
        final Exception exception = new RuntimeException();
        doThrow(exception).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext, mFontRequest,
                mFontProviderHelper);

        config.getMetadataRepoLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);
        verify(callback, times(1)).onFailed(same(exception));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_providerNotFound() throws NameNotFoundException {
        doThrow(new NameNotFoundException()).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                mFontRequest, mFontProviderHelper);

        config.getMetadataRepoLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);

        final ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(callback, times(1)).onFailed(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getMessage(), containsString("provider not found"));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_wrongCertificate() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_WRONG_CERTIFICATES, null /* fonts */,
                "fetchFonts failed (" + STATUS_WRONG_CERTIFICATES + ")");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_fontNotFound() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_FONT_NOT_FOUND),
                "fetchFonts result is not OK. (" + RESULT_CODE_FONT_NOT_FOUND + ")");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_fontUnavailable() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_FONT_UNAVAILABLE),
                "fetchFonts result is not OK. (" + RESULT_CODE_FONT_UNAVAILABLE + ")");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_malformedQuery() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_MALFORMED_QUERY),
                "fetchFonts result is not OK. (" + RESULT_CODE_MALFORMED_QUERY + ")");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_resultNotFound() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK, new FontInfo[] {},
                "fetchFonts failed (empty result)");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_nullFontInfo() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK, null /* fonts */,
                "fetchFonts failed (empty result)");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_cannotLoadTypeface() throws NameNotFoundException {
        // getTestFontInfoWithInvalidPath returns FontInfo with invalid path to file.
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_OK),
                "Unable to open file.");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_success() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final FontInfo[] fonts =  new FontInfo[] {
                new FontInfo(Uri.fromFile(file), 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_OK)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                mFontRequest, mFontProviderHelper);

        config.getMetadataRepoLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);
        verify(callback, times(1)).onLoaded(any(MetadataRepo.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_retryPolicy() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final FontInfo[] fonts =  new FontInfo[] {
                new FontInfo(Uri.fromFile(file), 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_FONT_UNAVAILABLE)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final WaitingRetryPolicy retryPolicy = spy(new WaitingRetryPolicy(-1, 1));
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                mFontRequest, mFontProviderHelper).setRetryPolicy(retryPolicy);

        config.getMetadataRepoLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);
        verify(callback, never()).onLoaded(any(MetadataRepo.class));
        verify(callback, times(1)).onFailed(any(Throwable.class));
        verify(retryPolicy, times(1)).getRetryDelay();
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_keepRetryingAndGiveUp() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final FontInfo[] fonts =  new FontInfo[] {
                new FontInfo(Uri.fromFile(file), 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_FONT_UNAVAILABLE)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final WaitingRetryPolicy retryPolicy = spy(new WaitingRetryPolicy(500, 1));
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                mFontRequest, mFontProviderHelper).setRetryPolicy(retryPolicy);

        config.getMetadataRepoLoader().load(callback);
        retryPolicy.await(DEFAULT_TIMEOUT_MILLIS);
        verify(callback, never()).onLoaded(any(MetadataRepo.class));
        verify(callback, never()).onFailed(any(Throwable.class));
        verify(retryPolicy, atLeastOnce()).getRetryDelay();
        retryPolicy.changeReturnValue(-1);
        callback.await(DEFAULT_TIMEOUT_MILLIS);
        verify(callback, never()).onLoaded(any(MetadataRepo.class));
        verify(callback, times(1)).onFailed(any(Throwable.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_keepRetryingAndFail() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final Uri uri = Uri.fromFile(file);

        final FontInfo[] fonts = new FontInfo[] {
                new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_FONT_UNAVAILABLE)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final WaitingRetryPolicy retryPolicy = spy(new WaitingRetryPolicy(500, 1));

        HandlerThread thread = new HandlerThread("testThread");
        thread.start();
        try {
            Handler handler = new Handler(thread.getLooper());

            final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                    mFontRequest, mFontProviderHelper).setHandler(handler)
                    .setRetryPolicy(retryPolicy);

            config.getMetadataRepoLoader().load(callback);
            retryPolicy.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, never()).onLoaded(any(MetadataRepo.class));
            verify(callback, never()).onFailed(any(Throwable.class));
            verify(retryPolicy, atLeastOnce()).getRetryDelay();

            // To avoid race condition, change the fetchFonts result on the handler thread.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        final FontInfo[] fontsSuccess = new FontInfo[] {
                                new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                                        false /* italic */, RESULT_CODE_FONT_NOT_FOUND)
                        };

                        doReturn(new FontFamilyResult(STATUS_OK, fontsSuccess)).when(
                                mFontProviderHelper).fetchFonts(any(Context.class),
                                any(FontRequest.class));
                    } catch (NameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            callback.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, never()).onLoaded(any(MetadataRepo.class));
            verify(callback, times(1)).onFailed(any(Throwable.class));
        } finally {
            thread.quit();
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_keepRetryingAndSuccess() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final Uri uri = Uri.fromFile(file);

        final FontInfo[] fonts = new FontInfo[]{
                new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_FONT_UNAVAILABLE)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final WaitingRetryPolicy retryPolicy = spy(new WaitingRetryPolicy(500, 1));

        HandlerThread thread = new HandlerThread("testThread");
        thread.start();
        try {
            Handler handler = new Handler(thread.getLooper());

            final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                    mFontRequest, mFontProviderHelper).setHandler(handler)
                    .setRetryPolicy(retryPolicy);

            config.getMetadataRepoLoader().load(callback);
            retryPolicy.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, never()).onLoaded(any(MetadataRepo.class));
            verify(callback, never()).onFailed(any(Throwable.class));
            verify(retryPolicy, atLeastOnce()).getRetryDelay();

            final FontInfo[] fontsSuccess = new FontInfo[]{
                    new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                            false /* italic */, RESULT_CODE_OK)
            };

            // To avoid race condition, change the fetchFonts result on the handler thread.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        doReturn(new FontFamilyResult(STATUS_OK, fontsSuccess)).when(
                                mFontProviderHelper).fetchFonts(any(Context.class),
                                any(FontRequest.class));
                    } catch (NameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            callback.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, times(1)).onLoaded(any(MetadataRepo.class));
            verify(callback, never()).onFailed(any(Throwable.class));
        } finally {
            thread.quit();
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_ObserverNotifyAndSuccess() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final Uri uri = Uri.fromFile(file);
        final FontInfo[] fonts = new FontInfo[]{
                new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_FONT_UNAVAILABLE)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final WaitingRetryPolicy retryPolicy = spy(new WaitingRetryPolicy(500, 2));

        HandlerThread thread = new HandlerThread("testThread");
        thread.start();
        try {
            Handler handler = new Handler(thread.getLooper());
            final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                    mFontRequest, mFontProviderHelper).setHandler(handler)
                    .setRetryPolicy(retryPolicy);

            ArgumentCaptor<ContentObserver> observerCaptor =
                    ArgumentCaptor.forClass(ContentObserver.class);

            config.getMetadataRepoLoader().load(callback);
            retryPolicy.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, never()).onLoaded(any(MetadataRepo.class));
            verify(callback, never()).onFailed(any(Throwable.class));
            verify(retryPolicy, atLeastOnce()).getRetryDelay();
            verify(mFontProviderHelper, times(1)).registerObserver(
                    any(Context.class), eq(uri), observerCaptor.capture());

            final FontInfo[] fontsSuccess = new FontInfo[]{
                    new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                            false /* italic */, RESULT_CODE_OK)
            };
            doReturn(new FontFamilyResult(STATUS_OK, fontsSuccess)).when(
                    mFontProviderHelper).fetchFonts(any(Context.class), any(FontRequest.class));

            final ContentObserver observer = observerCaptor.getValue();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    observer.onChange(false /* self change */, uri);
                }
            });

            callback.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, times(1)).onLoaded(any(MetadataRepo.class));
            verify(callback, never()).onFailed(any(Throwable.class));
        } finally {
            thread.quit();
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_ObserverNotifyAndFail() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final Uri uri = Uri.fromFile(file);
        final FontInfo[] fonts = new FontInfo[]{
                new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_FONT_UNAVAILABLE)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final WaitingRetryPolicy retryPolicy = spy(new WaitingRetryPolicy(500, 2));

        HandlerThread thread = new HandlerThread("testThread");
        thread.start();
        try {
            Handler handler = new Handler(thread.getLooper());
            final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                    mFontRequest, mFontProviderHelper).setHandler(handler)
                    .setRetryPolicy(retryPolicy);

            ArgumentCaptor<ContentObserver> observerCaptor =
                    ArgumentCaptor.forClass(ContentObserver.class);

            config.getMetadataRepoLoader().load(callback);
            retryPolicy.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, never()).onLoaded(any(MetadataRepo.class));
            verify(callback, never()).onFailed(any(Throwable.class));
            verify(retryPolicy, atLeastOnce()).getRetryDelay();
            verify(mFontProviderHelper, times(1)).registerObserver(
                    any(Context.class), eq(uri), observerCaptor.capture());

            final FontInfo[] fontsSuccess = new FontInfo[]{
                    new FontInfo(uri, 0 /* ttc index */, 400 /* weight */,
                            false /* italic */, RESULT_CODE_FONT_NOT_FOUND)
            };
            doReturn(new FontFamilyResult(STATUS_OK, fontsSuccess)).when(
                    mFontProviderHelper).fetchFonts(any(Context.class), any(FontRequest.class));

            final ContentObserver observer = observerCaptor.getValue();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    observer.onChange(false /* self change */, uri);
                }
            });

            callback.await(DEFAULT_TIMEOUT_MILLIS);
            verify(callback, never()).onLoaded(any(MetadataRepo.class));
            verify(callback, times(1)).onFailed(any(Throwable.class));
        } finally {
            thread.quit();
        }
    }

    private void verifyLoaderOnFailedCalled(final int statusCode,
            final FontInfo[] fonts, String exceptionMessage) throws NameNotFoundException {
        doReturn(new FontFamilyResult(statusCode, fonts)).when(mFontProviderHelper).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext, mFontRequest,
                mFontProviderHelper);

        config.getMetadataRepoLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);

        final ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(callback, times(1)).onFailed(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getMessage(), containsString(exceptionMessage));
    }

    public static class WaitingRetryPolicy extends FontRequestEmojiCompatConfig.RetryPolicy {
        private final CountDownLatch mLatch;
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private long mReturnValue;

        public WaitingRetryPolicy(long returnValue, int callCount) {
            mLatch = new CountDownLatch(callCount);
            synchronized (mLock) {
                mReturnValue = returnValue;
            }
        }

        @Override
        public long getRetryDelay() {
            mLatch.countDown();
            synchronized (mLock) {
                return mReturnValue;
            }
        }

        public void changeReturnValue(long value) {
            synchronized (mLock) {
                mReturnValue = value;
            }
        }

        public void await(long timeoutMillis) {
            try {
                mLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class WaitingLoaderCallback extends EmojiCompat.MetadataRepoLoaderCallback {
        final CountDownLatch mLatch;

        public WaitingLoaderCallback() {
            mLatch = new CountDownLatch(1);
        }

        @Override
        public void onLoaded(@NonNull MetadataRepo metadataRepo) {
            mLatch.countDown();
        }

        @Override
        public void onFailed(@Nullable Throwable throwable) {
            mLatch.countDown();
        }

        public void await(long timeoutMillis) {
            try {
                mLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static File loadFont(Context context, String fileName) {
        File cacheFile = new File(context.getCacheDir(), fileName);
        try {
            copyToCacheFile(context, fileName, cacheFile);
            return cacheFile;
        } catch (IOException e) {
            fail();
        }
        return null;
    }

    private static void copyToCacheFile(final Context context, final String assetPath,
            final File cacheFile) throws IOException {
        try (InputStream is = context.getAssets().open(assetPath, ACCESS_BUFFER);
             FileOutputStream fos = new FileOutputStream(cacheFile, false)) {
            byte[] buffer = new byte[1024];
            int readLen;
            while ((readLen = is.read(buffer)) != -1) {
                fos.write(buffer, 0, readLen);
            }
        }
    }

    private FontInfo[] getTestFontInfoWithInvalidPath(int resultCode) {
        return new FontInfo[] { new FontInfo(Uri.parse("file:///some/dummy/file"),
                0 /* ttc index */, 400 /* weight */, false /* italic */, resultCode) };
    }
}
