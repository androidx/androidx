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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.RestrictTo;

/**
 * The receiver used to call into providers when a pending intent is requested.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProviderRelayReceiver extends BroadcastReceiver {

    public static final String ACTION_PROVIDER_RELAY =
            "androidx.remotecallback.action.PROVIDER_RELAY";

    public static final String EXTRA_AUTHORITY =
            "androidx.remotecallback.extra.AUTHORITY";

    public static final String METHOD_PROVIDER_CALLBACK =
            "androidx.remotecallback.method.PROVIDER_CALLBACK";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_PROVIDER_RELAY.equals(intent.getAction())) {
            String authority = intent.getStringExtra(EXTRA_AUTHORITY);
            Uri uri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(authority)
                    .build();
            context.getContentResolver().call(uri, METHOD_PROVIDER_CALLBACK, null,
                    intent.getExtras());
        }
    }
}
