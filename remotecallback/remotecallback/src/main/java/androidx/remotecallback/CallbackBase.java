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

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Generates a {@link RemoteCallback} when a RemoteCallback is being triggered, should only
 * be used in the context on {@link CallbackReceiver#createRemoteCallback}.
 *
 * @param <T> Should be specified as the root class (e.g. class X extends
 *           CallbackReceiver\<X>)
 *
 * @deprecated Slice framework has been deprecated, it will not receive any updates moving
 * forward. If you are looking for a framework that handles communication across apps,
 * consider using {@link android.app.appsearch.AppSearchManager}.
 */
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface CallbackBase<T> {

    /**
     * Generates a {@link RemoteCallback} when a RemoteCallback is being triggered, should only
     * be used in the context on {@link CallbackReceiver#createRemoteCallback}.
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    RemoteCallback toRemoteCallback(@NonNull Class<T> cls, @NonNull Context context,
            @NonNull String authority, @NonNull Bundle args,
            @NonNull String method);
}
