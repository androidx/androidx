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

package androidx.slice;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import androidx.core.os.BuildCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

import androidx.slice.render.SliceRenderActivity;
import androidx.slice.widget.SliceLiveData;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceManagerTest {

    private final Context mContext = InstrumentationRegistry.getContext();
    private SliceProvider mSliceProvider;
    private SliceManager mManager;

    @Before
    public void setup() {
        TestSliceProvider.sSliceProviderReceiver = mSliceProvider = mock(SliceProvider.class);
        mManager = SliceManager.getInstance(mContext);
    }

    @Test
    public void testPin() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        try {
            mManager.pinSlice(uri);
            verify(mSliceProvider, timeout(2000)).onSlicePinned(eq(uri));
        } finally {
            mManager.unpinSlice(uri);
        }
    }

    @Test
    public void testUnpin() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        mManager.pinSlice(uri);
        verify(mSliceProvider, timeout(2000)).onSlicePinned(eq(uri));
        clearInvocations(mSliceProvider);
        mManager.unpinSlice(uri);
        verify(mSliceProvider, timeout(2000)).onSliceUnpinned(eq(uri));
    }

    @Test
    public void testCallback() {
        if (BuildCompat.isAtLeastP()) {
            return;
        }
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        Slice s = new Slice.Builder(uri).build();
        SliceManager.SliceCallback callback = mock(SliceManager.SliceCallback.class);
        when(mSliceProvider.onBindSlice(eq(uri))).thenReturn(s);
        mManager.registerSliceCallback(uri, new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                command.run();
            }
        }, callback);

        mContext.getContentResolver().notifyChange(uri, null);

        verify(callback, timeout(2000)).onSliceUpdated(any(Slice.class));
    }

    @Test
    public void testPinnedSpecs() {
        if (BuildCompat.isAtLeastP()) {
            return;
        }
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        mManager.pinSlice(uri);
        verify(mSliceProvider).onSlicePinned(eq(uri));

        assertEquals(SliceLiveData.SUPPORTED_SPECS, mManager.getPinnedSpecs(uri));
    }

    @Test
    public void testMapIntentToUri() {
        Uri expected = Uri.parse("content://androidx.slice.view.test/render");
        Slice s = new Slice.Builder(expected).build();
        when(mSliceProvider.onBindSlice(eq(expected))).thenReturn(s);
        Uri uri = mManager.mapIntentToUri(new Intent(mContext, SliceRenderActivity.class));
        assertEquals(expected, uri);
    }

    public static class TestSliceProvider extends SliceProvider {

        public static SliceProvider sSliceProviderReceiver;

        @Override
        public boolean onCreateSliceProvider() {
            if (sSliceProviderReceiver != null) {
                sSliceProviderReceiver.onCreateSliceProvider();
            }
            return true;
        }

        @Override
        public Slice onBindSlice(Uri sliceUri) {
            if (sSliceProviderReceiver != null) {
                return sSliceProviderReceiver.onBindSlice(sliceUri);
            }
            return null;
        }

        @NonNull
        @Override
        public Uri onMapIntentToUri(Intent intent) {
            if (sSliceProviderReceiver != null) {
                return sSliceProviderReceiver.onMapIntentToUri(intent);
            }
            return null;
        }

        @Override
        public void onSlicePinned(Uri sliceUri) {
            if (sSliceProviderReceiver != null) {
                sSliceProviderReceiver.onSlicePinned(sliceUri);
            }
        }

        @Override
        public void onSliceUnpinned(Uri sliceUri) {
            if (sSliceProviderReceiver != null) {
                sSliceProviderReceiver.onSliceUnpinned(sliceUri);
            }
        }
    }
}
