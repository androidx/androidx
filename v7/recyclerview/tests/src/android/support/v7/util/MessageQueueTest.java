/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.util;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class MessageQueueTest {
    MessageThreadUtil.MessageQueue mQueue;

    @Before
    public void setUp() throws Exception {
        mQueue = new MessageThreadUtil.MessageQueue();
    }

    @Test
    public void testAllArguments() {
        String data = "data";
        mQueue.sendMessage(MessageThreadUtil.SyncQueueItem.obtainMessage(
                0, 1, 2, 3, 4, 5, data));

        MessageThreadUtil.SyncQueueItem msg = mQueue.next();
        assertThat(msg.what, is(0));
        assertThat(msg.arg1, is(1));
        assertThat(msg.arg2, is(2));
        assertThat(msg.arg3, is(3));
        assertThat(msg.arg4, is(4));
        assertThat(msg.arg5, is(5));
        assertThat((String)msg.data, sameInstance(data));
    }

    @Test
    public void testSendInOrder() {
        mQueue.sendMessage(obtainMessage(1, 2));
        mQueue.sendMessage(obtainMessage(3, 4));
        mQueue.sendMessage(obtainMessage(5, 6));

        MessageThreadUtil.SyncQueueItem msg = mQueue.next();
        assertThat(msg.what, is(1));
        assertThat(msg.arg1, is(2));

        msg = mQueue.next();
        assertThat(msg.what, is(3));
        assertThat(msg.arg1, is(4));

        msg = mQueue.next();
        assertThat(msg.what, is(5));
        assertThat(msg.arg1, is(6));

        msg = mQueue.next();
        assertThat(msg, nullValue());
    }

    @Test
    public void testSendAtFront() {
        mQueue.sendMessage(obtainMessage(1, 2));
        mQueue.sendMessageAtFrontOfQueue(obtainMessage(3, 4));
        mQueue.sendMessage(obtainMessage(5, 6));

        MessageThreadUtil.SyncQueueItem msg = mQueue.next();
        assertThat(msg.what, is(3));
        assertThat(msg.arg1, is(4));

        msg = mQueue.next();
        assertThat(msg.what, is(1));
        assertThat(msg.arg1, is(2));

        msg = mQueue.next();
        assertThat(msg.what, is(5));
        assertThat(msg.arg1, is(6));

        msg = mQueue.next();
        assertThat(msg, nullValue());
    }

    @Test
    public void testRemove() {
        mQueue.sendMessage(obtainMessage(1, 0));
        mQueue.sendMessage(obtainMessage(2, 0));
        mQueue.sendMessage(obtainMessage(1, 0));
        mQueue.sendMessage(obtainMessage(2, 1));
        mQueue.sendMessage(obtainMessage(3, 0));
        mQueue.sendMessage(obtainMessage(1, 0));

        mQueue.removeMessages(1);

        MessageThreadUtil.SyncQueueItem msg = mQueue.next();
        assertThat(msg.what, is(2));
        assertThat(msg.arg1, is(0));

        msg = mQueue.next();
        assertThat(msg.what, is(2));
        assertThat(msg.arg1, is(1));

        msg = mQueue.next();
        assertThat(msg.what, is(3));
        assertThat(msg.arg1, is(0));

        msg = mQueue.next();
        assertThat(msg, nullValue());
    }

    private MessageThreadUtil.SyncQueueItem obtainMessage(int what, int arg) {
        return MessageThreadUtil.SyncQueueItem.obtainMessage(what, arg, null);
    }
}
