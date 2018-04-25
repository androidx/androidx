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

package androidx.core.app;

import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static androidx.core.app.NotificationCompat.DEFAULT_LIGHTS;
import static androidx.core.app.NotificationCompat.DEFAULT_SOUND;
import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_ALL;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_CHILDREN;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.core.app.NotificationCompat.MessagingStyle.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    private static final String TEXT_RESULT_KEY = "text";
    private static final String DATA_RESULT_KEY = "data";
    private static final String EXTRA_COLORIZED = "android.colorized";

    Context mContext;

    public NotificationCompatTest() {
        super(TestActivity.class);
    }

    @Before
    public void setup() {
        mContext = mActivityTestRule.getActivity();
    }

    @Test
    public void testBadgeIcon() throws Throwable {
        int badgeIcon = NotificationCompat.BADGE_ICON_SMALL;
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setBadgeIconType(badgeIcon)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(badgeIcon, NotificationCompat.getBadgeIconType(n));
        } else {
            assertEquals(NotificationCompat.BADGE_ICON_NONE,
                    NotificationCompat.getBadgeIconType(n));
        }
    }

    @Test
    public void testTimeout() throws Throwable {
        long timeout = 23552;
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setTimeoutAfter(timeout)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(timeout, NotificationCompat.getTimeoutAfter(n));
        } else {
            assertEquals(0, NotificationCompat.getTimeoutAfter(n));
        }
    }

    @Test
    public void testShortcutId() throws Throwable {
        String shortcutId = "fgdfg";
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setShortcutId(shortcutId)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(shortcutId, NotificationCompat.getShortcutId(n));
        } else {
            assertEquals(null, NotificationCompat.getShortcutId(n));
        }
    }

    @Test
    public void testNotificationChannel() throws Throwable {
        String channelId = "new ID";
        Notification n  = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setChannelId(channelId)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(channelId, NotificationCompat.getChannelId(n));
        } else {
            assertNull(NotificationCompat.getChannelId(n));
        }
    }

    @Test
    public void testNotificationChannel_assignedFromBuilder() throws Throwable {
        String channelId = "new ID";
        Notification n  = new NotificationCompat.Builder(mActivityTestRule.getActivity(), channelId)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(channelId, NotificationCompat.getChannelId(n));
        } else {
            assertNull(NotificationCompat.getChannelId(n));
        }
    }

    @Test
    public void testNotificationActionBuilder_assignsColorized() throws Throwable {
        Notification n = newNotificationBuilder().setColorized(true).build();
        if (Build.VERSION.SDK_INT >= 26) {
            Bundle extras = NotificationCompat.getExtras(n);
            assertTrue(Boolean.TRUE.equals(extras.get(EXTRA_COLORIZED)));
        }
    }

    @Test
    public void testNotificationActionBuilder_unassignesColorized() throws Throwable {
        Notification n = newNotificationBuilder().setColorized(false).build();
        if (Build.VERSION.SDK_INT >= 26) {
            Bundle extras = NotificationCompat.getExtras(n);
            assertTrue(Boolean.FALSE.equals(extras.get(EXTRA_COLORIZED)));
        }
    }

    @Test
    public void testNotificationActionBuilder_doesntAssignColorized() throws Throwable {
        Notification n = newNotificationBuilder().build();
        if (Build.VERSION.SDK_INT >= 26) {
            Bundle extras = NotificationCompat.getExtras(n);
            assertFalse(extras.containsKey(EXTRA_COLORIZED));
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

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testFrameworkNotificationActionBuilder_setAllowGeneratedRepliesTrue()
            throws Throwable {
        Notification notif = new Notification.Builder(mContext)
                .addAction(new Notification.Action.Builder(0, "title", null)
                        .setAllowGeneratedReplies(true).build()).build();
        NotificationCompat.Action action = NotificationCompat.getAction(notif, 0);
        assertTrue(action.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationActionBuilder_defaultAllowGeneratedRepliesTrue() throws Throwable {
        NotificationCompat.Action a = newActionBuilder().build();

        assertTrue(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationActionBuilder_defaultShowsUserInterfaceTrue() {
        NotificationCompat.Action action = newActionBuilder().build();

        assertTrue(action.getShowsUserInterface());
    }

    @Test
    public void testNotificationAction_defaultAllowGeneratedRepliesTrue() throws Throwable {
        NotificationCompat.Action a = new NotificationCompat.Action(0, null, null);

        assertTrue(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationAction_defaultShowsUserInterfaceTrue() {
        NotificationCompat.Action action = new NotificationCompat.Action(0, null, null);

        assertTrue(action.getShowsUserInterface());
    }

    @Test
    public void testNotificationActionBuilder_setAllowGeneratedRepliesFalse() throws Throwable {
        NotificationCompat.Action a = newActionBuilder()
                .setAllowGeneratedReplies(false).build();

        assertFalse(a.getAllowGeneratedReplies());
    }

    @Test
    public void testNotificationAction_setShowsUserInterfaceFalse() {
        NotificationCompat.Action action = newActionBuilder()
                .setShowsUserInterface(false).build();

        assertFalse(action.getShowsUserInterface());
    }

    @SdkSuppress(minSdkVersion = 20)
    @Test
    public void testGetActionCompatFromAction_showsUserInterface() {
        NotificationCompat.Action action = newActionBuilder()
                .setShowsUserInterface(false).build();
        Notification notification = newNotificationBuilder().addAction(action).build();
        NotificationCompat.Action result =
                NotificationCompat.getActionCompatFromAction(notification.actions[0]);

        assertFalse(result.getExtras().getBoolean(
                NotificationCompat.Action.EXTRA_SHOWS_USER_INTERFACE, true));
        assertFalse(result.getShowsUserInterface());
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

    @Test
    public void testMessage_setAndGetExtras() throws Throwable {
        String extraKey = "extra_key";
        CharSequence extraValue = "extra_value";
        Message m =
                new Message("text", 0 /*timestamp */, "sender");
        m.getExtras().putCharSequence(extraKey, extraValue);
        assertEquals(extraValue, m.getExtras().getCharSequence(extraKey));

        ArrayList<Message> messages = new ArrayList<>(1);
        messages.add(m);
        Bundle[] bundleArray =
                Message.getBundleArrayForMessages(messages);
        assertEquals(1, bundleArray.length);
        Message fromBundle =
                Message.getMessageFromBundle(bundleArray[0]);
        assertEquals(extraValue, fromBundle.getExtras().getCharSequence(extraKey));
    }

    @Test
    public void testGetGroupAlertBehavior() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .build();
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(GROUP_ALERT_CHILDREN, NotificationCompat.getGroupAlertBehavior(n));
        } else {
            assertEquals(GROUP_ALERT_ALL, NotificationCompat.getGroupAlertBehavior(n));
        }
    }

    @Test
    public void testGroupAlertBehavior_mutesGroupNotifications() throws Throwable {
        // valid between api 20, when groups were added, and api 25, the last to use sound
        // and vibration from the notification itself

        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .setVibrate(new long[] {235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(true)
                .build();

        Notification n2 = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setVibrate(new long[] {235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(false)
                .build();

        if (Build.VERSION.SDK_INT >= 20 && !(Build.VERSION.SDK_INT >= 26)) {
            assertNull(n.sound);
            assertNull(n.vibrate);
            assertTrue((n.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n.defaults & DEFAULT_SOUND) == 0);
            assertTrue((n.defaults & DEFAULT_VIBRATE) == 0);

            assertNull(n2.sound);
            assertNull(n2.vibrate);
            assertTrue((n2.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n2.defaults & DEFAULT_SOUND) == 0);
            assertTrue((n2.defaults & DEFAULT_VIBRATE) == 0);
        } else if (Build.VERSION.SDK_INT < 20) {
            assertNotNull(n.sound);
            assertNotNull(n.vibrate);
            assertTrue((n.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n.defaults & DEFAULT_VIBRATE) != 0);

            assertNotNull(n2.sound);
            assertNotNull(n2.vibrate);
            assertTrue((n2.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n2.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n2.defaults & DEFAULT_VIBRATE) != 0);
        }
    }

    @Test
    public void testGroupAlertBehavior_doesNotMuteIncorrectGroupNotifications() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setVibrate(new long[] {235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(true)
                .build();

        Notification n2 = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .setVibrate(new long[] {235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(false)
                .build();

        Notification n3 = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setVibrate(new long[] {235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup("grouped")
                .setGroupSummary(false)
                .build();

        if (Build.VERSION.SDK_INT >= 20 && !(Build.VERSION.SDK_INT >= 26)) {
            assertNotNull(n.sound);
            assertNotNull(n.vibrate);
            assertTrue((n.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n.defaults & DEFAULT_VIBRATE) != 0);

            assertNotNull(n2.sound);
            assertNotNull(n2.vibrate);
            assertTrue((n2.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n2.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n2.defaults & DEFAULT_VIBRATE) != 0);

            assertNotNull(n3.sound);
            assertNotNull(n3.vibrate);
            assertTrue((n3.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n3.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n3.defaults & DEFAULT_VIBRATE) != 0);
        }
    }

    @Test
    public void testGroupAlertBehavior_doesNotMuteNonGroupNotifications() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setGroupAlertBehavior(GROUP_ALERT_CHILDREN)
                .setVibrate(new long[] {235})
                .setSound(Uri.EMPTY)
                .setDefaults(DEFAULT_ALL)
                .setGroup(null)
                .setGroupSummary(false)
                .build();
        if (!(Build.VERSION.SDK_INT >= 26)) {
            assertNotNull(n.sound);
            assertNotNull(n.vibrate);
            assertTrue((n.defaults & DEFAULT_LIGHTS) != 0);
            assertTrue((n.defaults & DEFAULT_SOUND) != 0);
            assertTrue((n.defaults & DEFAULT_VIBRATE) != 0);
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testHasAudioAttributesFrom21() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY)
                .build();
        assertNotNull(n.audioAttributes);
        assertEquals(-1, n.audioStreamType);
        assertEquals(Uri.EMPTY, n.sound);

        n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY, AudioManager.STREAM_RING)
                .build();
        assertNotNull(n.audioAttributes);
        assertEquals(AudioAttributes.CONTENT_TYPE_SONIFICATION,
                n.audioAttributes.getContentType());
        assertEquals(-1, n.audioStreamType);
        assertEquals(Uri.EMPTY, n.sound);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 20)
    public void testHasStreamTypePre21() throws Throwable {
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY, 34)
                .build();
        assertEquals(34, n.audioStreamType);
        assertEquals(Uri.EMPTY, n.sound);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testClearAlertingFieldsIfUsingChannels() throws Throwable {
        long[] vibration = new long[]{100};

        // stripped if using channels
        Notification n = new NotificationCompat.Builder(mActivityTestRule.getActivity(), "test")
                .setSound(Uri.EMPTY)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(vibration)
                .setLights(Color.BLUE, 100, 100)
                .build();
        assertNull(n.sound);
        assertEquals(0, n.defaults);
        assertNull(n.vibrate);
        assertEquals(0, n.ledARGB);
        assertEquals(0, n.ledOnMS);
        assertEquals(0, n.ledOffMS);

        // left intact if not using channels
        n = new NotificationCompat.Builder(mActivityTestRule.getActivity())
                .setSound(Uri.EMPTY)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(vibration)
                .setLights(Color.BLUE, 100, 100)
                .build();
        assertEquals(Uri.EMPTY, n.sound);
        assertNotNull(n.audioAttributes);
        assertEquals(Notification.DEFAULT_ALL, n.defaults);
        assertEquals(vibration, n.vibrate);
        assertEquals(Color.BLUE, n.ledARGB);
        assertEquals(100, n.ledOnMS);
        assertEquals(100, n.ledOffMS);
    }

    @Test
    public void testMessagingStyle_nullPerson() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name");
        messagingStyle.addMessage("text", 200, (Person) null);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        List<Message> result = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                .getMessages();

        assertEquals(1, result.size());
        assertEquals("text", result.get(0).getText());
        assertEquals(200, result.get(0).getTimestamp());
        assertNull(result.get(0).getPerson());
        assertNull(result.get(0).getSender());
    }

    @Test
    public void testMessagingStyle_message() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name");
        Person person = new Person.Builder().setName("test name").setKey("key").build();
        Person person2 = new Person.Builder()
                .setName("test name 2").setKey("key 2").setImportant(true).build();
        messagingStyle.addMessage("text", 200, person);
        messagingStyle.addMessage("text2", 300, person2);

        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        List<Message> result = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
                .getMessages();

        assertEquals(2, result.size());
        assertEquals("text", result.get(0).getText());
        assertEquals(200, result.get(0).getTimestamp());
        assertEquals("test name", result.get(0).getPerson().getName());
        assertEquals("key", result.get(0).getPerson().getKey());
        assertEquals("text2", result.get(1).getText());
        assertEquals(300, result.get(1).getTimestamp());
        assertEquals("test name 2", result.get(1).getPerson().getName());
        assertEquals("key 2", result.get(1).getPerson().getKey());
        assertTrue(result.get(1).getPerson().isImportant());
    }

    @Test
    public void testMessagingStyle_requiresNonEmptyUserName() {
        try {
            new NotificationCompat.MessagingStyle(new Person.Builder().build());
            fail("Expected IllegalArgumentException about a non-empty user name.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testMessagingStyle_isGroupConversation() {
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.P;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(true)
                        .setConversationTitle("test conversation title");
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_noConversationTitle() {
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.P;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(true)
                        .setConversationTitle(null);
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withConversationTitle_legacy() {
        // In legacy (version < P), isGroupConversation is controlled by conversationTitle.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setConversationTitle("test conversation title");
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withoutConversationTitle_legacy() {
        // In legacy (version < P), isGroupConversation is controlled by conversationTitle.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setConversationTitle(null);
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertFalse(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withConversationTitle_legacyWithOverride() {
        // #setGroupConversation should always take precedence over legacy behavior, so a non-null
        // title shouldn't affect #isGroupConversation.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(false)
                        .setConversationTitle("test conversation title");
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertFalse(result.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_isGroupConversation_withoutTitle_legacyWithOverride() {
        // #setGroupConversation should always take precedence over legacy behavior, so a null
        // title shouldn't affect #isGroupConversation.
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.O;
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(true)
                        .setConversationTitle(null);
        Notification notification = new NotificationCompat.Builder(mContext, "test id")
                .setSmallIcon(1)
                .setContentTitle("test title")
                .setStyle(messagingStyle)
                .build();

        NotificationCompat.MessagingStyle result =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification);

        assertTrue(result.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testMessagingStyle_applyNoTitleAndNotGroup() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(false)
                        .addMessage(
                                new Message(
                                        "body",
                                        1,
                                        new Person.Builder().setName("example name").build()))
                        .addMessage(new Message("body 2", 2, (Person) null));

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultCompatMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK >= 28 applies no title when none is provided to MessagingStyle.
        assertNull(resultCompatMessagingStyle.getConversationTitle());
        assertFalse(resultCompatMessagingStyle.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
    @Test
    public void testMessagingStyle_applyNoTitleAndNotGroup_legacy() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(false)
                        .addMessage(
                                new Message(
                                        "body",
                                        1,
                                        new Person.Builder().setName("example name").build()))
                        .addMessage(new Message("body 2", 2, (Person) null));

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultCompatMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK [24, 27] applies first incoming message sender name as Notification content title.
        assertEquals("example name", NotificationCompat.getContentTitle(resultNotification));
        assertNull(resultCompatMessagingStyle.getConversationTitle());
        assertFalse(resultCompatMessagingStyle.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testMessagingStyle_applyConversationTitleAndNotGroup() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(false)
                        .setConversationTitle("test title");

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK >= 28 applies provided title to MessagingStyle.
        assertEquals("test title", resultMessagingStyle.getConversationTitle());
        assertFalse(resultMessagingStyle.isGroupConversation());
    }

    @SdkSuppress(minSdkVersion = 19, maxSdkVersion = 27)
    @Test
    public void testMessagingStyle_applyConversationTitleAndNotGroup_legacy() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle("self name")
                        .setGroupConversation(false)
                        .setConversationTitle("test title");

        Notification resultNotification = new NotificationCompat.Builder(mContext, "test id")
                .setStyle(messagingStyle)
                .build();
        NotificationCompat.MessagingStyle resultMessagingStyle =
                NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(resultNotification);

        // SDK <= 27 applies MessagingStyle title as Notification content title.
        assertEquals("test title", NotificationCompat.getContentTitle(resultNotification));
        assertEquals("test title", resultMessagingStyle.getConversationTitle());
        assertFalse(resultMessagingStyle.isGroupConversation());
    }

    @Test
    public void testMessagingStyle_restoreFromCompatExtras() {
        NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(
                        new Person.Builder().setName("test name").build())
                        .setGroupConversation(true);
        Bundle bundle = new Bundle();
        messagingStyle.addCompatExtras(bundle);

        NotificationCompat.MessagingStyle resultMessagingStyle =
                new NotificationCompat.MessagingStyle(new Person.Builder().setName("temp").build());
        resultMessagingStyle.restoreFromCompatExtras(bundle);

        assertTrue(resultMessagingStyle.isGroupConversation());
        assertEquals("test name", resultMessagingStyle.getUser().getName());
    }

    @Test
    public void testMessagingStyleMessage_bundle_legacySender() {
        Bundle legacyBundle = new Bundle();
        legacyBundle.putCharSequence(Message.KEY_TEXT, "message");
        legacyBundle.putLong(Message.KEY_TIMESTAMP, 100);
        legacyBundle.putCharSequence(Message.KEY_SENDER, "sender");

        Message result = Message.getMessageFromBundle(legacyBundle);
        assertEquals("sender", result.getPerson().getName());
    }

    @Test
    public void action_builder_hasDefault() {
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(0, "Test Title", null).build();
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_NONE, action.getSemanticAction());
    }

    @Test
    public void action_builder_setSemanticAction() {
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(0, "Test Title", null)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .build();
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_REPLY, action.getSemanticAction());
    }

    @Test
    @SdkSuppress(minSdkVersion = 20)
    public void action_semanticAction_toAndFromNotification() {
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(0, "Test Title", null)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .build();
        Notification notification = newNotificationBuilder().addAction(action).build();
        NotificationCompat.Action result = NotificationCompat.getAction(notification, 0);

        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_REPLY, result.getSemanticAction());
    }

    private static final NotificationCompat.Action TEST_INVISIBLE_ACTION =
            new NotificationCompat.Action.Builder(0, "Test Title", null)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MUTE)
                    .setShowsUserInterface(false)
                    .build();

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void getInvisibleActions() {
        Notification notification =
                newNotificationBuilder().addInvisibleAction(TEST_INVISIBLE_ACTION).build();
        verifyInvisibleActionExists(notification);
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void getInvisibleActions_withCarExtender() {
        NotificationCompat.CarExtender carExtender = new NotificationCompat.CarExtender();
        Notification notification = newNotificationBuilder()
                .addInvisibleAction(TEST_INVISIBLE_ACTION)
                .extend(carExtender)
                .build();
        verifyInvisibleActionExists(notification);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void getContentTitle() {
        Notification notification = new NotificationCompat.Builder(mContext, "test channel")
                .setContentTitle("example title")
                .build();

        assertEquals("example title", NotificationCompat.getContentTitle(notification));
    }

    private static void verifyInvisibleActionExists(Notification notification) {
        List<NotificationCompat.Action> result =
                NotificationCompat.getInvisibleActions(notification);
        assertTrue("Expecting 1 result, got " + result.size(), result.size() == 1);
        NotificationCompat.Action resultAction = result.get(0);
        assertEquals(resultAction.getIcon(), TEST_INVISIBLE_ACTION.getIcon());
        assertEquals(resultAction.getTitle(), TEST_INVISIBLE_ACTION.getTitle());
        assertEquals(
                resultAction.getShowsUserInterface(),
                TEST_INVISIBLE_ACTION.getShowsUserInterface());
        assertEquals(resultAction.getSemanticAction(), TEST_INVISIBLE_ACTION.getSemanticAction());
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
