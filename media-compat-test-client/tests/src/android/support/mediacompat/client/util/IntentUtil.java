/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.mediacompat.client.util;

import static android.support.mediacompat.testlib.IntentConstants
        .ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD;
import static android.support.mediacompat.testlib.IntentConstants.KEY_ARGUMENT;
import static android.support.mediacompat.testlib.IntentConstants.KEY_METHOD_ID;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;

public class IntentUtil {

    public static final ComponentName SERVICE_RECEIVER_COMPONENT_NAME = new ComponentName(
            "android.support.mediacompat.service.test",
            "android.support.mediacompat.service.ServiceBroadcastReceiver");

    public static void callMediaBrowserServiceMethod(int methodId, Object arg, Context context) {
        Intent intent = createIntent(SERVICE_RECEIVER_COMPONENT_NAME, methodId, arg);
        intent.setAction(ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD);
        if (Build.VERSION.SDK_INT >= 16) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        context.sendBroadcast(intent);
    }

    private static Intent createIntent(ComponentName componentName, int methodId, Object arg) {
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.putExtra(KEY_METHOD_ID, methodId);

        if (arg instanceof String) {
            intent.putExtra(KEY_ARGUMENT, (String) arg);
        } else if (arg instanceof Integer) {
            intent.putExtra(KEY_ARGUMENT, (int) arg);
        } else if (arg instanceof Long) {
            intent.putExtra(KEY_ARGUMENT, (long) arg);
        } else if (arg instanceof Boolean) {
            intent.putExtra(KEY_ARGUMENT, (boolean) arg);
        } else if (arg instanceof Parcelable) {
            intent.putExtra(KEY_ARGUMENT, (Parcelable) arg);
        } else if (arg instanceof ArrayList<?>) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(KEY_ARGUMENT, (ArrayList<? extends Parcelable>) arg);
            intent.putExtras(bundle);
        } else if (arg instanceof Bundle) {
            Bundle bundle = new Bundle();
            bundle.putBundle(KEY_ARGUMENT, (Bundle) arg);
            intent.putExtras(bundle);
        }
        return intent;
    }
}
