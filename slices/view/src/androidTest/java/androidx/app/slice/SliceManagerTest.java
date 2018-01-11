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

package androidx.app.slice;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.os.BuildCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.Executor;

import androidx.app.slice.widget.SliceLiveData;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceManagerTest {

    private final Context mContext = InstrumentationRegistry.getContext();
    private SliceProvider mSliceProvider;
    private SliceManager mManager;

    @Before
    public void setup() {
        TestSliceProvider.sSliceProviderReceiver = mSliceProvider = mock(SliceProvider.class);
        mManager = createSliceManager(mContext);
    }

    private SliceManager createSliceManager(Context context) {
        if (BuildCompat.isAtLeastP()) {
            android.app.slice.SliceManager manager = mock(android.app.slice.SliceManager.class);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    TestSliceProvider.sSliceProviderReceiver.onSlicePinned(
                            (Uri) invocation.getArguments()[0]);
                    return null;
                }
            }).when(manager).pinSlice(any(Uri.class), any(List.class));
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    TestSliceProvider.sSliceProviderReceiver.onSliceUnpinned(
                            (Uri) invocation.getArguments()[0]);
                    return null;
                }
            }).when(manager).unpinSlice(any(Uri.class));
            return new SliceManagerWrapper(context, manager);
        } else {
            return SliceManager.get(context);
        }
    }

    @Test
    public void testPin() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        mManager.pinSlice(uri);
        verify(mSliceProvider).onSlicePinned(eq(uri));
    }

    @Test
    public void testUnpin() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        mManager.pinSlice(uri);
        clearInvocations(mSliceProvider);
        mManager.unpinSlice(uri);
        verify(mSliceProvider).onSliceUnpinned(eq(uri));
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
