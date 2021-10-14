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

package androidx.browser.trusted;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 *
 * Utility class to use new APIs that were added in O (API level 25). These need to exist in a
 * separate class so that Android framework can successfully verify classes without
 * encountering the new APIs.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.O)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class NotificationApiHelperForO {
    static boolean isChannelEnabled(NotificationManager manager, String channelId) {
        NotificationChannel channel = manager.getNotificationChannel(channelId);

        return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    @Nullable static Notification copyNotificationOntoChannel(Context context,
            NotificationManager manager, Notification notification, String channelId,
            String channelName) {
        // Create the notification channel, (no-op if already created).
        manager.createNotificationChannel(new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_DEFAULT));

        // Check that the channel is enabled.
        if (manager.getNotificationChannel(channelId).getImportance()
                == NotificationManager.IMPORTANCE_NONE) {
            return null;
        }

        // Set our notification to have that channel.
        Notification.Builder builder = Notification.Builder.recoverBuilder(context, notification);
        builder.setChannelId(channelId);
        return builder.build();
    }

    private NotificationApiHelperForO() {}
}
