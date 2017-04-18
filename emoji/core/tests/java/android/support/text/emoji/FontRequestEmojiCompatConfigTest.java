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
import static android.support.v4.graphics.TypefaceCompat.FontRequestCallback
        .FAIL_REASON_FONT_NOT_FOUND;
import static android.support.v4.graphics.TypefaceCompat.FontRequestCallback
        .FAIL_REASON_FONT_UNAVAILABLE;
import static android.support.v4.graphics.TypefaceCompat.FontRequestCallback
        .FAIL_REASON_MALFORMED_QUERY;
import static android.support.v4.graphics.TypefaceCompat.FontRequestCallback
        .FAIL_REASON_WRONG_CERTIFICATES;
import static android.support.v4.provider.FontsContract.Columns.RESULT_CODE_FONT_NOT_FOUND;
import static android.support.v4.provider.FontsContract.Columns.RESULT_CODE_FONT_UNAVAILABLE;
import static android.support.v4.provider.FontsContract.Columns.RESULT_CODE_MALFORMED_QUERY;
import static android.support.v4.provider.FontsContract.Columns.RESULT_CODE_OK;
import static android.support.v4.provider.FontsContract.RESULT_CODE_PROVIDER_NOT_FOUND;
import static android.support.v4.provider.FontsContract.RESULT_CODE_WRONG_CERTIFICATES;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.graphics.fonts.FontRequest;
import android.support.v4.graphics.fonts.FontResult;
import android.support.v4.os.ResultReceiver;
import android.support.v4.provider.FontsContract;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class FontRequestEmojiCompatConfigTest {

    private Context mContext;
    private FontRequest mFontRequest;
    private FontsContract mFontsContract;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mFontRequest = new FontRequest("authority", "package", "query",
                new ArrayList<List<byte[]>>());
        mFontsContract = mock(FontsContract.class);
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
    public void testLoad_whenGetFontThrowsException() {
        final Exception exception = new RuntimeException();
        doThrow(exception).when(mFontsContract).getFont(any(FontRequest.class),
                any(ResultReceiver.class));
        final EmojiCompat.LoaderCallback callback = mock(EmojiCompat.LoaderCallback.class);
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext, mFontRequest,
                mFontsContract);

        config.getMetadataLoader().load(callback);

        verify(callback, times(1)).onFailed(same(exception));
    }

    @Test
    public void testLoad_providerNotFound() {
        verifyLoaderOnFailedCalled(RESULT_CODE_PROVIDER_NOT_FOUND,
                RESULT_CODE_PROVIDER_NOT_FOUND/*expected*/,
                new Bundle());
    }

    @Test
    public void testLoad_wrongCertificate() {
        verifyLoaderOnFailedCalled(RESULT_CODE_WRONG_CERTIFICATES,
                FAIL_REASON_WRONG_CERTIFICATES/*expected*/,
                new Bundle());
    }

    @Test
    public void testLoad_fontNotFound() {
        verifyLoaderOnFailedCalled(RESULT_CODE_FONT_NOT_FOUND,
                FAIL_REASON_FONT_NOT_FOUND/*expected*/,
                new Bundle());
    }

    @Test
    public void testLoad_fontUnavailable() {
        verifyLoaderOnFailedCalled(RESULT_CODE_FONT_UNAVAILABLE,
                FAIL_REASON_FONT_UNAVAILABLE/*expected*/,
                new Bundle());
    }

    @Test
    public void testLoad_malformedQuery() {
        verifyLoaderOnFailedCalled(RESULT_CODE_MALFORMED_QUERY,
                FAIL_REASON_MALFORMED_QUERY/*expected*/,
                new Bundle());
    }

    @Test
    public void testLoad_resultNotFound() {
        verifyLoaderOnFailedCalled(RESULT_CODE_OK,
                FAIL_REASON_FONT_NOT_FOUND, null);
    }

    @Test
    public void testLoad_bundleReturnsNull() {
        final Bundle bundle = new Bundle();
        verifyLoaderOnFailedCalled(RESULT_CODE_OK,
                FAIL_REASON_FONT_NOT_FOUND, bundle);
    }

    @Test
    public void testLoad_cannotLoadTypeface() {
        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(FontsContract.PARCEL_FONT_RESULTS,
                new ArrayList<Parcelable>());
        verifyLoaderOnFailedCalled(RESULT_CODE_OK,
                FAIL_REASON_FONT_NOT_FOUND, bundle);
    }

    @Test
    public void testLoad_success() throws IOException, InterruptedException {
        final File file = loadFont(mContext, "NotoColorEmojiCompat.ttf");
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY)) {
            final FontResult fontResult = new FontResult(pfd, 0, null, 1, false);
            final Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(FontsContract.PARCEL_FONT_RESULTS,
                    new ArrayList<>(Arrays.asList(fontResult)));

            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    final Object[] args = invocation.getArguments();
                    final ResultReceiver receiver = (ResultReceiver) args[1];
                    receiver.send(RESULT_CODE_OK, bundle);
                    return null;
                }
            }).when(mFontsContract).getFont(any(FontRequest.class), any(ResultReceiver.class));

            final CountDownLatch latch = new CountDownLatch(1);
            final EmojiCompat.LoaderCallback callback = spy(new WaitingLoaderCallback(latch));
            final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext,
                    mFontRequest, mFontsContract);

            config.getMetadataLoader().load(callback);
            latch.await(3000, TimeUnit.SECONDS);
            verify(callback, times(1)).onLoaded(any(MetadataRepo.class));
        }
    }

    private void verifyLoaderOnFailedCalled(final int resultCode, final int expectedErrorCode,
            final Bundle resultData) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final ResultReceiver receiver = (ResultReceiver) args[1];
                receiver.send(resultCode, resultData);
                return null;
            }
        }).when(mFontsContract).getFont(any(FontRequest.class), any(ResultReceiver.class));

        final EmojiCompat.LoaderCallback callback = mock(EmojiCompat.LoaderCallback.class);
        final EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext, mFontRequest,
                mFontsContract);

        config.getMetadataLoader().load(callback);

        final ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(callback, times(1)).onFailed(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getMessage(),
                containsString("error code:" + expectedErrorCode));
    }

    public static class WaitingLoaderCallback extends EmojiCompat.LoaderCallback {
        final CountDownLatch mLatch;

        public WaitingLoaderCallback(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void onLoaded(@NonNull MetadataRepo metadataRepo) {
            mLatch.countDown();
        }

        @Override
        public void onFailed(@Nullable Throwable throwable) {
            mLatch.countDown();
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
}
