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

import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaSessionCompat}.
 */
public class MediaSessionCompatTest {
    private static final int TIMEOUT_MS = 1000;

    @Test
    @SmallTest
    public void testConstructor_withoutLooper() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                MediaSessionCompat session = new MediaSessionCompat(context, "testConstructor");
                session.setActive(true);
                session.release();
                latch.countDown();
            }
        }.run();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
