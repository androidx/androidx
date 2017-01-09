/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.app;

import android.annotation.TargetApi;
import android.app.Notification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;

@RequiresApi(24)
@TargetApi(24)
class NotificationCompatImpl24 {

    public static void addDecoratedCustomViewStyle(NotificationBuilderWithBuilderAccessor b) {
        Notification.Builder builder = b.getBuilder();
        builder.setStyle(new Notification.DecoratedCustomViewStyle());
    }

    public static void addDecoratedMediaCustomViewStyle(NotificationBuilderWithBuilderAccessor b) {
        Notification.Builder builder = b.getBuilder();
        builder.setStyle(new Notification.DecoratedMediaCustomViewStyle());
    }
}
