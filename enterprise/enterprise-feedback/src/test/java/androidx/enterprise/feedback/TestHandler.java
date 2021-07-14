/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.enterprise.feedback;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;

import org.robolectric.annotation.internal.DoNotInstrument;

/** Handler which stores the most recently handled message in a public field. */
@DoNotInstrument
@SuppressWarnings("deprecation")
class TestHandler extends Handler {

    @Nullable
    private Message mLatestMessage;
    private int mMessageCount = 0;

    @Override
    public void handleMessage(Message message) {
        // The message is emptied after handleMessage so we copy it for testing.
        this.mLatestMessage = Message.obtain();
        this.mLatestMessage.copyFrom(message);
        mMessageCount += 1;
    }

    @Nullable
    public Message latestMessage() {
        return mLatestMessage;
    }

    public int messageCount() {
        return mMessageCount;
    }

    public void reset() {
        mLatestMessage = null;
        mMessageCount = 0;
    }
}
