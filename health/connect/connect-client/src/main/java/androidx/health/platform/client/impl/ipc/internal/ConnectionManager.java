/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.platform.client.impl.ipc.internal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages connections to a service in a different process.
 *
 */
@RestrictTo(Scope.LIBRARY)
public final class ConnectionManager implements Handler.Callback, ServiceConnection.Callback {
    private static final String TAG = "ConnectionManager";

    @VisibleForTesting
    static final int UNBIND_IDLE_DELAY_MILLISECONDS = 15_000;

    private static final int MSG_CONNECTED = 1;
    private static final int MSG_DISCONNECTED = 2;
    private static final int MSG_EXECUTE = 3;
    private static final int MSG_REGISTER_LISTENER = 4;
    private static final int MSG_UNREGISTER_LISTENER = 5;

    private static final int MSG_UNBIND = 6;

    private final Context mContext;
    private final Handler mHandler;
    private final Map<String, ServiceConnection> mServiceConnectionMap = new HashMap<>();

    private boolean mBindToSelfEnabled;

    public ConnectionManager(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new Handler(looper, this);
    }

    /**
     * Schedules operation for execution
     *
     * @param operation Operation prepared for scheduling on the connection queue.
     */
    public void scheduleForExecution(QueueOperation operation) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_EXECUTE, operation));
    }

    /**
     * Registers a listener by executing an operation represented by the {@link QueueOperation}.
     *
     * @param listenerKey       Key based on which listeners will be distinguished.
     * @param registerOperation Queue operation executed against the corresponding connection to
     *                          register the listener. Will be used to re-register when
     *                          connection is lost.
     */
    public void registerListener(ListenerKey listenerKey, QueueOperation registerOperation) {
        mHandler.sendMessage(
                mHandler.obtainMessage(
                        MSG_REGISTER_LISTENER, new ListenerHolder(listenerKey, registerOperation)));
    }

    /**
     * Unregisters a listener by executing an operation represented by the {@link QueueOperation}.
     *
     * @param listenerKey         Key based on which listeners will be distinguished.
     * @param unregisterOperation Queue operation executed against the corresponding connection to
     *                            unregister the listener.
     */
    public void unregisterListener(ListenerKey listenerKey, QueueOperation unregisterOperation) {
        mHandler.sendMessage(
                mHandler.obtainMessage(
                        MSG_UNREGISTER_LISTENER,
                        new ListenerHolder(listenerKey, unregisterOperation)));
    }

    /**
     * Delays any existing {@code MSG_UNBIND} message to {@code UNBIND_IDLE_DELAY_MILLISECONDS}
     * later.
     *
     * @param serviceConnection Service connection that we will auto-unbind.
     */
    void delayIdleServiceUnbindCheck(@NonNull ServiceConnection serviceConnection) {
        mHandler.removeMessages(MSG_UNBIND, serviceConnection);
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(MSG_UNBIND, serviceConnection),
                UNBIND_IDLE_DELAY_MILLISECONDS);
    }

    @Override
    public void onConnected(ServiceConnection connection) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_CONNECTED, connection));
    }

    @Override
    public void onDisconnected(ServiceConnection connection, long reconnectDelayMs) {
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(MSG_DISCONNECTED, connection), reconnectDelayMs);
    }

    @Override
    public boolean isBindToSelfEnabled() {
        return mBindToSelfEnabled;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_CONNECTED:
                ServiceConnection serviceConnection = ((ServiceConnection) msg.obj);
                serviceConnection.reRegisterAllListeners();
                serviceConnection.refreshServiceVersion();
                serviceConnection.flushQueue();
                delayIdleServiceUnbindCheck(serviceConnection);
                return true;
            case MSG_DISCONNECTED:
                ((ServiceConnection) msg.obj).maybeReconnect();
                return true;
            case MSG_EXECUTE:
                QueueOperation queueOperation = (QueueOperation) msg.obj;
                ServiceConnection serviceConnectionForExecute =
                        getConnection(queueOperation.getConnectionConfiguration());
                serviceConnectionForExecute.enqueue(queueOperation);
                delayIdleServiceUnbindCheck(serviceConnectionForExecute);
                return true;
            case MSG_REGISTER_LISTENER:
                ListenerHolder registerListenerHolder = (ListenerHolder) msg.obj;
                ServiceConnection serviceConnectionForRegister =
                        getConnection(
                                registerListenerHolder
                                        .getListenerOperation()
                                        .getConnectionConfiguration());
                serviceConnectionForRegister
                        .registerListener(
                                registerListenerHolder.getListenerKey(),
                                registerListenerHolder.getListenerOperation());
                delayIdleServiceUnbindCheck(serviceConnectionForRegister);
                return true;
            case MSG_UNREGISTER_LISTENER:
                ListenerHolder unregisterListenerHolder = (ListenerHolder) msg.obj;
                ServiceConnection serviceConnectionForUnregister =
                        getConnection(
                                unregisterListenerHolder
                                        .getListenerOperation()
                                        .getConnectionConfiguration());
                serviceConnectionForUnregister
                        .unregisterListener(
                                unregisterListenerHolder.getListenerKey(),
                                unregisterListenerHolder.getListenerOperation());
                delayIdleServiceUnbindCheck(serviceConnectionForUnregister);
                return true;
            case MSG_UNBIND:
                ServiceConnection serviceConnectionToClear = ((ServiceConnection) msg.obj);
                if (mHandler.hasMessages(MSG_EXECUTE)
                        || mHandler.hasMessages(MSG_REGISTER_LISTENER)
                        || mHandler.hasMessages(MSG_UNREGISTER_LISTENER)) {
                    return true;
                }
                boolean isIdle = serviceConnectionToClear.clearConnectionIfIdle();
                if (!isIdle) {
                    delayIdleServiceUnbindCheck(serviceConnectionToClear);
                }
                return true;
            default:
                Log.e(TAG, "Received unknown message: " + msg.what);
                return false;
        }
    }

    public void setBindToSelf(boolean bindToSelfEnabled) {
        this.mBindToSelfEnabled = bindToSelfEnabled;
    }

    @VisibleForTesting
    ServiceConnection getConnection(ConnectionConfiguration connectionConfiguration) {
        String connectionKey = connectionConfiguration.getKey();
        ServiceConnection serviceConnection = mServiceConnectionMap.get(connectionKey);
        if (serviceConnection == null) {
            serviceConnection =
                    new ServiceConnection(
                            mContext, connectionConfiguration, new DefaultExecutionTracker(), this);
            mServiceConnectionMap.put(connectionKey, serviceConnection);
        }
        return serviceConnection;
    }

    private static class ListenerHolder {
        private final ListenerKey mListenerKey;
        private final QueueOperation mListenerOperation;

        ListenerHolder(ListenerKey listenerKey, QueueOperation listenerOperation) {
            this.mListenerKey = listenerKey;
            this.mListenerOperation = listenerOperation;
        }

        ListenerKey getListenerKey() {
            return mListenerKey;
        }

        QueueOperation getListenerOperation() {
            return mListenerOperation;
        }
    }
}
