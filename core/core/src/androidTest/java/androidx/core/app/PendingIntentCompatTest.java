/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.util.ObjectsCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PendingIntentCompatTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testSend() throws Exception {
        Intent intent = new Intent("test1").addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        PendingIntent pendingIntent = ObjectsCompat.requireNonNull(PendingIntentCompat.getBroadcast(
                mContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false));

        PendingIntent.OnFinished onFinished = mock(PendingIntent.OnFinished.class);
        PendingIntentCompat.send(pendingIntent, 0, onFinished, null);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(onFinished, timeout(2000)).onSendFinished(
                eq(pendingIntent),
                intentCaptor.capture(),
                eq(0),
                nullable(String.class),
                nullable(Bundle.class));
        assertEquals(intent.getAction(), intentCaptor.getValue().getAction());
    }

    @Test
    public void testSend_canceled() {
        PendingIntent pendingIntent = ObjectsCompat.requireNonNull(PendingIntentCompat.getBroadcast(
                mContext,
                0,
                new Intent("test1").addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false));
        pendingIntent.cancel();

        PendingIntent.OnFinished onFinished = mock(PendingIntent.OnFinished.class);
        assertThrows(PendingIntent.CanceledException.class,
                () -> PendingIntentCompat.send(pendingIntent, 99, onFinished, null));

        verify(onFinished, after(500).never()).onSendFinished(
                any(),
                any(),
                anyInt(),
                nullable(String.class),
                nullable(Bundle.class));
    }
}
