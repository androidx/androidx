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

package androidx.media2.test.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.media2.common.CallbackMediaItem;
import androidx.media2.common.DataSourceCallback;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.common.UriMediaItem;
import androidx.media2.test.service.MediaTestUtils;
import androidx.media2.test.service.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests {@link MediaItem} and its subclasses.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(Parameterized.class)
@SmallTest
public class MediaItemTest {
    private final MediaItemFactory mItemFactory;
    private final Class<?> mItemBuilderClass;
    private Context mContext;
    private MediaItem mTestItem;

    private static final MediaItemFactory sMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            final MediaMetadata testMetadata = new MediaMetadata.Builder()
                    .putLong("MediaItemTest", 1).build();
            return new MediaItem.Builder()
                    .setMetadata(testMetadata)
                    .setStartPosition(1)
                    .setEndPosition(10)
                    .build();
        }
    };

    private static final MediaItemFactory sUriMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            final MediaMetadata testMetadata = new MediaMetadata.Builder()
                    .putString("MediaItemTest", "MediaItemTest").build();
            return new UriMediaItem.Builder(Uri.parse("test://test"))
                    .setMetadata(testMetadata)
                    .setStartPosition(1)
                    .setEndPosition(1000)
                    .build();
        }
    };

    private static final MediaItemFactory sCallbackMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            final MediaMetadata testMetadata = new MediaMetadata.Builder()
                    .putText("MediaItemTest", "testtest").build();
            final DataSourceCallback callback = new DataSourceCallback() {
                @Override
                public int readAt(long position, @NonNull byte[] buffer, int offset, int size)
                        throws IOException {
                    return 0;
                }

                @Override
                public long getSize() throws IOException {
                    return 0;
                }

                @Override
                public void close() throws IOException {
                    // no-op
                }
            };
            return new CallbackMediaItem.Builder(callback)
                    .setMetadata(testMetadata)
                    .setStartPosition(0)
                    .setEndPosition(0)
                    .build();
        }
    };

    private static final MediaItemFactory sFileMediaItemFactory = new MediaItemFactory() {
        @Override
        public MediaItem create(Context context) {
            int resId = R.raw.midi8sec;
            try (AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId)) {
                return new FileMediaItem.Builder(
                        ParcelFileDescriptor.dup(afd.getFileDescriptor())).build();
            } catch (Exception e) {
                return null;
            }
        }
    };

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {sMediaItemFactory, MediaItem.Builder.class},
                {sUriMediaItemFactory, UriMediaItem.Builder.class},
                {sCallbackMediaItemFactory, CallbackMediaItem.Builder.class},
                {sFileMediaItemFactory, FileMediaItem.Builder.class}});
    }

    public MediaItemTest(MediaItemFactory factory, Class<?> builderClass) {
        mItemFactory = factory;
        mItemBuilderClass = builderClass;
    }

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTestItem = mItemFactory.create(mContext);
    }

    @Test
    public void testSubclass_sameProcess() {
        final ParcelImpl parcel = MediaParcelUtils.toParcelable(mTestItem);

        final MediaItem testRemoteItem = MediaParcelUtils.fromParcelable(parcel);
        assertEquals(mTestItem, testRemoteItem);
    }

    @Test
    public void testSubclass_acrossProcessWithMediaUtils() {
        // Mocks the binder call across the processes by using writeParcelable/readParcelable
        // which only happens between processes. Code snippets are copied from
        // VersionedParcelIntegTest#parcelCopy.
        final Parcel p = Parcel.obtain();
        p.writeParcelable(MediaParcelUtils.toParcelable(mTestItem), 0);
        p.setDataPosition(0);
        final MediaItem testRemoteItem = MediaParcelUtils.fromParcelable(
                (ParcelImpl) p.readParcelable(MediaItem.class.getClassLoader()));

        assertEquals(MediaItem.class, testRemoteItem.getClass());
        assertEquals(mTestItem.getStartPosition(), testRemoteItem.getStartPosition());
        assertEquals(mTestItem.getEndPosition(), testRemoteItem.getEndPosition());
        MediaTestUtils.assertMediaMetadataEquals(
                mTestItem.getMetadata(), testRemoteItem.getMetadata());
    }

    @Test
    public void testSubclass_acrossProcessWithParcelUtils() {
        if (mTestItem.getClass() == MediaItem.class) {
            return;
        }
        try {
            // Mocks the binder call across the processes by using writeParcelable/readParcelable
            // which only happens between processes. Code snippets are copied from
            // VersionedParcelIntegTest#parcelCopy.
            final Parcel p = Parcel.obtain();
            p.writeParcelable(ParcelUtils.toParcelable(mTestItem), 0);
            fail("Write to parcel should throw RuntimeException for subclass of MediaItem");
        } catch (RuntimeException e) {
            // Expected.
        }
    }

    /**
     * Tests whether the methods in MediaItem.Builder have been hidden in subclasses by overriding
     * them all.
     */
    @Test
    public void testSubclass_overriddenAllMethods() throws Exception {
        Method[] mediaItemBuilderMethods = MediaItem.Builder.class.getDeclaredMethods();
        for (int i = 0; i < mediaItemBuilderMethods.length; i++) {
            Method mediaItemBuilderMethod = mediaItemBuilderMethods[i];
            if (!Modifier.isPublic(mediaItemBuilderMethod.getModifiers())) {
                continue;
            }
            Method subclassMethod = mItemBuilderClass.getMethod(
                    mediaItemBuilderMethod.getName(), mediaItemBuilderMethod.getParameterTypes());
            assertEquals(subclassMethod.getDeclaringClass(), mItemBuilderClass);
        }
    }

    interface MediaItemFactory {
        MediaItem create(Context context);
    }
}
