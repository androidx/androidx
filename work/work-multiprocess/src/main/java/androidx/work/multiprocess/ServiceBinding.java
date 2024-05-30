/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.work.multiprocess;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Function;
import androidx.work.Logger;
import androidx.work.impl.utils.futures.SettableFuture;

class ServiceBinding {

    private ServiceBinding() {
    }

    static <T extends IInterface> Session<T> bindToService(@NonNull Context context,
            @NonNull Intent intent,
            @NonNull Function<IBinder, T> asInterface,
            @NonNull String loggingTag) {
        Logger.get().debug(loggingTag, "Binding via " + intent);
        Session<T> session = new Session<>(loggingTag, asInterface);
        try {
            boolean bound = context.bindService(intent, session, BIND_AUTO_CREATE);
            if (!bound) {
                session.resolveClosedConnection(new RuntimeException("Unable to bind to service"));
            }
        } catch (Throwable throwable) {
            session.resolveClosedConnection(throwable);
        }
        return session;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static class Session<T extends IInterface> implements ServiceConnection {
        private final String mLogTag;
        final SettableFuture<T> mConnectedFuture;
        final SettableFuture<T> mDisconnectedFuture;
        final Function<IBinder, T> mAsInterface;

        public Session(String loggingTag, @NonNull Function<IBinder, T> asInterface) {
            mAsInterface = asInterface;
            mConnectedFuture = SettableFuture.create();
            mDisconnectedFuture = SettableFuture.create();
            mLogTag = loggingTag;
        }

        @Override
        public void onServiceConnected(
                @NonNull ComponentName componentName,
                @NonNull IBinder iBinder) {
            mConnectedFuture.set(mAsInterface.apply(iBinder));
        }

        @Override
        public void onServiceDisconnected(@NonNull ComponentName componentName) {
            Logger.get().debug(mLogTag, "Service disconnected");
            resolveClosedConnection(new RuntimeException("Service disconnected"));
        }

        @Override
        public void onBindingDied(@NonNull ComponentName name) {
            onBindingDied();
        }

        /**
         * Clean-up client when a binding dies.
         */
        public void onBindingDied() {
            Logger.get().debug(mLogTag, "Binding died");
            resolveClosedConnection(new RuntimeException("Binding died"));
        }

        @Override
        public void onNullBinding(@NonNull ComponentName name) {
            Logger.get().error(mLogTag, "Unable to bind to service");
            resolveClosedConnection(new RuntimeException("Cannot bind to service " + name));
        }

        private void resolveClosedConnection(Throwable throwable) {
            // finishing connected future, in case onServiceDisconnected hasn't been
            // called.
            mConnectedFuture.setException(throwable);
            mDisconnectedFuture.set(null);
        }
    }
}
