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

package android.support.mediacompat.service.util;

import static android.support.mediacompat.testlib.IntentConstants
        .ACTION_CALL_MEDIA_CONTROLLER_METHOD;
import static android.support.mediacompat.testlib.IntentConstants
        .ACTION_CALL_TRANSPORT_CONTROLS_METHOD;
import static android.support.mediacompat.testlib.IntentConstants.KEY_ARGUMENT;
import static android.support.mediacompat.testlib.IntentConstants.KEY_METHOD_ID;
import static android.support.mediacompat.testlib.IntentConstants.KEY_SESSION_TOKEN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;

public final class TestUtil {

    public static final ComponentName CLIENT_RECEIVER_COMPONENT_NAME = new ComponentName(
            "android.support.mediacompat.client.test",
            "android.support.mediacompat.client.ClientBroadcastReceiver");

    public static void assertBundleEquals(Bundle expected, Bundle observed) {
        if (expected == null || observed == null) {
            assertTrue(expected == observed);
            return;
        }
        assertEquals(expected.size(), observed.size());
        for (String key : expected.keySet()) {
            assertEquals(expected.get(key), observed.get(key));
        }
    }

    public static void callMediaControllerMethod(
            int methodId, Object arg, Context context, Parcelable token) {
        Intent intent = createIntent(CLIENT_RECEIVER_COMPONENT_NAME, methodId, arg);
        intent.setAction(ACTION_CALL_MEDIA_CONTROLLER_METHOD);
        intent.putExtra(KEY_SESSION_TOKEN, token);
        if (Build.VERSION.SDK_INT >= 16) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        context.sendBroadcast(intent);
    }

    public static void callTransportControlsMethod(
            int methodId, Object arg, Context context, Parcelable token) {
        Intent intent = createIntent(CLIENT_RECEIVER_COMPONENT_NAME, methodId, arg);
        intent.setAction(ACTION_CALL_TRANSPORT_CONTROLS_METHOD);
        intent.putExtra(KEY_SESSION_TOKEN, token);
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
