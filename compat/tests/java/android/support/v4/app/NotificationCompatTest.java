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
 * limitations under the License.
 */

package android.support.v4.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.content.Context;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.os.BuildCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationCompatTest extends BaseInstrumentationTestCase<TestSupportActivity> {
    private static final String TEXT_RESULT_KEY = "text";
    private static final String DATA_RESULT_KEY = "data";

    Context mContext;

    public NotificationCompatTest() {
        super(TestSupportActivity.class);
    }

    @Before
    public void setup() {
        mContext = mActivityTestRule.getActivity();
    }

    @Test
    public void testNotificationChannel() throws Throwable {
        String channelId = "new ID";
        Notification n  = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setChannel(channelId)
                .build();
        if (BuildCompat.isAtLeastO()) {
            assertEquals(channelId, NotificationCompat.getChannel(n));
        } else {
            assertNull(NotificationCompat.getChannel(n));
        }
    }

    @Test
    public void testNotificationActionBuilder_copiesRemoteInputs() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .addRemoteInput(new RemoteInput("a", "b", null, false, null, null)).build();

        NotificationCompat.Action aCopy = new NotificationCompat.Action.Builder(a).build();

        assertSame(a.getRemoteInputs()[0], aCopy.getRemoteInputs()[0]);
    }

    @Test
    public void testNotificationActionBuilder_copiesAllowGeneratedReplies() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(true).build();

        NotificationCompat.Action aCopy = new NotificationCompat.Action.Builder(a).build();

        assertEquals(a.getAllowGeneratedReplies(), aCopy.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationActionBuilder_defaultAllowGeneratedRepliesTrue() throws Throwable {
        NotificationCompat.Action a = newActionBuilder().build();

        assertTrue(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationAction_defaultAllowGeneratedRepliesTrue() throws Throwable {
        NotificationCompat.Action a = new NotificationCompat.Action(0, null, null);

        assertTrue(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationActionBuilder_setAllowGeneratedRepliesFalse() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(false).build();

        assertFalse(a.getAllowGeneratedReplies());
    }

    @SdkSuppress(minSdkVersion = 17)
    @Test
    public void testNotificationWearableExtenderAction_setAllowGeneratedRepliesTrue()
            throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(true).build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        assertTrue(new NotificationCompat.WearableExtender(notification).getActions().get(0)
                .getAllowGeneratedReplies());
    }

    @SdkSuppress(minSdkVersion = 17)
    @Test
    public void testNotificationWearableExtenderAction_setAllowGeneratedRepliesFalse()
            throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(false).build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        assertFalse(new NotificationCompat.WearableExtender(notification).getActions().get(0)
                .getAllowGeneratedReplies());
    }


    @SdkSuppress(maxSdkVersion = 16)
    @SmallTest
    @Test
    public void testNotificationWearableExtenderAction_noActions()
            throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(true).build();
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .addAction(a);
        Notification notification = newNotificationBuilder().extend(extender).build();
        assertTrue(new NotificationCompat.WearableExtender(notification).getActions().size() == 0);
    }

    @Test
    public void testNotificationActionBuilder_setDataOnlyRemoteInput() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .addRemoteInput(newDataOnlyRemoteInput()).build();
        RemoteInput[] textInputs = a.getRemoteInputs();
        assertTrue(textInputs == null || textInputs.length == 0);
        verifyRemoteInputArrayHasSingleResult(a.getDataOnlyRemoteInputs(), DATA_RESULT_KEY);
    }

    @Test
    public void testNotificationActionBuilder_setTextAndDataOnlyRemoteInput() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .addRemoteInput(newDataOnlyRemoteInput())
                .addRemoteInput(newTextRemoteInput())
                .build();

        verifyRemoteInputArrayHasSingleResult(a.getRemoteInputs(), TEXT_RESULT_KEY);
        verifyRemoteInputArrayHasSingleResult(a.getDataOnlyRemoteInputs(), DATA_RESULT_KEY);
    }

    private static RemoteInput newDataOnlyRemoteInput() {
        return new RemoteInput.Builder(DATA_RESULT_KEY)
            .setAllowFreeFormInput(false)
            .setAllowDataType("mimeType", true)
            .build();
    }

    private static RemoteInput newTextRemoteInput() {
        return new RemoteInput.Builder(TEXT_RESULT_KEY).build();  // allowFreeForm defaults to true
    }

    private static void verifyRemoteInputArrayHasSingleResult(
            RemoteInput[] remoteInputs, String expectedResultKey) {
        assertTrue(remoteInputs != null && remoteInputs.length == 1);
        assertEquals(expectedResultKey, remoteInputs[0].getResultKey());
    }

    private static NotificationCompat.Action.Builder newActionBuilder() {
        return new NotificationCompat.Action.Builder(0, "title", null);
    }

    private NotificationCompat.Builder newNotificationBuilder() {
        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(0)
                .setContentTitle("title")
                .setContentText("text");
    }
}
