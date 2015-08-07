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

package android.support.customtabs;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Wrapper class that can be used as a unique identifier for a session. Also contains an accessor
 * for the {@link CustomTabsCallback} for the session if there was any.
 */
public class CustomTabsSessionToken {
    private static final String TAG = "CustomTabsSessionToken";
    private final ICustomTabsCallback mCallbackBinder;
    private final CustomTabsCallback mCallback;

    /**@hide*/
    CustomTabsSessionToken(ICustomTabsCallback callbackBinder) {
        mCallbackBinder = callbackBinder;
        mCallback = new CustomTabsCallback() {

            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
                try {
                    mCallbackBinder.onNavigationEvent(navigationEvent, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }
        };
    }

    /**@hide*/
    IBinder getCallbackBinder() {
        return mCallbackBinder.asBinder();
    }

    @Override
    public int hashCode() {
        return getCallbackBinder().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomTabsSessionToken)) return false;
        CustomTabsSessionToken token = (CustomTabsSessionToken) o;
        return token.getCallbackBinder().equals(mCallbackBinder.asBinder());
    }

    /**
     * @return {@link CustomTabsCallback} corresponding to this session if there was any non-null
     *         callbacks passed by the client.
     */
    public CustomTabsCallback getCallback() {
        return mCallback;
    }
}