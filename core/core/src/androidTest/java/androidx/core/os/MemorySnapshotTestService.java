/*
 * Copyright 2026 The Android Open Source Project
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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A remote service running in a separate process ({@code :secondary}) used for testing
 * {@link MemorySnapshot#capture(int)} across processes in a multi-process application.
 */
public class MemorySnapshotTestService extends Service {

    public static final int MSG_GET_PID = 1;
    public static final int MSG_PID_REPLY = 2;

    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private static class IncomingHandler extends Handler {
        IncomingHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_GET_PID && msg.replyTo != null) {
                Message reply = Message.obtain(null, MSG_PID_REPLY, Process.myPid(), 0);
                try {
                    msg.replyTo.send(reply);
                } catch (RemoteException ignored) {
                }
            } else {
                super.handleMessage(msg);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
