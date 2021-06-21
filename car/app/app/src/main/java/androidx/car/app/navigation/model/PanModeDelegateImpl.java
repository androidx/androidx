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

package androidx.car.app.navigation.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.RemoteException;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.utils.RemoteUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation class for {@link PanModeDelegate}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
@CarProtocol
public class PanModeDelegateImpl implements PanModeDelegate {
    @Keep
    @Nullable
    private final IPanModeListener mStub;

    @Override
    public void sendPanModeChanged(boolean isInPanMode, @NonNull @NotNull OnDoneCallback callback) {
        try {
            requireNonNull(mStub).onPanModeChanged(isInPanMode,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private PanModeDelegateImpl(@NonNull PanModeListener listener) {
        mStub = new PanModeListenerStub(listener);
    }

    /** For serialization. */
    private PanModeDelegateImpl() {
        mStub = null;
    }

    @NonNull
    // This listener relates to UI event and is expected to be triggered on the main thread.
    @SuppressLint("ExecutorRegistration")
    static PanModeDelegate create(@NonNull PanModeListener listener) {
        return new PanModeDelegateImpl(listener);
    }

    @Keep // We need to keep these stub for Bundler serialization logic.
    private static class PanModeListenerStub extends IPanModeListener.Stub {
        private final PanModeListener mListener;

        PanModeListenerStub(PanModeListener listener) {
            mListener = listener;
        }

        @Override
        public void onPanModeChanged(boolean isInPanMode, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(callback, "onPanModeChanged", () -> {
                        mListener.onPanModeChanged(isInPanMode);
                        return null;
                    }
            );
        }
    }
}
