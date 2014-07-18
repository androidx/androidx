/*
 * Copyright (C) 2014 The Android Open Source Project
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

class NotificationCompatApi21 {

    public static final String CATEGORY_CALL = Notification.CATEGORY_CALL;
    public static final String CATEGORY_MESSAGE = Notification.CATEGORY_MESSAGE;
    public static final String CATEGORY_EMAIL = Notification.CATEGORY_EMAIL;
    public static final String CATEGORY_EVENT = Notification.CATEGORY_EVENT;
    public static final String CATEGORY_PROMO = Notification.CATEGORY_PROMO;
    public static final String CATEGORY_ALARM = Notification.CATEGORY_ALARM;
    public static final String CATEGORY_PROGRESS = Notification.CATEGORY_PROGRESS;
    public static final String CATEGORY_SOCIAL = Notification.CATEGORY_SOCIAL;
    public static final String CATEGORY_ERROR = Notification.CATEGORY_ERROR;
    public static final String CATEGORY_TRANSPORT = Notification.CATEGORY_TRANSPORT;
    public static final String CATEGORY_SYSTEM = Notification.CATEGORY_SYSTEM;
    public static final String CATEGORY_SERVICE = Notification.CATEGORY_SERVICE;
    public static final String CATEGORY_RECOMMENDATION = Notification.CATEGORY_RECOMMENDATION;
    public static final String CATEGORY_STATUS = Notification.CATEGORY_STATUS;

    public static String getCategory(Notification notif) {
        return notif.category;
    }

}
