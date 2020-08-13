/*
 * Copyright 2020 The Android Open Source Project
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
package android.support.mediacompat.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link MediaSessionCompat}.
 */
public class MediaSessionCompatTest {
    private static final int TIMEOUT_MS = 1000;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    @SmallTest
    public void testConstructor_withoutLooper() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                MediaSessionCompat session = new MediaSessionCompat(mContext, "testConstructor");
                session.setActive(true);
                session.release();
                latch.countDown();
            }
        }.start();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @SmallTest
    public void testSetQueue_withNullItem_throwsIAE() {
        List<QueueItem> queue = new ArrayList<>();
        queue.add(createQueueItemWithId(0));
        queue.add(createQueueItemWithId(1));
        queue.add(null);
        queue.add(createQueueItemWithId(2));

        MediaSessionCompat session = new MediaSessionCompat(mContext, "testSetQueue");
        try {
            session.setQueue(queue);
            fail("setQueue should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private QueueItem createQueueItemWithId(long id) {
        return new QueueItem(
                new MediaDescriptionCompat.Builder().setMediaId("item" + id).build(), id);
    }
}
