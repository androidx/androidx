/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;

import androidx.car.app.HandshakeInfo;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.versioning.CarAppApiLevels;

/**
 * Use delegate to forward events to a mock. Mockito interceptor is not maintained on top-level
 * IBinder after call to IRenderService.Stub.asInterface() in CarAppActivity.
 */
public class RenderServiceDelegate extends IRendererService.Stub {
    private final IRendererService mService;
    private ICarAppActivity mCarAppActivity;

    RenderServiceDelegate(IRendererService service) {
        mService = service;
    }

    @Override
    public boolean initialize(ICarAppActivity carActivity, ComponentName serviceName,
            int displayId) throws RemoteException {
        mCarAppActivity = carActivity;
        return mService.initialize(carActivity, serviceName, displayId);
    }

    @Override
    public boolean onNewIntent(Intent intent, ComponentName serviceName, int displayId)
            throws RemoteException {
        return mService.onNewIntent(intent, serviceName, displayId);
    }

    @Override
    public void terminate(ComponentName serviceName) throws RemoteException {
        mService.terminate(serviceName);
    }

    @Override
    public Bundleable performHandshake(ComponentName serviceName, int appLatestApiLevel)
            throws RemoteException {
        mService.performHandshake(serviceName, appLatestApiLevel);
        try {
            return Bundleable.create(new HandshakeInfo("", CarAppApiLevels.getLatest()));
        } catch (BundlerException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns the {@link ICarAppActivity} received in {@link #initialize}. */
    public ICarAppActivity getCarAppActivity() {
        return mCarAppActivity;
    }
}
