/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.os.Handler;
import android.os.Message;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class MainThreadAsyncHandlerTest {

    @Test
    public void canPostTaskToMainLooper() {
        Handler handler = MainThreadAsyncHandler.getInstance();
        final AtomicBoolean didRun = new AtomicBoolean(false);

        ShadowLooper.pauseMainLooper();
        handler.post(() -> didRun.set(true));

        boolean ranBeforeTrigger = didRun.get();
        ShadowLooper.runMainLooperOneTask();
        boolean ranAfterTrigger = didRun.get();

        assertThat(ranBeforeTrigger).isFalse();
        assertThat(ranAfterTrigger).isTrue();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.LOLLIPOP_MR1) // Message#isAsynchronous() added in API 22
    public void sentMessageIsAsynchronous() {
        Message message = Message.obtain();
        boolean isAsyncBeforeSending = message.isAsynchronous();

        MainThreadAsyncHandler.getInstance().sendMessage(message);

        boolean isAsyncAfterSending = message.isAsynchronous();

        assertThat(isAsyncBeforeSending).isFalse();
        assertThat(isAsyncAfterSending).isTrue();
    }
}
