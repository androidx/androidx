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

package androidx.remotecallback;

import static androidx.remotecallback.ProviderRelayReceiver.ACTION_PROVIDER_RELAY;
import static androidx.remotecallback.RemoteCallback.EXTRA_METHOD;
import static androidx.remotecallback.RemoteCallback.TYPE_PROVIDER;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.Bundle;

/**
 * Version of ContentProvider that can be used as a {@link CallbackReceiver}.
 *
 * Be sure to call the super of {@link #call} when unhandled to ensure
 * callbacks are triggered.
 *
 * @param <T> Should be specified as the root class (e.g. class X extends
 *           ContentProviderWithCallbacks\<X>)
 */
public abstract class ContentProviderWithCallbacks<T extends ContentProviderWithCallbacks> extends
        ContentProvider implements CallbackReceiver<T> {

    String mAuthority;
    Context mContext;

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        mAuthority = info.authority;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (ProviderRelayReceiver.METHOD_PROVIDER_CALLBACK.equals(method)) {
            CallbackHandlerRegistry.sInstance.ensureInitialized(getClass());
            CallbackHandlerRegistry.sInstance.invokeCallback(getContext(), this, extras);
            return null;
        }
        return super.call(method, arg, extras);
    }

    @Override
    public T createRemoteCallback(Context context) {
        CallbackHandlerRegistry.sInstance.ensureInitialized(getClass());
        return CallbackHandlerRegistry.sInstance.getAndResetStub(getClass(), context, mAuthority);
    }

    @Override
    public RemoteCallback toRemoteCallback(Class<T> cls, Bundle args, String method) {
        if (mAuthority == null) {
            throw new IllegalStateException(
                    "ContentProvider must be attached before creating callbacks");
        }
        Intent intent = new Intent(ACTION_PROVIDER_RELAY);
        intent.setComponent(new ComponentName(mContext.getPackageName(),
                ProviderRelayReceiver.class.getName()));
        args.putString(EXTRA_METHOD, method);
        args.putString(ProviderRelayReceiver.EXTRA_AUTHORITY, mAuthority);
        intent.putExtras(args);
        return new RemoteCallback(mContext, TYPE_PROVIDER, intent, cls.getName(), args);
    }
}
