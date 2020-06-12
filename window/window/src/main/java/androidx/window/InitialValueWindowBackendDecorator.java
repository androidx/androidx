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

package androidx.window;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;

/**
 * An implementation of {@link WindowBackend} that will emit the current value whenever a
 * listener is registered. Otherwise it delegates calls to the wrapped {@link WindowBackend}.
 */
final class InitialValueWindowBackendDecorator implements WindowBackend {

    @NonNull
    private final WindowBackend mWindowBackend;

    InitialValueWindowBackendDecorator(@NonNull WindowBackend windowBackend) {
        mWindowBackend = windowBackend;
    }

    @NonNull
    @Override
    public WindowLayoutInfo getWindowLayoutInfo(@NonNull Context context) {
        return mWindowBackend.getWindowLayoutInfo(context);
    }

    @NonNull
    @Override
    public DeviceState getDeviceState() {
        return mWindowBackend.getDeviceState();
    }

    @Override
    public void registerLayoutChangeCallback(@NonNull Context context, @NonNull Executor executor,
            @NonNull Consumer<WindowLayoutInfo> callback) {
        final WindowLayoutInfo info = getWindowLayoutInfo(context);
        executor.execute(() -> {
            callback.accept(info);
        });
        mWindowBackend.registerLayoutChangeCallback(context, executor, callback);
    }

    @Override
    public void unregisterLayoutChangeCallback(@NonNull Consumer<WindowLayoutInfo> callback) {
        mWindowBackend.unregisterLayoutChangeCallback(callback);
    }

    @Override
    public void registerDeviceStateChangeCallback(@NonNull Executor executor,
            @NonNull Consumer<DeviceState> callback) {
        final DeviceState state = getDeviceState();
        executor.execute(() -> {
            callback.accept(state);
        });
        mWindowBackend.registerDeviceStateChangeCallback(executor, callback);
    }

    @Override
    public void unregisterDeviceStateChangeCallback(@NonNull Consumer<DeviceState> callback) {
        mWindowBackend.unregisterDeviceStateChangeCallback(callback);
    }
}
