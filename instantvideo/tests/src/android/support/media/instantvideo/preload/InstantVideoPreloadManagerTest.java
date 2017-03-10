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
package android.support.media.instantvideo.preload;

import static android.support.test.InstrumentationRegistry.getContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.support.media.instantvideo.preload.InstantVideoPreloadManager.VideoPreloader;
import android.support.media.instantvideo.preload.InstantVideoPreloadManager.VideoPreloaderFactory;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for IntentVideoPreloadManager.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class InstantVideoPreloadManagerTest {
    private static final Uri PRELOAD_VIDEO_URI_1 = Uri.parse("http://test/test1.mp4");
    private static final Uri PRELOAD_VIDEO_URI_2 = Uri.parse("http://test/test2.mp4");

    private InstantVideoPreloadManager mPreloadManager;
    @Mock
    private VideoPreloaderFactory mMockVideoPreloaderFactory;
    @Mock
    private VideoPreloader mMockVideoPreloader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mMockVideoPreloaderFactory.createVideoPreloader(any(Uri.class)))
                .thenReturn(mMockVideoPreloader);
        mPreloadManager = new InstantVideoPreloadManager(getContext(), mMockVideoPreloaderFactory);
    }

    @After
    public void tearDown() {
        mPreloadManager.clearCache();
    }

    @Test
    public void preload() {
        mPreloadManager.preload(PRELOAD_VIDEO_URI_1);
        assertCacheSize(1);
        mPreloadManager.preload(PRELOAD_VIDEO_URI_2);
        assertCacheSize(2);
    }

    @Test
    public void preload_duplicate() {
        mPreloadManager.preload(PRELOAD_VIDEO_URI_1);
        mPreloadManager.preload(PRELOAD_VIDEO_URI_1);
        assertCacheSize(1);
    }

    @Test
    public void setMaxPreloadVideoCount() {
        mPreloadManager.setMaxPreloadVideoCount(1);
        mPreloadManager.preload(PRELOAD_VIDEO_URI_1);
        assertCacheSize(1);
        mPreloadManager.preload(PRELOAD_VIDEO_URI_2);
        assertCacheSize(1);
    }

    @Test
    public void clearCache() {
        mPreloadManager.preload(PRELOAD_VIDEO_URI_1);
        mPreloadManager.preload(PRELOAD_VIDEO_URI_2);
        mPreloadManager.clearCache();
        assertCacheSize(0);
    }

    private void assertCacheSize(int expected) {
        int cacheSize = mPreloadManager.getCacheSize();
        assertEquals("The cache size should be " + expected + ", but was " + cacheSize, expected,
                cacheSize);
    }
}
