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

package androidx.slice.compat;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_BIND_URI;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_SLICE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.SliceSpec;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceProviderCompatTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    @Test
    public void testBindWithPermission() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("my.authority")
                .path("my_path")
                .build();
        Slice s = new Slice.Builder(uri)
                .addText("", null)
                .build();

        SliceProvider provider = spy(new SliceProviderImpl());
        CompatPermissionManager permissions = mock(CompatPermissionManager.class);
        when(permissions.checkSlicePermission(any(Uri.class), anyInt(), anyInt()))
                .thenReturn(PERMISSION_GRANTED);

        when(provider.onBindSlice(eq(uri))).thenReturn(s);
        SliceProviderCompat compat = new SliceProviderCompat(provider, permissions,
                mContext) {
            @Override
            public String getCallingPackage() {
                return mContext.getPackageName();
            }
        };

        Bundle b = new Bundle();
        b.putParcelable(EXTRA_BIND_URI, uri);
        SliceProviderCompat.addSpecs(b, Collections.<SliceSpec>emptySet());

        Bundle result = compat.call(SliceProviderCompat.METHOD_SLICE, null, b);
        assertEquals(s.toString(), new Slice(result.getBundle(EXTRA_SLICE)).toString());
    }

    @Test
    public void testBindWithoutPermission() {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("my.authority")
                .path("my_path")
                .build();
        Slice s = new Slice.Builder(uri)
                .addText("", null)
                .build();

        SliceProvider provider = spy(new SliceProviderImpl());
        CompatPermissionManager permissions = mock(CompatPermissionManager.class);
        when(permissions.checkSlicePermission(any(Uri.class), anyInt(), anyInt()))
                .thenReturn(PERMISSION_DENIED);

        when(provider.onBindSlice(eq(uri))).thenReturn(s);
        SliceProviderCompat compat = new SliceProviderCompat(provider, permissions,
                mContext) {
            @Override
            public String getCallingPackage() {
                return mContext.getPackageName();
            }
        };

        Bundle b = new Bundle();
        b.putParcelable(EXTRA_BIND_URI, uri);
        SliceProviderCompat.addSpecs(b, Collections.<SliceSpec>emptySet());

        Bundle result = compat.call(SliceProviderCompat.METHOD_SLICE, null, b);
        assertNotEquals(s.toString(), new Slice(result.getBundle(EXTRA_SLICE)).toString());
    }

    public static class SliceProviderImpl extends SliceProvider {

        @Override
        public boolean onCreateSliceProvider() {
            return true;
        }

        @Override
        public Slice onBindSlice(Uri sliceUri) {
            return null;
        }
    }
}
