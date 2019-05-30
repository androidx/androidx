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

package androidx.core.os;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Message;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public final class MessageCompatTest {
    @Test
    @SdkSuppress(minSdkVersion = 16)
    public void async() {
        Message message = Message.obtain();
        assertFalse(MessageCompat.isAsynchronous(message));
        MessageCompat.setAsynchronous(message, true);
        assertTrue(MessageCompat.isAsynchronous(message));
        message.recycle();
    }

    @Test
    @SdkSuppress(maxSdkVersion = 15)
    public void asyncPreApi16() {
        Message message = Message.obtain();
        MessageCompat.setAsynchronous(message, true);
        assertFalse(MessageCompat.isAsynchronous(message));
        message.recycle();
    }
}
