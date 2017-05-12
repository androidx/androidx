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

import static android.content.res.AssetManager.ACCESS_BUFFER;
import static android.support.v4.provider.FontsContractCompat.Columns.RESULT_CODE_FONT_NOT_FOUND;
import static android.support.v4.provider.FontsContractCompat.Columns.RESULT_CODE_FONT_UNAVAILABLE;
import static android.support.v4.provider.FontsContractCompat.Columns.RESULT_CODE_MALFORMED_QUERY;
import static android.support.v4.provider.FontsContractCompat.Columns.RESULT_CODE_OK;
import static android.support.v4.provider.FontsContractCompat.FontFamilyResult.STATUS_OK;
import static android.support.v4.provider.FontsContractCompat.FontFamilyResult.STATUS_WRONG_CERTIFICATES;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.provider.FontRequest;
import android.support.v4.provider.FontsContractCompat.FontFamilyResult;
import android.support.v4.provider.FontsContractCompat.FontInfo;

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
    private FontRequestEmojiCompatConfig.FontsContractDelegate mFontsContract;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mFontRequest = new FontRequest("authority", "package", "query",
                new ArrayList<List<byte[]>>());
        mFontsContract = mock(FontRequestEmojiCompatConfig.FontsContractDelegate.class);
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
    public void testLoad_whenGetFontThrowsException() throws NameNotFoundException {
        final Exception exception = new RuntimeException();
        doThrow(exception).when(mFontsContract).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext, mFontRequest,
                mFontsContract);

        config.getMetadataLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);
        verify(callback, times(1)).onFailed(same(exception));
    }

    @Test
    public void testLoad_providerNotFound() throws NameNotFoundException {
        doThrow(new NameNotFoundException()).when(mFontsContract).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                mFontRequest, mFontsContract);

        config.getMetadataLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);

        final ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(callback, times(1)).onFailed(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getMessage(), containsString("provider not found"));
    }

    @Test
    public void testLoad_wrongCertificate() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_WRONG_CERTIFICATES, null /* fonts */,
                "fetchFonts failed (" + STATUS_WRONG_CERTIFICATES + ")");
    }

    @Test
    public void testLoad_fontNotFound() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_FONT_NOT_FOUND),
                "fetchFonts result is not OK. (" + RESULT_CODE_FONT_NOT_FOUND + ")");
    }

    @Test
    public void testLoad_fontUnavailable() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_FONT_UNAVAILABLE),
                "fetchFonts result is not OK. (" + RESULT_CODE_FONT_UNAVAILABLE + ")");
    }

    @Test
    public void testLoad_malformedQuery() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_MALFORMED_QUERY),
                "fetchFonts result is not OK. (" + RESULT_CODE_MALFORMED_QUERY + ")");
    }

    @Test
    public void testLoad_resultNotFound() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK, new FontInfo[] {},
                "fetchFonts failed (empty result)");
    }

    @Test
    public void testLoad_nullFontInfo() throws NameNotFoundException {
        verifyLoaderOnFailedCalled(STATUS_OK, null /* fonts */,
                "fetchFonts failed (empty result)");
    }

    @Test
    public void testLoad_cannotLoadTypeface() throws NameNotFoundException {
        // getTestFontInfoWithInvalidPath returns FontInfo with invalid path to file.
        verifyLoaderOnFailedCalled(STATUS_OK,
                getTestFontInfoWithInvalidPath(RESULT_CODE_OK),
                "Unable to open file.");
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    @TargetApi(19)
    public void testLoad_success() throws IOException, NameNotFoundException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        final FontInfo[] fonts =  new FontInfo[] {
                new FontInfo(Uri.fromFile(file), 0 /* ttc index */, 400 /* weight */,
                        false /* italic */, RESULT_CODE_OK)
        };
        doReturn(new FontFamilyResult(STATUS_OK, fonts)).when(mFontsContract).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                mFontRequest, mFontsContract);

        config.getMetadataLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);
        verify(callback, times(1)).onLoaded(any(MetadataRepo.class));
    }

    private void verifyLoaderOnFailedCalled(final int statusCode,
            final FontInfo[] fonts, String exceptionMessage) throws NameNotFoundException {
        doReturn(new FontFamilyResult(statusCode, fonts)).when(mFontsContract).fetchFonts(
                any(Context.class), any(FontRequest.class));
        final WaitingLoaderCallback callback = spy(new WaitingLoaderCallback());
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext, mFontRequest,
                mFontsContract);

        config.getMetadataLoader().load(callback);
        callback.await(DEFAULT_TIMEOUT_MILLIS);

        final ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(callback, times(1)).onFailed(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getMessage(), containsString(exceptionMessage));
    }

    public static class WaitingLoaderCallback extends EmojiCompat.LoaderCallback {
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
