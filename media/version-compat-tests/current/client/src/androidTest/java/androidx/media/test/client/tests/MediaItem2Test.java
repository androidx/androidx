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

package androidx.media.test.client.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;

import androidx.media.test.lib.TestUtils;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaUtils2;
import androidx.media2.UriMediaItem2;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link MediaItem2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaItem2Test {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testSubclass_sameProcess() {
        final UriMediaItem2 testUriItem = createUriMediaItem2();
        final ParcelImpl parcel = MediaUtils2.toParcelable(testUriItem);

        final MediaItem2 testRemoteItem = MediaUtils2.fromParcelable(parcel);
        assertEquals(testUriItem, testRemoteItem);
    }

    @Test
    public void testSubclass_acrossProcessWithMediaUtils() {
        final UriMediaItem2 testUriItem = createUriMediaItem2();

        // Mocks the binder call across the processes by using writeParcelable/readParcelable
        // which only happens between processes. Code snippets are copied from
        // VersionedParcelIntegTest#parcelCopy.
        final Parcel p = Parcel.obtain();
        p.writeParcelable(MediaUtils2.toParcelable(testUriItem), 0);
        p.setDataPosition(0);
        final MediaItem2 testRemoteItem = MediaUtils2.fromParcelable(
                (ParcelImpl) p.readParcelable(MediaItem2.class.getClassLoader()));

        assertFalse(testRemoteItem instanceof UriMediaItem2);
        assertEquals(testUriItem.getStartPosition(), testRemoteItem.getStartPosition());
        assertEquals(testUriItem.getEndPosition(), testRemoteItem.getEndPosition());
        TestUtils.equals(testUriItem.getMetadata().toBundle(),
                testRemoteItem.getMetadata().toBundle());
    }

    @Test
    public void testSubclass_acrossProcessWithParcelUtils() {
        final UriMediaItem2 testUriItem = createUriMediaItem2();

        // Mocks the binder call across the processes by using writeParcelable/readParcelable
        // which only happens between processes. Code snippets are copied from
        // VersionedParcelIntegTest#parcelCopy.
        try {
            final Parcel p = Parcel.obtain();
            p.writeParcelable(ParcelUtils.toParcelable(testUriItem), 0);
            p.setDataPosition(0);
            final MediaItem2 testRemoteItem = ParcelUtils.fromParcelable(
                    (ParcelImpl) p.readParcelable(MediaItem2.class.getClassLoader()));
            fail("Write to parcel should fail for subclass of MediaItem2");
        } catch (Exception e) {
        }
    }

    private UriMediaItem2 createUriMediaItem2() {
        final MediaMetadata2 testMetadata = new MediaMetadata2.Builder()
                .putString("MediaItem2Test", "MediaItem2Test").build();
        return new UriMediaItem2.Builder(mContext, Uri.parse("test://test"))
                        .setMetadata(testMetadata)
                        .setStartPosition(1)
                        .setEndPosition(1000)
                        .build();
    }
}
