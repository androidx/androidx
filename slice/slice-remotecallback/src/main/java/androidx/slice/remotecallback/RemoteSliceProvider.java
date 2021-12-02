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

package androidx.slice.remotecallback;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.remotecallback.ProviderRelayReceiver.ACTION_PROVIDER_RELAY;
import static androidx.remotecallback.RemoteCallback.EXTRA_METHOD;
import static androidx.remotecallback.RemoteCallback.TYPE_PROVIDER;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.remotecallback.CallbackBase;
import androidx.remotecallback.CallbackHandlerRegistry;
import androidx.remotecallback.CallbackReceiver;
import androidx.remotecallback.ProviderRelayReceiver;
import androidx.remotecallback.RemoteCallback;
import androidx.slice.SliceProvider;

/**
 * Version of SliceProvider that can be used as a {@link CallbackReceiver}.
 *
 * Be sure to call the super of {@link #call} when unhandled to ensure
 * callbacks are triggered.
 *
 * @param <T> Should be specified as the root class (e.g. class X extends
 *           RemoteSliceProvider\<X>)
 */
public abstract class RemoteSliceProvider<T extends RemoteSliceProvider> extends
        SliceProvider implements CallbackReceiver<T>, CallbackBase<T> {

    String mAuthority;

    @Override
    public void attachInfo(@NonNull Context context, @NonNull ProviderInfo info) {
        super.attachInfo(context, info);
        mAuthority = info.authority;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (extras == null) return null;
        if (ProviderRelayReceiver.METHOD_PROVIDER_CALLBACK.equals(method)) {
            CallbackHandlerRegistry.sInstance.invokeCallback(getContext(), this, extras);
            return null;
        }
        return super.call(method, arg, extras);
    }

    @NonNull
    @Override
    public T createRemoteCallback(@NonNull Context context) {
        return CallbackHandlerRegistry.sInstance.getAndResetStub(getClass(), context, mAuthority);
    }

    /**
     * Note: Only visible because metalava doesn't realize this is hidden. Will properly
     * disappear when we have support for androidx-level @RestrictTo.
     * @hide
     */
    @NonNull
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public RemoteCallback toRemoteCallback(@NonNull Class<T> cls, @NonNull Context context,
            @NonNull String authority, @NonNull Bundle args, @Nullable String method) {
        if (authority == null) {
            throw new IllegalStateException(
                    "ContentProvider must be attached before creating callbacks");
        }
        Intent intent = new Intent(ACTION_PROVIDER_RELAY);
        intent.setComponent(new ComponentName(context.getPackageName(),
                ProviderRelayReceiver.class.getName()));
        args.putString(EXTRA_METHOD, method);
        args.putString(ProviderRelayReceiver.EXTRA_AUTHORITY, authority);
        intent.putExtras(args);
        return new RemoteCallback(context, TYPE_PROVIDER, intent, cls.getName(), args);
    }
}
