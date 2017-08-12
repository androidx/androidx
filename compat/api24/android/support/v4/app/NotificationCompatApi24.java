/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v4.app;

import android.app.Notification;
import android.app.RemoteInput;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

@RequiresApi(24)
class NotificationCompatApi24 {

    public static void addAction(Notification.Builder b, NotificationCompatBase.Action action) {
        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(
                action.getIcon(), action.getTitle(), action.getActionIntent());
        if (action.getRemoteInputs() != null) {
            for (RemoteInput remoteInput : RemoteInputCompatApi20.fromCompat(
                    action.getRemoteInputs())) {
                actionBuilder.addRemoteInput(remoteInput);
            }
        }
        Bundle actionExtras;
        if (action.getExtras() != null) {
            actionExtras = new Bundle(action.getExtras());
        } else {
            actionExtras = new Bundle();
        }
        actionExtras.putBoolean(NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES,
                action.getAllowGeneratedReplies());
        actionBuilder.setAllowGeneratedReplies(action.getAllowGeneratedReplies());
        actionBuilder.addExtras(actionExtras);
        b.addAction(actionBuilder.build());
    }

    static Notification.Action getActionFromActionCompat(
            NotificationCompatBase.Action actionCompat) {
        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(
                actionCompat.getIcon(), actionCompat.getTitle(), actionCompat.getActionIntent());
        Bundle actionExtras;
        if (actionCompat.getExtras() != null) {
            actionExtras = new Bundle(actionCompat.getExtras());
        } else {
            actionExtras = new Bundle();
        }
        actionExtras.putBoolean(NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES,
                actionCompat.getAllowGeneratedReplies());
        actionBuilder.setAllowGeneratedReplies(actionCompat.getAllowGeneratedReplies());
        actionBuilder.addExtras(actionExtras);
        RemoteInputCompatBase.RemoteInput[] remoteInputCompats = actionCompat.getRemoteInputs();
        if (remoteInputCompats != null) {
            RemoteInput[] remoteInputs = RemoteInputCompatApi20.fromCompat(remoteInputCompats);
            for (RemoteInput remoteInput : remoteInputs) {
                actionBuilder.addRemoteInput(remoteInput);
            }
        }
        return actionBuilder.build();
    }
}
