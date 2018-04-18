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

import static androidx.slice.compat.SliceProviderCompat.PERMS_PREFIX;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;
import androidx.slice.compat.CompatPermissionManager;
import androidx.slice.render.SliceRenderActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

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
    public void testPinList() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        Uri longerUri = uri.buildUpon().appendPath("something").build();
        try {
            mManager.pinSlice(uri);
            mManager.pinSlice(longerUri);
            verify(mSliceProvider, timeout(2000)).onSlicePinned(eq(longerUri));

            List<Uri> uris = mManager.getPinnedSlices();
            assertEquals(2, uris.size());
            assertTrue(uris.contains(uri));
            assertTrue(uris.contains(longerUri));
        } finally {
            mManager.unpinSlice(uri);
            mManager.unpinSlice(longerUri);
        }
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

        // Disabled while we update APIs.
        //assertEquals(SliceLiveData.SUPPORTED_SPECS, mManager.getPinnedSpecs(uri));
    }

    @Test
    public void testMapIntentToUriStatic() {
        Uri expected = Uri.parse("content://androidx.slice.view.test/render");

        Uri uri = mManager.mapIntentToUri(new Intent(mContext, SliceRenderActivity.class));

        assertEquals(expected, uri);
    }

    @Test
    public void testMapIntentToUri() {
        Uri expected = Uri.parse("content://androidx.slice.view.test/render");
        Intent intent = new Intent("androidx.slice.action.TEST")
                .setPackage(mContext.getPackageName());

        when(mSliceProvider.onMapIntentToUri(eq(intent))).thenReturn(expected);
        Uri uri = mManager.mapIntentToUri(intent);

        verify(mSliceProvider).onMapIntentToUri(eq(intent));
        assertEquals(expected, uri);
    }

    @Test
    public void testGetDescendants() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mContext.getPackageName())
                .build();
        Collection<Uri> collection = Arrays.asList(
                uri,
                uri.buildUpon().appendPath("1").build(),
                uri.buildUpon().appendPath("2").build()
        );
        when(mSliceProvider.onGetSliceDescendants(any(Uri.class)))
                .thenReturn(collection);

        Collection<Uri> allUris = mManager.getSliceDescendants(uri);

        assertEquals(allUris, collection);
        verify(mSliceProvider).onGetSliceDescendants(eq(uri));
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

        protected CompatPermissionManager onCreatePermissionManager(
                String[] autoGrantPermissions) {
            return new CompatPermissionManager(getContext(), PERMS_PREFIX + getClass().getName(),
                    -1 /* Different uid to run permissions */, autoGrantPermissions);
        }

        @Override
        public Collection<Uri> onGetSliceDescendants(Uri uri) {
            if (sSliceProviderReceiver != null) {
                return sSliceProviderReceiver.onGetSliceDescendants(uri);
            }
            return super.onGetSliceDescendants(uri);
        }
    }
}
