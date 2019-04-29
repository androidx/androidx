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

import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.MediaBrowserServiceCompat.Result;
import androidx.media2.session.MediaBrowser;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.SessionToken;
import androidx.media2.test.service.MediaTestUtils;
import androidx.media2.test.service.MockMediaBrowserServiceCompat;
import androidx.media2.test.service.MockMediaBrowserServiceCompat.Proxy;
import androidx.media2.test.service.RemoteMediaBrowser;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaBrowser} with {@link MediaBrowserServiceCompat}.
 */
@LargeTest
public class MediaBrowserServiceCompatCallbackTestWithMediaBrowser extends MediaSessionTestBase {
    private SessionToken mToken;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mToken = new SessionToken(mContext, new ComponentName(
                mContext, MockMediaBrowserServiceCompat.class));
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void testOnGetRootCalledByGetLibraryRoot() throws InterruptedException {
        prepareLooper();
        final String testMediaId = "testOnGetRootCalledByGetLibraryRoot";
        final Bundle testExtras = new Bundle();
        testExtras.putString(testMediaId, testMediaId);
        final LibraryParams testParams = new LibraryParams.Builder()
                .setSuggested(true).setExtras(testExtras).build();

        final CountDownLatch latch = new CountDownLatch(1);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public BrowserRoot onGetRoot(String clientPackageName, int clientUid,
                    Bundle rootHints) {
                assertEquals(CLIENT_PACKAGE_NAME, clientPackageName);
                if (rootHints.keySet().contains(testMediaId)) {
                    MediaTestUtils.assertEqualLibraryParams(testParams, rootHints);
                    // This should happen because getLibraryRoot() is called with testExtras.
                    latch.countDown();
                }
                // For other random connection requests.
                return new BrowserRoot("rootId", null);
            }
        });

        RemoteMediaBrowser browser = new RemoteMediaBrowser(mContext, mToken, true, null);
        browser.getLibraryRoot(testParams);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnLoadItemCalledByGetItem() throws InterruptedException {
        prepareLooper();
        final String testMediaId = "test_media_item";
        final MediaItem testItem = createMediaItem(testMediaId);
        final CountDownLatch latch = new CountDownLatch(1);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadItem(String itemId, Result<MediaItem> result) {
                assertEquals(testMediaId, itemId);
                result.sendResult(testItem);
                latch.countDown();
            }
        });

        RemoteMediaBrowser browser = new RemoteMediaBrowser(mContext, mToken, true, null);
        browser.getItem(testMediaId);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnLoadChildrenWithoutOptionsCalledByGetChildren() throws InterruptedException {
        prepareLooper();
        final String testParentId = "test_media_parent";
        final int testPage = 2;
        final int testPageSize = 4;
        final List<MediaItem> testFullMediaItemList = createMediaItems(
                (testPage + 1) * testPageSize);
        final CountDownLatch latch = new CountDownLatch(1);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
                assertEquals(testParentId, parentId);
                result.sendResult(testFullMediaItemList);
                latch.countDown();
            }
        });
        RemoteMediaBrowser browser = new RemoteMediaBrowser(mContext, mToken, true, null);
        browser.getChildren(testParentId, testPage, testPageSize, null);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnLoadChildrenWithOptionsCalledByGetChildren() throws InterruptedException {
        prepareLooper();
        final String testParentId = "test_media_parent";
        final int testPage = 2;
        final int testPageSize = 4;
        final List<MediaItem> testMediaItemList = createMediaItems(testPageSize);
        final CountDownLatch latch = new CountDownLatch(1);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
                fail("This isn't expected to be called");
            }

            @Override
            public void onLoadChildren(String parentId, Result<List<MediaItem>> result,
                    Bundle options) {
                assertEquals(testParentId, parentId);
                assertEquals(testPage, options.getInt(MediaBrowserCompat.EXTRA_PAGE));
                assertEquals(testPageSize, options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                assertEquals(2, options.keySet().size());
                result.sendResult(testMediaItemList);
                latch.countDown();
            }
        });
        RemoteMediaBrowser browser = new RemoteMediaBrowser(mContext, mToken, true, null);
        browser.getChildren(testParentId, testPage, testPageSize, null);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnLoadChildrenCalledBySubscribe() throws InterruptedException {
        prepareLooper();
        final String testParentId = "testOnLoadChildrenCalledBySubscribe";
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onLoadChildren(String parentId, Result<List<MediaItem>> result,
                    Bundle option) {
                assertEquals(testParentId, parentId);
                MediaTestUtils.assertEqualLibraryParams(testParams, option);
                result.sendResult(null);
                subscribeLatch.countDown();
            }
        });
        RemoteMediaBrowser browser = new RemoteMediaBrowser(mContext, mToken, true, null);
        browser.subscribe(testParentId, testParams);
        assertTrue(subscribeLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnSearchCalledBySearch() throws InterruptedException {
        prepareLooper();
        final String testQuery = "search_query";
        final int testPage = 2;
        final int testPageSize = 4;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();
        final List<MediaItem> testFullSearchResult = createMediaItems(
                (testPage + 1) * testPageSize + 3);

        final CountDownLatch latch = new CountDownLatch(1);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result) {
                assertEquals(testQuery, query);
                MediaTestUtils.assertEqualLibraryParams(testParams, extras);
                result.sendResult(testFullSearchResult);
                latch.countDown();
            }
        });

        RemoteMediaBrowser browser = new RemoteMediaBrowser(mContext, mToken, true, null);
        browser.search(testQuery, testParams);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnSearchCalledByGetSearchResult() throws InterruptedException {
        prepareLooper();
        final String testQuery = "search_query";
        final int testPage = 2;
        final int testPageSize = 4;
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
            @Override
            public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result) {
                assertEquals(testQuery, query);
                MediaTestUtils.assertEqualLibraryParams(testParams, extras);
                assertEquals(testPage, extras.getInt(MediaBrowserCompat.EXTRA_PAGE));
                assertEquals(testPageSize, extras.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE));
                result.sendResult(null);
                latch.countDown();
            }
        });

        RemoteMediaBrowser browser = new RemoteMediaBrowser(mContext, mToken, true, null);
        browser.getSearchResult(testQuery, testPage, testPageSize, testParams);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private static MediaItem createMediaItem(String mediaId) {
        final MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId).setTitle("title: " + mediaId).build();
        return new MediaItem(desc, MediaItem.FLAG_PLAYABLE);
    }

    private static List<MediaItem> createMediaItems(int size) {
        final List<MediaItem> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[2].getMethodName();
        for (int i = 0; i < size; i++) {
            list.add(createMediaItem(caller + "_child_" + i));
        }
        return list;
    }
}
