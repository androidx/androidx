/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.mediacompat.testlib.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Methods and constants used for sending intent between client and service apps.
 */
public class IntentUtil {

    public static final String SERVICE_PACKAGE_NAME = "android.support.mediacompat.service.test";
    public static final String CLIENT_PACKAGE_NAME = "android.support.mediacompat.client.test";

    public static final ComponentName SERVICE_RECEIVER_COMPONENT_NAME = new ComponentName(
            SERVICE_PACKAGE_NAME, "android.support.mediacompat.service.ServiceBroadcastReceiver");
    public static final ComponentName CLIENT_RECEIVER_COMPONENT_NAME = new ComponentName(
            CLIENT_PACKAGE_NAME, "android.support.mediacompat.client.ClientBroadcastReceiver");

    public static final String ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD =
            "android.support.mediacompat.service.action.CALL_MEDIA_BROWSER_SERVICE_METHOD";
    public static final String ACTION_CALL_MEDIA_SESSION_METHOD =
            "android.support.mediacompat.service.action.CALL_MEDIA_SESSION_METHOD";
    public static final String ACTION_CALL_MEDIA_CONTROLLER_METHOD =
            "android.support.mediacompat.client.action.CALL_MEDIA_CONTROLLER_METHOD";
    public static final String ACTION_CALL_TRANSPORT_CONTROLS_METHOD =
            "android.support.mediacompat.client.action.CALL_TRANSPORT_CONTROLS_METHOD";

    public static final String KEY_METHOD_ID = "method_id";
    public static final String KEY_ARGUMENT = "argument";
    public static final String KEY_SESSION_TOKEN = "session_token";

    /**
     * Calls a method of MediaBrowserService. Used by client app.
     */
    public static void callMediaBrowserServiceMethod(int methodId, Object arg, Context context) {
        Intent intent = createIntent(SERVICE_RECEIVER_COMPONENT_NAME, methodId, arg);
        intent.setAction(ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD);
        if (Build.VERSION.SDK_INT >= 16) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        context.sendBroadcast(intent);
    }

    /**
     * Calls a method of MediaSession. Used by client app.
     */
    public static void callMediaSessionMethod(int methodId, Object arg, Context context) {
        Intent intent = createIntent(SERVICE_RECEIVER_COMPONENT_NAME, methodId, arg);
        intent.setAction(ACTION_CALL_MEDIA_SESSION_METHOD);
        if (Build.VERSION.SDK_INT >= 16) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        context.sendBroadcast(intent);
    }

    /**
     * Calls a method of MediaController. Used by service app.
     */
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

    /**
     * Calls a method of TransportControls. Used by service app.
     */
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

    private IntentUtil() {
    }
}
