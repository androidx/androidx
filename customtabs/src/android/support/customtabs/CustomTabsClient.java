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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.List;

/**
 * Class to communicate with a {@link CustomTabsService} and create
 * {@link CustomTabsSession} from it.
 */
public class CustomTabsClient {
    private final ICustomTabsService mService;
    private final ComponentName mServiceComponentName;

    /**@hide*/
    CustomTabsClient(ICustomTabsService service, ComponentName componentName) {
        mService = service;
        mServiceComponentName = componentName;
    }

    /**
     * Bind to a {@link CustomTabsService} using the given package name and
     * {@link ServiceConnection}.
     * @param context     {@link Context} to use while calling
     *                    {@link Context#bindService(Intent, ServiceConnection, int)}
     * @param packageName Package name to set on the {@link Intent} for binding.
     * @param connection  {@link CustomTabsServiceConnection} to use when binding. This will
     *                    return a {@link CustomTabsClient} on
     *                    {@link CustomTabsServiceConnection
     *                    #onCustomTabsServiceConnected(ComponentName, CustomTabsClient)}
     * @return Whether the binding was successful.
     */
    public static boolean bindCustomTabsService(Context context,
            String packageName, CustomTabsServiceConnection connection) {
        Intent intent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        if (!TextUtils.isEmpty(packageName)) intent.setPackage(packageName);
        return context.bindService(intent, connection,
                Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
    }

    /**
     * Warm up the browser process.
     * @param flags Reserved for future use.
     * @return      Whether the warmup was successful.
     */
    public boolean warmup(long flags) {
        try {
            return mService.warmup(flags);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Creates a new session through an ICustomTabsService with the optional callback. This session
     * can be used to associate any related communication through the service with an intent and
     * then later with a Custom Tab. The client can then send later service calls or intents to
     * through same session-intent-Custom Tab association.
     * @param callback The callback through which the client will receive updates about the created
     *                 session. Can be null.
     * @return The session object that was created as a result of the transaction. The client can
     *         use this to relay {@link CustomTabsSession#mayLaunchUrl(Uri, Bundle, List)} calls.
     *         Null on error.
     */
    public CustomTabsSession newSession(final CustomTabsCallback callback) {
        ICustomTabsCallback.Stub wrapper = new ICustomTabsCallback.Stub() {
            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
                if (callback != null) callback.onNavigationEvent(navigationEvent, extras);
            }

            @Override
            public void extraCallback(String callbackName, Bundle args) throws RemoteException {
                if (callback != null) callback.extraCallback(callbackName, args);
            }
        };

        try {
            if (!mService.newSession(wrapper)) return null;
        } catch (RemoteException e) {
            return null;
        }
        return new CustomTabsSession(mService, wrapper, mServiceComponentName);
    }

    public Bundle extraCommand(String commandName, Bundle args) {
        try {
            return mService.extraCommand(commandName, args);
        } catch (RemoteException e) {
            return null;
        }
    }
}
