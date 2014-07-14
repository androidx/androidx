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
import android.view.Gravity;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link android.support.v4.app.NotificationCompat.WearableExtender}.
 */
public class NotificationCompatWearableExtenderTest extends AndroidTestCase {
    public static final int CUSTOM_CONTENT_HEIGHT_DP = 256;

    private PendingIntent mPendingIntent;
    private int mCustomContentHeightPx;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mPendingIntent = PendingIntent.getActivity(getContext(), 0, new Intent(), 0);

        mCustomContentHeightPx = Math.round(getContext().getResources().getDisplayMetrics().density
                * CUSTOM_CONTENT_HEIGHT_DP);
    }

    public void testEmptyEquals() throws Exception {
        assertExtendersEqual(new Notification.WearableExtender(),
                new NotificationCompat.WearableExtender());
    }

    public void testRealReadCompatEmptyValue() throws Exception {
        NotificationCompat.WearableExtender compatExtender =
                new NotificationCompat.WearableExtender();
        Notification notif = new NotificationCompat.Builder(getContext())
                .extend(compatExtender)
                .build();

        assertExtendersEqual(new Notification.WearableExtender(notif), compatExtender);
        assertExtendersEqual(new Notification.WearableExtender(notif),
                new NotificationCompat.WearableExtender(notif));
    }

    public void testCompatReadRealEmptyValue() throws Exception {
        Notification.WearableExtender realExtender =
                new Notification.WearableExtender();
        Notification notif = new Notification.Builder(getContext())
                .extend(realExtender)
                .build();

        assertExtendersEqual(realExtender, new NotificationCompat.WearableExtender(notif));
        assertExtendersEqual(new Notification.WearableExtender(notif),
                new NotificationCompat.WearableExtender(notif));
    }

    public void testRealReadCompatValue() throws Exception {
        RemoteInput.Builder remoteInput = new RemoteInput.Builder("result_key1")
                .setLabel("label")
                .setChoices(new CharSequence[] {"choice 1", "choice 2"});
        remoteInput.getExtras().putString("remoteinput_string", "test");
        NotificationCompat.Action.Builder action2 = new NotificationCompat.Action.Builder(
                R.drawable.action_icon, "Test title", mPendingIntent)
                .addRemoteInput(remoteInput.build())
                .extend(new NotificationCompat.Action.WearableExtender()
                        .setAvailableOffline(false));
        // Add an arbitrary key/value.
        action2.getExtras().putFloat("action_float", 10.5f);

        Notification page2 = new Notification.Builder(getContext())
                .setContentTitle("page2 title")
                .extend(new Notification.WearableExtender()
                        .setContentIcon(R.drawable.content_icon))
                .build();

        NotificationCompat.WearableExtender compatExtender =
                new NotificationCompat.WearableExtender()
                        .addAction(new NotificationCompat.Action(R.drawable.action_icon2, "Action1",
                                mPendingIntent))
                        .addAction(action2.build())
                        .setContentIntentAvailableOffline(false)
                        .setHintHideIcon(true)
                        .setHintShowBackgroundOnly(true)
                        .setStartScrollBottom(true)
                        .setDisplayIntent(mPendingIntent)
                        .addPage(page2)
                        .setContentIcon(R.drawable.content_icon2)
                        .setContentIconGravity(Gravity.START)
                        .setContentAction(5 /* arbitrary content action index */)
                        .setCustomSizePreset(NotificationCompat.WearableExtender.SIZE_MEDIUM)
                        .setCustomContentHeight(mCustomContentHeightPx)
                        .setGravity(Gravity.TOP);

        Notification notif = new NotificationCompat.Builder(getContext())
                .extend(compatExtender).build();
        assertExtendersEqual(new Notification.WearableExtender(notif), compatExtender);
        assertExtendersEqual(new Notification.WearableExtender(notif),
                new NotificationCompat.WearableExtender(notif));
    }

    public void testCompatReadRealValue() throws Exception {
        android.app.RemoteInput.Builder remoteInput = new android.app.RemoteInput.Builder(
                "result_key1")
                .setLabel("label")
                .setChoices(new CharSequence[] {"choice 1", "choice 2"});
        remoteInput.getExtras().putString("remoteinput_string", "test");
        Notification.Action.Builder action2 = new Notification.Action.Builder(
                R.drawable.action_icon, "Test title", mPendingIntent)
                .addRemoteInput(remoteInput.build())
                .extend(new Notification.Action.WearableExtender()
                        .setAvailableOffline(false));
        // Add an arbitrary key/value.
        action2.getExtras().putFloat("action_float", 10.5f);

        Notification page2 = new Notification.Builder(getContext())
                .setContentTitle("page2 title")
                .extend(new Notification.WearableExtender()
                        .setContentIcon(R.drawable.content_icon))
                .build();

        Notification.WearableExtender realExtender =
                new Notification.WearableExtender()
                        .addAction(new Notification.Action(R.drawable.action_icon2, "Action1",
                                mPendingIntent))
                        .addAction(action2.build())
                        .setContentIntentAvailableOffline(false)
                        .setHintHideIcon(true)
                        .setHintShowBackgroundOnly(true)
                        .setStartScrollBottom(true)
                        .setDisplayIntent(mPendingIntent)
                        .addPage(page2)
                        .setContentIcon(R.drawable.content_icon2)
                        .setContentIconGravity(Gravity.START)
                        .setContentAction(5 /* arbitrary content action index */)
                        .setCustomSizePreset(NotificationCompat.WearableExtender.SIZE_MEDIUM)
                        .setCustomContentHeight(mCustomContentHeightPx)
                        .setGravity(Gravity.TOP);

        Notification notif = new Notification.Builder(getContext())
                .extend(realExtender).build();
        assertExtendersEqual(realExtender, new NotificationCompat.WearableExtender(notif));
        assertExtendersEqual(new Notification.WearableExtender(notif),
                new NotificationCompat.WearableExtender(notif));
    }

    private void assertExtendersEqual(Notification.WearableExtender real,
            NotificationCompat.WearableExtender compat) {
        assertActionsEquals(real.getActions(), compat.getActions());
        assertEquals(real.getContentIntentAvailableOffline(),
                compat.getContentIntentAvailableOffline());
        assertEquals(real.getHintHideIcon(), compat.getHintHideIcon());
        assertEquals(real.getHintShowBackgroundOnly(), compat.getHintShowBackgroundOnly());
        assertEquals(real.getStartScrollBottom(), compat.getStartScrollBottom());
        assertEquals(real.getDisplayIntent(), compat.getDisplayIntent());
        assertPagesEquals(real.getPages(), compat.getPages());
        assertEquals(real.getBackground(), compat.getBackground());
        assertEquals(real.getContentIcon(), compat.getContentIcon());
        assertEquals(real.getContentIconGravity(), compat.getContentIconGravity());
        assertEquals(real.getContentAction(), compat.getContentAction());
        assertEquals(real.getCustomSizePreset(), compat.getCustomSizePreset());
        assertEquals(real.getCustomContentHeight(), compat.getCustomContentHeight());
        assertEquals(real.getGravity(), compat.getGravity());
    }

    private void assertPagesEquals(List<Notification> pages1, List<Notification> pages2) {
        assertEquals(pages1.size(), pages2.size());
        for (int i = 0; i < pages1.size(); i++) {
            assertNotificationsEqual(pages1.get(i), pages2.get(i));
        }
    }

    private void assertNotificationsEqual(Notification n1, Notification n2) {
        assertEquals(n1.icon, n2.icon);
        assertBundlesEqual(n1.extras, n2.extras);
        assertExtendersEqual(new Notification.WearableExtender(n1),
                new NotificationCompat.WearableExtender(n2));
    }

    private void assertActionsEquals(List<Notification.Action> realArray,
            List<NotificationCompat.Action> compatArray) {
        assertEquals(realArray.size(), compatArray.size());
        for (int i = 0; i < realArray.size(); i++) {
            assertActionsEqual(realArray.get(i), compatArray.get(i));
        }
    }

    private void assertActionsEqual(Notification.Action real, NotificationCompat.Action compat) {
        assertEquals(real.icon, compat.icon);
        assertEquals(real.title, compat.title);
        assertEquals(real.actionIntent, compat.actionIntent);
        assertRemoteInputsEquals(real.getRemoteInputs(), compat.getRemoteInputs());
        assertBundlesEqual(real.getExtras(), compat.getExtras());
    }

    private void assertRemoteInputsEquals(android.app.RemoteInput[] realArray,
            RemoteInput[] compatArray) {
        assertEquals(realArray == null, compatArray == null);
        if (realArray != null) {
            assertEquals(realArray.length, compatArray.length);
            for (int i = 0; i < realArray.length; i++) {
                assertRemoteInputsEqual(realArray[i], compatArray[i]);
            }
        }
    }

    private void assertRemoteInputsEqual(android.app.RemoteInput real,
            RemoteInput compat) {
        assertEquals(real.getResultKey(), compat.getResultKey());
        assertEquals(real.getLabel(), compat.getLabel());
        assertCharSequencesEquals(real.getChoices(), compat.getChoices());
        assertEquals(real.getAllowFreeFormInput(), compat.getAllowFreeFormInput());
        assertBundlesEqual(real.getExtras(), compat.getExtras());
    }

    private void assertCharSequencesEquals(CharSequence[] array1, CharSequence[] array2) {
        if (!Arrays.deepEquals(array1, array2)) {
            fail("Arrays not equal: " + Arrays.toString(array1) + " != " + Arrays.toString(array2));
        }
    }

    private void assertBundlesEqual(Bundle bundle1, Bundle bundle2) {
        assertEquals(bundle1.size(), bundle2.size());
        for (String key : bundle1.keySet()) {
            assertEquals(bundle1.get(key), bundle2.get(key));
        }
    }
}
