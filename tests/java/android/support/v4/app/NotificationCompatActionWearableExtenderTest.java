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
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.tests.R;
import android.test.AndroidTestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link android.support.v4.app.NotificationCompat.Action.WearableExtender}.
 */
public class NotificationCompatActionWearableExtenderTest extends AndroidTestCase {

    private int mIcon;
    private String mTitle = "Test Title";
    private PendingIntent mPendingIntent;

    private String mInProgress = "In Progress Label";
    private String mConfirm = "Confirmation Label";
    private String mCancel = "Cancelation Label";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mIcon = R.drawable.action_icon;
        mPendingIntent = PendingIntent.getActivity(getContext(), 0, new Intent(), 0);
    }

    // Test that the default empty Extender is equal to the compat version.
    public void testEmptyEquals() throws Exception {
        assertExtendersEqual(new Notification.Action.WearableExtender(),
                new NotificationCompat.Action.WearableExtender());
    }

    // Test that the fully populated Extender is equal to the compat version.
    public void testFullEquals() throws Exception {
        Notification.Action.WearableExtender baseExtender =
            new Notification.Action.WearableExtender()
                .setAvailableOffline(true)
                .setInProgressLabel(mInProgress)
                .setConfirmLabel(mConfirm)
                .setCancelLabel(mCancel);
        NotificationCompat.Action.WearableExtender compatExtender =
            new NotificationCompat.Action.WearableExtender()
                .setAvailableOffline(true)
                .setInProgressLabel(mInProgress)
                .setConfirmLabel(mConfirm)
                .setCancelLabel(mCancel);
        assertExtendersEqual(baseExtender, compatExtender);
    }

    // Test that the base WearableExtender from an empty Notification is equal to the compat.
    public void testEmptyNotification() throws Exception {
        Notification baseNotif = new Notification.Builder(getContext())
                .build();
        Notification compatNotif = new NotificationCompat.Builder(getContext())
                .build();

        assertExtendersFromNotificationEqual(baseNotif, baseNotif);
        assertExtendersFromNotificationEqual(compatNotif, compatNotif);
        assertExtendersFromNotificationEqual(baseNotif, compatNotif);
        assertExtendersFromNotificationEqual(compatNotif, baseNotif);
    }

    public void testDefaultActionNotification() throws Exception {
        Notification.Action.Builder baseAction =
            new Notification.Action.Builder(mIcon, mTitle, mPendingIntent);
        NotificationCompat.Action.Builder compatAction =
            new NotificationCompat.Action.Builder(mIcon, mTitle, mPendingIntent);

        Notification.WearableExtender baseNoteExtender =
                new Notification.WearableExtender()
                        .addAction(baseAction.build());
        NotificationCompat.WearableExtender compatNoteExtender =
                new NotificationCompat.WearableExtender()
                        .addAction(compatAction.build());

        Notification baseNotif = new Notification.Builder(getContext())
                .extend(baseNoteExtender).build();
        Notification compatNotif = new NotificationCompat.Builder(getContext())
                .extend(compatNoteExtender).build();

        assertExtendersFromNotificationEqual(baseNotif, baseNotif);
        assertExtendersFromNotificationEqual(compatNotif, compatNotif);
        assertExtendersFromNotificationEqual(baseNotif, compatNotif);
        assertExtendersFromNotificationEqual(compatNotif, baseNotif);
    }

    public void testDefaultActionExtenderNotification() throws Exception {
        Notification.Action.WearableExtender baseExtender =
            new Notification.Action.WearableExtender();
        NotificationCompat.Action.WearableExtender compatExtender =
            new NotificationCompat.Action.WearableExtender();

        Notification.Action.Builder baseAction =
            new Notification.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(baseExtender);
        NotificationCompat.Action.Builder compatAction =
            new NotificationCompat.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(compatExtender);

        Notification.WearableExtender baseNoteExtender =
                new Notification.WearableExtender()
                        .addAction(baseAction.build());
        NotificationCompat.WearableExtender compatNoteExtender =
                new NotificationCompat.WearableExtender()
                        .addAction(compatAction.build());

        Notification baseNotif = new Notification.Builder(getContext())
                .extend(baseNoteExtender).build();
        Notification compatNotif = new NotificationCompat.Builder(getContext())
                .extend(compatNoteExtender).build();

        assertExtendersFromNotificationEqual(baseNotif, baseNotif);
        assertExtendersFromNotificationEqual(compatNotif, compatNotif);
        assertExtendersFromNotificationEqual(baseNotif, compatNotif);
        assertExtendersFromNotificationEqual(compatNotif, baseNotif);
    }

    public void testFullNotification() throws Exception {
        Notification.Action.WearableExtender baseExtender =
            new Notification.Action.WearableExtender()
                .setAvailableOffline(true)
                .setInProgressLabel(mInProgress)
                .setConfirmLabel(mConfirm)
                .setCancelLabel(mCancel);
        NotificationCompat.Action.WearableExtender compatExtender =
            new NotificationCompat.Action.WearableExtender()
                .setAvailableOffline(true)
                .setInProgressLabel(mInProgress)
                .setConfirmLabel(mConfirm)
                .setCancelLabel(mCancel);

        Notification.Action.Builder baseAction =
            new Notification.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(baseExtender);
        NotificationCompat.Action.Builder compatAction =
            new NotificationCompat.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(compatExtender);

        Notification.WearableExtender baseNoteExtender =
                new Notification.WearableExtender()
                        .addAction(baseAction.build());
        NotificationCompat.WearableExtender compatNoteExtender =
                new NotificationCompat.WearableExtender()
                        .addAction(compatAction.build());

        Notification baseNotif = new Notification.Builder(getContext())
                .extend(baseNoteExtender).build();
        Notification compatNotif = new NotificationCompat.Builder(getContext())
                .extend(compatNoteExtender).build();

        assertExtendersFromNotificationEqual(baseNotif, baseNotif);
        assertExtendersFromNotificationEqual(compatNotif, compatNotif);
        assertExtendersFromNotificationEqual(baseNotif, compatNotif);
        assertExtendersFromNotificationEqual(compatNotif, baseNotif);
    }

    public void testMultipleActionsInANotification() throws Exception {
        Notification.Action.WearableExtender baseExtender1 =
            new Notification.Action.WearableExtender()
                .setAvailableOffline(true)
                .setInProgressLabel(mInProgress)
                .setConfirmLabel(mConfirm)
                .setCancelLabel(mCancel);
        NotificationCompat.Action.WearableExtender compatExtender1 =
            new NotificationCompat.Action.WearableExtender()
                .setAvailableOffline(true)
                .setInProgressLabel(mInProgress)
                .setConfirmLabel(mConfirm)
                .setCancelLabel(mCancel);

        Notification.Action.Builder baseAction1 =
            new Notification.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(baseExtender1);
        NotificationCompat.Action.Builder compatAction1 =
            new NotificationCompat.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(compatExtender1);

        Notification.Action.WearableExtender baseExtender2 =
            new Notification.Action.WearableExtender()
                .setAvailableOffline(false)
                .setInProgressLabel("Alternate Label")
                .setConfirmLabel("Duplicated Label")
                .setCancelLabel("Duplicated Label");
        NotificationCompat.Action.WearableExtender compatExtender2 =
            new NotificationCompat.Action.WearableExtender()
                .setAvailableOffline(false)
                .setInProgressLabel("Alternate Label")
                .setConfirmLabel("Duplicated Label")
                .setCancelLabel("Duplicated Label");

        Notification.Action.Builder baseAction2 =
            new Notification.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(baseExtender2);
        NotificationCompat.Action.Builder compatAction2 =
            new NotificationCompat.Action.Builder(mIcon, mTitle, mPendingIntent)
                .extend(compatExtender2);

        Notification.WearableExtender baseNoteExtender =
                new Notification.WearableExtender()
                        .addAction(baseAction1.build())
                        .addAction(new Notification.Action(R.drawable.action_icon2, "Action1",
                                mPendingIntent))
                        .addAction(baseAction2.build());
        NotificationCompat.WearableExtender compatNoteExtender =
                new NotificationCompat.WearableExtender()
                        .addAction(compatAction1.build())
                        .addAction(new NotificationCompat.Action(R.drawable.action_icon2,
                                "Action1", mPendingIntent))
                        .addAction(compatAction2.build());

        Notification baseNotif = new Notification.Builder(getContext())
                .extend(baseNoteExtender).build();
        Notification compatNotif = new NotificationCompat.Builder(getContext())
                .extend(compatNoteExtender).build();

        assertExtendersFromNotificationEqual(baseNotif, baseNotif);
        assertExtendersFromNotificationEqual(compatNotif, compatNotif);
        assertExtendersFromNotificationEqual(baseNotif, compatNotif);
        assertExtendersFromNotificationEqual(compatNotif, baseNotif);
    }

    private void assertExtendersEqual(Notification.Action.WearableExtender base,
            NotificationCompat.Action.WearableExtender compat) {
        assertEquals(base.isAvailableOffline(), compat.isAvailableOffline());
        assertEquals(base.getInProgressLabel(), compat.getInProgressLabel());
        assertEquals(base.getConfirmLabel(), compat.getConfirmLabel());
        assertEquals(base.getCancelLabel(), compat.getCancelLabel());
    }

    // Parse the Notification using the base parser and the compat parser and confirm
    // that the WearableExtender bundles are equivelent.
    private void assertExtendersFromNotificationEqual(Notification first,
                                                      Notification second) {
        Notification.WearableExtender baseExtender = new Notification.WearableExtender(first);
        NotificationCompat.WearableExtender compatExtender =
            new NotificationCompat.WearableExtender(second);
        List<Notification.Action> baseArray = baseExtender.getActions();
        List<NotificationCompat.Action> compatArray = compatExtender.getActions();
        assertEquals(baseArray.size(), compatArray.size());
        for (int i = 0; i < baseArray.size(); i++) {
            // Verify that the key value pairs are equal. We only care about
            // the bundle in getExtras().getBundle("android.wearable.EXTENSIONS"),
            // but it doesn't hurt to check them all as long we recurse.
            assertBundlesEqual(baseArray.get(i).getExtras(),
                               compatArray.get(i).getExtras());
            // Verify that the parsed WearableExtentions are equal
            Notification.Action.WearableExtender base =
                new Notification.Action.WearableExtender(baseArray.get(i));
            NotificationCompat.Action.WearableExtender compat =
                new NotificationCompat.Action.WearableExtender(compatArray.get(i));
            assertExtendersEqual(base, compat);
        }
    }

    private void assertBundlesEqual(Bundle bundle1, Bundle bundle2) {
        assertEquals(bundle1.size(), bundle2.size());
        for (String key : bundle1.keySet()) {
            Object value1 = bundle1.get(key);
            Object value2 = bundle2.get(key);
            if (value1 instanceof Bundle && value2 instanceof Bundle) {
                assertBundlesEqual((Bundle) value1, (Bundle) value2);
            } else {
                assertEquals(value1, value2);
            }
        }
    }
}
