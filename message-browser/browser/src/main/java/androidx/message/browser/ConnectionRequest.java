/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.message.browser;

import android.os.Bundle;
import android.text.TextUtils;

class ConnectionRequest {
    static final String KEY_LIBRARY_VERSION = "androidx.message.LIBRARY_VERSION";
    static final String KEY_PACKAGE_NAME = "androidx.message.MessageBrowser.KEY_PACKAGE_NAME";
    static final String KEY_PROCESS_ID = "androidx.message.MessageBrowser.KEY_PROCESS_ID";
    static final String KEY_CONNECTION_HINTS = "androidx.message.MessageBrowser"
            + ".KEY_CONNECTION_HINTS";

    public final int version;
    public final String packageName;
    public final int pid;
    public final Bundle connectionHints;

    ConnectionRequest(String packageName, int pid, Bundle connectionHints) {
        this.version = MessageUtils.LIBRARY_VERSION;
        this.packageName = packageName;
        this.pid = pid;
        this.connectionHints = connectionHints;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{version=").append(version);
        sb.append(", packageName=").append(packageName);
        sb.append(", pid=").append(pid);
        sb.append(", connectionHints=").append(connectionHints);
        sb.append('}');
        return sb.toString();
    }

    private ConnectionRequest(String packageName, int pid, int version, Bundle connectionHints) {
        this.version = version;
        this.packageName = packageName;
        this.pid = pid;
        this.connectionHints = connectionHints;
    }

    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_LIBRARY_VERSION, version);
        bundle.putString(KEY_PACKAGE_NAME, packageName);
        bundle.putInt(KEY_PROCESS_ID, pid);
        bundle.putBundle(KEY_CONNECTION_HINTS, connectionHints);
        return bundle;
    }

    static final ConnectionRequest fromBundle(Bundle bundle) {
        int version = bundle.getInt(KEY_LIBRARY_VERSION);
        if (version < 1) return null;

        Bundle request = MessageUtils.unparcelWithClassLoader(bundle);
        String packageName = bundle.getString(KEY_PACKAGE_NAME);
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        int pid = request.getInt(KEY_PROCESS_ID, -1);
        if (pid < 0) return null;

        ConnectionRequest cr = new ConnectionRequest(packageName, pid, version,
                request.getBundle(KEY_CONNECTION_HINTS));
        return cr;
    }
}
