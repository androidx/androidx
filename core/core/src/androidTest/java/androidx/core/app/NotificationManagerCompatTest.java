/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static androidx.core.app.NotificationChannelCompatTest.TestHelper.areEqual;
import static androidx.core.app.NotificationChannelCompatTest.TestHelper.listContains;
import static androidx.core.app.NotificationChannelCompatTest.TestHelper.listContainsCompat;
import static androidx.core.app.NotificationChannelGroupCompatTest.TestHelper.findGroupCompat;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_MAX;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_MIN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationManagerCompatTest {

    private static final String TAG = NotificationManagerCompatTest.class.getSimpleName();

    private static final String TYPE_CHANNEL = "channel";
    private static final String TYPE_GROUP = "group";
    private static final String TYPE_SHORTCUT = "shortcut";

    private Context mContext;
    private NotificationManager mPlatformNotificationManager;


    /**
     * Generate unique ID for Channels and Groups to prevent conflicts between tests
     *
     * @param type Type of ID. Channel, group or any string
     * @return Unique ID
     */
    private static String genUniqueId(String type) {
        return TAG + "_" + UUID.randomUUID() + "_" + type + "_id";
    }

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mPlatformNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    @AfterClass
    public static void clean() {
        // Delete all channels and groups created during tests

        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager notificationManager =
                (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        List<NotificationChannel> channels = notificationManager.getNotificationChannels();
        for (NotificationChannel channel : channels) {
            // Generated IDs start with TAG
            if (channel.getId().startsWith(TAG)) {
                notificationManager.deleteNotificationChannel(channel.getId());
            }
        }

        List<NotificationChannelGroup> groups = notificationManager.getNotificationChannelGroups();
        for (NotificationChannelGroup group : groups) {
            // Generated IDs start with TAG
            if (group.getId().startsWith(TAG)) {
                notificationManager.deleteNotificationChannelGroup(group.getId());
            }
        }

    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testConstants() {
        assertEquals(NotificationManager.IMPORTANCE_UNSPECIFIED,
                NotificationManagerCompat.IMPORTANCE_UNSPECIFIED);
        assertEquals(NotificationManager.IMPORTANCE_MIN,
                NotificationManagerCompat.IMPORTANCE_MIN);
        assertEquals(NotificationManager.IMPORTANCE_LOW,
                NotificationManagerCompat.IMPORTANCE_LOW);
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT,
                NotificationManagerCompat.IMPORTANCE_DEFAULT);
        assertEquals(NotificationManager.IMPORTANCE_HIGH,
                NotificationManagerCompat.IMPORTANCE_HIGH);
        assertEquals(NotificationManager.IMPORTANCE_MAX,
                NotificationManagerCompat.IMPORTANCE_MAX);
    }

    @Test
    public void testGetNotificationChannel() {
        String channelId = genUniqueId(TYPE_CHANNEL);
        String channelName = "channelName";
        String channelDescription = "channelDescription";

        // create a channel, so we can get it later
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                    new NotificationChannel(channelId, channelName, IMPORTANCE_DEFAULT);
            channel.setDescription(channelDescription);
            mPlatformNotificationManager.createNotificationChannel(channel);
            assertNotNull(mPlatformNotificationManager.getNotificationChannel(channelId));
        }

        NotificationChannel result = NotificationManagerCompat.from(mContext)
                .getNotificationChannel(channelId);

        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(result);
            assertEquals(channelId, result.getId());
            assertEquals(channelName, result.getName());
            assertEquals(IMPORTANCE_DEFAULT, result.getImportance());
            assertEquals(channelDescription, result.getDescription());
        } else {
            // should be null on SDKs which don't support Notification Channels
            assertNull(result);
        }
    }

    @Test
    public void testGetNotificationChannelWithUnknownId() {
        NotificationChannel result = NotificationManagerCompat.from(mContext)
                .getNotificationChannel("unknownChannelId");

        assertNull(result);
    }

    @Test
    public void testGetNotificationChannelGroup() {
        String groupId = genUniqueId(TYPE_GROUP);
        String groupName = "groupName";
        String groupDescription = "groupDescription";
        String groupChannelId = genUniqueId(TYPE_CHANNEL);
        String groupChannelName = "groupChannelName";
        String groupChannelDescription = "groupChannelDescription";

        // create a group with channel, so we can get them later
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannelGroup group = new NotificationChannelGroup(groupId, groupName);
            if (Build.VERSION.SDK_INT >= 28) group.setDescription(groupDescription);

            NotificationChannel channel =
                    new NotificationChannel(groupChannelId, groupChannelName, IMPORTANCE_LOW);
            channel.setDescription(groupChannelDescription);
            channel.setGroup(groupId);

            mPlatformNotificationManager.createNotificationChannelGroup(group);
            mPlatformNotificationManager.createNotificationChannel(channel);

            // get group by its ID was added in SDK 28
            if (Build.VERSION.SDK_INT >= 28) {
                assertNotNull(mPlatformNotificationManager.getNotificationChannelGroup(groupId));
            } else {
                assertTrue(mPlatformNotificationManager.getNotificationChannelGroups()
                        .contains(group));
            }
            assertNotNull(mPlatformNotificationManager.getNotificationChannel(groupChannelId));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        NotificationChannelGroup resultGroup =
                notificationManager.getNotificationChannelGroup(groupId);
        NotificationChannel resultChannel =
                notificationManager.getNotificationChannel(groupChannelId);


        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(resultGroup);
            assertEquals(groupId, resultGroup.getId());
            assertEquals(groupName, resultGroup.getName());
            if (Build.VERSION.SDK_INT >= 28) {
                assertEquals(groupDescription, resultGroup.getDescription());
            }
            assertNotNull(resultChannel);
            assertEquals(groupChannelId, resultChannel.getId());
            assertEquals(groupChannelName, resultChannel.getName());
            assertEquals(groupId, resultChannel.getGroup());
            assertEquals(IMPORTANCE_LOW, resultChannel.getImportance());
            assertEquals(groupChannelDescription, resultChannel.getDescription());
        } else {
            // should be null on SDKs which don't support Notification Channels
            assertNull(resultGroup);
        }
    }

    @Test
    public void testGetNotificationChannelGroupWithUnknownId() {
        NotificationChannelGroup result = NotificationManagerCompat.from(mContext)
                .getNotificationChannelGroup("unknownGroupId");

        assertNull(result);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannel() {
        String channelId = genUniqueId(TYPE_CHANNEL);
        String channelName = "channelName";
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        notificationManager.createNotificationChannel(
                new NotificationChannel(channelId, channelName, IMPORTANCE_DEFAULT));

        NotificationChannel result = mPlatformNotificationManager.getNotificationChannel(channelId);

        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals(channelName, result.getName());
        assertEquals(IMPORTANCE_DEFAULT, result.getImportance());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannelWithParams() {
        String channelId = genUniqueId(TYPE_CHANNEL);
        String channelName = "channelName";
        String channelDescription = "channelDescription";
        int channelLightColor = Color.GREEN;
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        NotificationChannel channel =
                new NotificationChannel(channelId, channelName, IMPORTANCE_HIGH);
        channel.setDescription(channelDescription);
        channel.enableLights(true);
        channel.setLightColor(channelLightColor);
        channel.setSound(Uri.EMPTY, audioAttributes);

        notificationManager.createNotificationChannel(channel);

        NotificationChannel result = mPlatformNotificationManager.getNotificationChannel(channelId);
        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals(channelName, result.getName());
        assertEquals(IMPORTANCE_HIGH, result.getImportance());
        assertEquals(channelDescription, result.getDescription());
        assertTrue(result.shouldShowLights());
        assertEquals(channelLightColor, result.getLightColor());
        assertEquals(Uri.EMPTY, result.getSound());
        assertEquals(audioAttributes.getContentType(),
                result.getAudioAttributes().getContentType());
        assertEquals(audioAttributes.getUsage(), result.getAudioAttributes().getUsage());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testUpdateNotificationChannelWithParams() {
        String channelId = genUniqueId(TYPE_CHANNEL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        NotificationChannelCompat channel =
                new NotificationChannelCompat.Builder(channelId, IMPORTANCE_LOW)
                        .setName("channelName")
                        .setDescription("channelDescription")
                        .setLightsEnabled(true)
                        .setLightColor(Color.GREEN)
                        .setSound(Uri.EMPTY, new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build())
                        .build();

        notificationManager.createNotificationChannel(channel);

        NotificationChannel result = mPlatformNotificationManager.getNotificationChannel(channelId);
        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals("channelName", result.getName());
        assertEquals(IMPORTANCE_LOW, result.getImportance());
        assertEquals("channelDescription", result.getDescription());
        assertTrue(result.shouldShowLights());
        assertEquals(Color.GREEN, result.getLightColor());
        assertEquals(Uri.EMPTY, result.getSound());
        assertEquals(AudioAttributes.CONTENT_TYPE_SONIFICATION,
                result.getAudioAttributes().getContentType());
        assertEquals(AudioAttributes.USAGE_NOTIFICATION, result.getAudioAttributes().getUsage());

        channel = new NotificationChannelCompat.Builder(channelId, IMPORTANCE_HIGH)
                .setName("channelName2")
                .setDescription("channelDescription2")
                .setLightsEnabled(false)
                .setLightColor(Color.RED)
                .setSound(Uri.EMPTY, new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build())
                .build();

        notificationManager.createNotificationChannel(channel);

        result = mPlatformNotificationManager.getNotificationChannel(channelId);
        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals("channelName2", result.getName());
        assertEquals(IMPORTANCE_LOW, result.getImportance());
        assertEquals("channelDescription2", result.getDescription());
        assertTrue(result.shouldShowLights());
        assertEquals(Color.GREEN, result.getLightColor());
        assertEquals(Uri.EMPTY, result.getSound());
        assertEquals(AudioAttributes.CONTENT_TYPE_SONIFICATION,
                result.getAudioAttributes().getContentType());
        assertEquals(AudioAttributes.USAGE_NOTIFICATION, result.getAudioAttributes().getUsage());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannelGroup() {
        String groupId = genUniqueId(TYPE_GROUP);
        String groupName = "groupName";

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        notificationManager.createNotificationChannelGroup(
                new NotificationChannelGroup(groupId, groupName));

        NotificationChannelGroup result = notificationManager.getNotificationChannelGroup(groupId);
        assertNotNull(result);
        assertEquals(groupId, result.getId());
        assertEquals(groupName, result.getName());
        assertTrue(result.getChannels().isEmpty());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannelGroupWithParams() {
        String groupId = genUniqueId(TYPE_GROUP);
        String groupName = "groupName";
        String groupDescription = "groupDescription";
        String channelId = genUniqueId(TYPE_CHANNEL);
        NotificationChannel notificationChannel =
                new NotificationChannel(channelId, "groupChannelName", IMPORTANCE_LOW);
        notificationChannel.setGroup(groupId);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        NotificationChannelGroup group = new NotificationChannelGroup(groupId, groupName);
        if (Build.VERSION.SDK_INT >= 28) group.setDescription(groupDescription);
        notificationManager.createNotificationChannelGroup(group);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationChannelGroup result = notificationManager.getNotificationChannelGroup(groupId);
        assertNotNull(result);
        assertEquals(groupId, result.getId());
        assertEquals(groupName, result.getName());
        if (Build.VERSION.SDK_INT >= 28) assertEquals(groupDescription, result.getDescription());

        NotificationChannel resultChannel =
                mPlatformNotificationManager.getNotificationChannel(channelId);
        assertNotNull(resultChannel);
        assertEquals(groupId, resultChannel.getGroup());
        assertTrue(areEqual(notificationChannel, resultChannel));
    }


    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannels() {
        String channelOneId = genUniqueId(TYPE_CHANNEL);
        String channelTwoId = genUniqueId(TYPE_CHANNEL);
        NotificationChannel channelOne = new NotificationChannel(channelOneId, "oneName",
                IMPORTANCE_DEFAULT);
        NotificationChannel channelTwo = new NotificationChannel(channelTwoId, "twoName",
                IMPORTANCE_MIN);
        List<NotificationChannel> channels = Arrays.asList(channelOne, channelTwo);
        int channelsBefore = mPlatformNotificationManager.getNotificationChannels().size();

        NotificationManagerCompat.from(mContext).createNotificationChannels(channels);

        // check if channels were created
        List<NotificationChannel> result = mPlatformNotificationManager.getNotificationChannels();
        assertEquals(channelsBefore + channels.size(), result.size());
        assertTrue(listContains(result, channels));

        // just to be sure
        NotificationChannel channel =
                mPlatformNotificationManager.getNotificationChannel(channelOneId);
        assertNotNull(channel);
        assertEquals(channelOne.getName(), channel.getName());
        assertEquals(IMPORTANCE_DEFAULT, channel.getImportance());

        channel = mPlatformNotificationManager.getNotificationChannel(channelTwoId);
        assertNotNull(channel);
        assertEquals(channelTwo.getName(), channel.getName());
        assertEquals(IMPORTANCE_MIN, channel.getImportance());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannelGroups() {
        String groupOneId = genUniqueId(TYPE_GROUP);
        String channelGroupOneId = genUniqueId(TYPE_CHANNEL);
        NotificationChannelGroup groupOne = new NotificationChannelGroup(groupOneId,
                "groupOneName");
        NotificationChannel channelGroupOne = new NotificationChannel(channelGroupOneId,
                "channelGroupOneName", IMPORTANCE_MIN);
        channelGroupOne.setGroup(groupOneId);

        String channelGroupTwoId = genUniqueId(TYPE_CHANNEL);
        String secondChannelGroupTwoId = genUniqueId(TYPE_CHANNEL);
        NotificationChannelGroup groupTwo = new NotificationChannelGroup(genUniqueId(TYPE_GROUP),
                "groupTwoName");
        NotificationChannel channelGroupTwo = new NotificationChannel(channelGroupTwoId,
                "channelGroupTwoName", IMPORTANCE_DEFAULT);
        channelGroupTwo.setGroup(groupTwo.getId());
        NotificationChannel secondChannelGroupTwo = new NotificationChannel(
                secondChannelGroupTwoId, "secondChannelGroupTwoName",
                IMPORTANCE_MAX);
        secondChannelGroupTwo.setGroup(groupTwo.getId());

        List<NotificationChannelGroup> groups = Arrays.asList(groupOne, groupTwo);
        List<NotificationChannel> channels =
                Arrays.asList(channelGroupOne, channelGroupTwo, secondChannelGroupTwo);

        int groupsBefore = mPlatformNotificationManager.getNotificationChannelGroups().size();
        int channelsBefore = mPlatformNotificationManager.getNotificationChannels().size();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.createNotificationChannelGroups(groups);
        notificationManager.createNotificationChannels(channels);

        // the correct number of groups and channels was created
        assertEquals(groupsBefore + groups.size(),
                mPlatformNotificationManager.getNotificationChannelGroups().size());
        assertEquals(channelsBefore + channels.size(),
                mPlatformNotificationManager.getNotificationChannels().size());

        NotificationChannelGroup resultOne = notificationManager.getNotificationChannelGroup(
                groupOneId);
        assertNotNull(resultOne);
        assertEquals(groupOne.getName(), resultOne.getName());
        NotificationChannel resultChannel =
                mPlatformNotificationManager.getNotificationChannel(channelGroupOneId);
        assertEquals(groupOneId, resultChannel.getGroup());
        assertTrue(areEqual(channelGroupOne, resultChannel));

        NotificationChannelGroup resultTwo = notificationManager.getNotificationChannelGroup(
                groupTwo.getId());
        assertNotNull(resultTwo);
        assertEquals(groupTwo.getName(), resultTwo.getName());
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(2, resultTwo.getChannels().size());
            assertTrue(listContains(resultTwo.getChannels(),
                    Arrays.asList(channelGroupTwo, secondChannelGroupTwo)));
        } else {
            // TODO(b/34970783): On API < 28 NotificationChannelGroup#getChannels always returned an
            //  empty list. A complete NotificationManagerCompat implementation could hide this bug.
            assertEquals(0, resultTwo.getChannels().size());
        }

        assertTrue(listContains(
                mPlatformNotificationManager.getNotificationChannels(), channels));
    }

    @Test
    public void testCreateNotificationChannelCompat() {
        String channelId = genUniqueId(TYPE_CHANNEL);
        String channelName = "channelName";
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(channelId, IMPORTANCE_DEFAULT)
                        .setName(channelName)
                        .build());

        NotificationChannel result = notificationManager.getNotificationChannel(channelId);
        if (Build.VERSION.SDK_INT < 26) {
            assertNull(result);
            return;
        }
        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals(channelName, result.getName());
        assertEquals(IMPORTANCE_DEFAULT, result.getImportance());
    }

    @Test
    public void testCreateNotificationChannelCompatWithParams() {
        String channelId = genUniqueId(TYPE_CHANNEL);
        String channelName = "channelName";
        String channelDescription = "channelDescription";
        int channelLightColor = Color.GREEN;
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(channelId, IMPORTANCE_HIGH)
                        .setName(channelName)
                        .setDescription(channelDescription)
                        .setLightsEnabled(true)
                        .setLightColor(channelLightColor)
                        .setSound(Uri.EMPTY, audioAttributes)
                        .build());

        NotificationChannel result = notificationManager.getNotificationChannel(channelId);
        if (Build.VERSION.SDK_INT < 26) {
            assertNull(result);
            return;
        }
        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals(channelName, result.getName());
        assertEquals(IMPORTANCE_HIGH, result.getImportance());
        assertEquals(channelDescription, result.getDescription());
        assertTrue(result.shouldShowLights());
        assertEquals(channelLightColor, result.getLightColor());
        assertEquals(Uri.EMPTY, result.getSound());
        assertEquals(audioAttributes.getContentType(),
                result.getAudioAttributes().getContentType());
        assertEquals(audioAttributes.getUsage(), result.getAudioAttributes().getUsage());
    }

    @Test
    public void testCreateNotificationChannelGroupCompat() {
        String groupId = genUniqueId(TYPE_GROUP);
        String groupName = "groupName";

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        notificationManager.createNotificationChannelGroup(
                new NotificationChannelGroupCompat.Builder(groupId)
                        .setName(groupName)
                        .build());

        NotificationChannelGroup result = notificationManager.getNotificationChannelGroup(groupId);
        if (Build.VERSION.SDK_INT < 26) {
            assertNull(result);
            return;
        }
        assertNotNull(result);
        assertEquals(groupId, result.getId());
        assertEquals(groupName, result.getName());
        assertTrue(result.getChannels().isEmpty());
    }

    @Test
    public void testCreateNotificationChannelGroupCompatWithParams() {
        String groupId = genUniqueId(TYPE_GROUP);
        String groupName = "groupName";
        String groupDescription = "groupDescription";
        String channelId = genUniqueId(TYPE_CHANNEL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        notificationManager.createNotificationChannelGroup(
                new NotificationChannelGroupCompat.Builder(groupId)
                        .setName(groupName)
                        .setDescription(groupDescription)
                        .build());
        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(channelId, IMPORTANCE_LOW)
                        .setName("groupChannelName")
                        .setGroup(groupId)
                        .build());

        NotificationChannelGroup result = notificationManager.getNotificationChannelGroup(groupId);
        NotificationChannel resultChannel = notificationManager.getNotificationChannel(channelId);
        if (Build.VERSION.SDK_INT < 26) {
            assertNull(result);
            assertNull(resultChannel);
            return;
        }

        assertNotNull(result);
        assertEquals(groupId, result.getId());
        assertEquals(groupName, result.getName());
        if (Build.VERSION.SDK_INT >= 28) assertEquals(groupDescription, result.getDescription());

        assertNotNull(resultChannel);
        assertEquals(groupId, resultChannel.getGroup());
        assertTrue(areEqual(
                new NotificationChannelCompat.Builder(channelId, IMPORTANCE_LOW)
                        .setName("groupChannelName")
                        .setGroup(groupId)
                        .build()
                        .getNotificationChannel(),
                resultChannel));
    }


    @Test
    public void testCreateNotificationChannelsCompat() {
        String channelOneId = genUniqueId(TYPE_CHANNEL);
        String channelTwoId = genUniqueId(TYPE_CHANNEL);
        NotificationChannelCompat channelOne =
                new NotificationChannelCompat.Builder(channelOneId, IMPORTANCE_DEFAULT)
                        .setName("oneName")
                        .build();
        NotificationChannelCompat channelTwo =
                new NotificationChannelCompat.Builder(channelTwoId, IMPORTANCE_MIN)
                        .setName("twoName")
                        .build();
        List<NotificationChannelCompat> channels = Arrays.asList(channelOne, channelTwo);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        int channelsBefore = notificationManager.getNotificationChannels().size();

        notificationManager.createNotificationChannelsCompat(channels);

        // check if channels were created
        List<NotificationChannel> result = notificationManager.getNotificationChannels();
        if (Build.VERSION.SDK_INT < 26) {
            assertTrue(result.isEmpty());
            return;
        }
        assertEquals(channelsBefore + channels.size(), result.size());
        assertTrue(listContainsCompat(result, channels));

        // just to be sure
        NotificationChannel channel = notificationManager.getNotificationChannel(channelOneId);
        assertNotNull(channel);
        assertEquals("oneName", channel.getName());
        assertEquals(IMPORTANCE_DEFAULT, channel.getImportance());

        channel = notificationManager.getNotificationChannel(channelTwoId);
        assertNotNull(channel);
        assertEquals("twoName", channel.getName());
        assertEquals(IMPORTANCE_MIN, channel.getImportance());
    }

    @Test
    public void testCreateNotificationChannelGroupsCompat() {
        String groupOneId = genUniqueId(TYPE_GROUP);
        String channelGroupOneId = genUniqueId(TYPE_CHANNEL);
        NotificationChannelGroupCompat groupOne =
                new NotificationChannelGroupCompat.Builder(groupOneId)
                        .setName("groupOneName")
                        .build();
        NotificationChannelCompat channelGroupOne =
                new NotificationChannelCompat.Builder(channelGroupOneId, IMPORTANCE_MIN)
                        .setName("channelGroupOneName")
                        .setGroup(groupOneId)
                        .build();

        String channelGroupTwoId = genUniqueId(TYPE_CHANNEL);
        String secondChannelGroupTwoId = genUniqueId(TYPE_CHANNEL);
        NotificationChannelGroupCompat groupTwo =
                new NotificationChannelGroupCompat.Builder(genUniqueId(TYPE_GROUP))
                        .setName("groupTwoName")
                        .build();
        NotificationChannelCompat channelGroupTwo =
                new NotificationChannelCompat.Builder(channelGroupTwoId, IMPORTANCE_DEFAULT)
                        .setName("channelGroupTwoName")
                        .setGroup(groupTwo.getId()).build();
        NotificationChannelCompat secondChannelGroupTwo =
                new NotificationChannelCompat.Builder(secondChannelGroupTwoId, IMPORTANCE_MAX)
                        .setName("secondChannelGroupTwoName")
                        .setGroup(groupTwo.getId())
                        .build();

        List<NotificationChannelGroupCompat> groups = Arrays.asList(groupOne, groupTwo);
        List<NotificationChannelCompat> channels =
                Arrays.asList(channelGroupOne, channelGroupTwo, secondChannelGroupTwo);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        int groupsBefore = notificationManager.getNotificationChannelGroups().size();
        int channelsBefore = notificationManager.getNotificationChannels().size();

        notificationManager.createNotificationChannelGroupsCompat(groups);
        notificationManager.createNotificationChannelsCompat(channels);
        if (Build.VERSION.SDK_INT < 26) {
            assertEquals(0, notificationManager.getNotificationChannelGroups().size());
            assertEquals(0, notificationManager.getNotificationChannels().size());
            return;
        }
        // the correct number of groups and channels was created
        assertEquals(groupsBefore + groups.size(),
                notificationManager.getNotificationChannelGroups().size());
        assertEquals(channelsBefore + channels.size(),
                notificationManager.getNotificationChannels().size());

        NotificationChannelGroup resultOne = notificationManager.getNotificationChannelGroup(
                groupOneId);
        assertNotNull(resultOne);
        assertEquals("groupOneName", resultOne.getName());
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(1, resultOne.getChannels().size());
            assertTrue(listContainsCompat(resultOne.getChannels(),
                    Collections.singletonList(channelGroupOne)));
        } else {
            // TODO(b/34970783): On API < 28 NotificationChannelGroup#getChannels always returned an
            //  empty list. A complete NotificationManagerCompat implementation could hide this bug.
            assertEquals(0, resultOne.getChannels().size());
        }
        NotificationChannel resultChannel =
                notificationManager.getNotificationChannel(channelGroupOneId);
        assertEquals(groupOneId, resultChannel.getGroup());
        assertTrue(areEqual(channelGroupOne, resultChannel));

        NotificationChannelGroup resultTwo = notificationManager.getNotificationChannelGroup(
                groupTwo.getId());
        assertNotNull(resultTwo);
        assertEquals("groupTwoName", resultTwo.getName());
        if (Build.VERSION.SDK_INT >= 28) {
            assertEquals(2, resultTwo.getChannels().size());
            assertTrue(listContainsCompat(resultTwo.getChannels(),
                    Arrays.asList(channelGroupTwo, secondChannelGroupTwo)));
        } else {
            // TODO(b/34970783): On API < 28 NotificationChannelGroup#getChannels always returned an
            //  empty list. A complete NotificationManagerCompat implementation could hide this bug.
            assertEquals(0, resultTwo.getChannels().size());
        }
        assertTrue(listContainsCompat(
                notificationManager.getNotificationChannels(), channels));
    }

    @Test
    public void testDeleteNotificationChannel() {
        String channelId = genUniqueId(TYPE_CHANNEL);

        // create a channel, so we can delete it later
        if (Build.VERSION.SDK_INT >= 26) {
            mPlatformNotificationManager.createNotificationChannel(
                    new NotificationChannel(channelId, "channelName", IMPORTANCE_DEFAULT));
            assertNotNull(mPlatformNotificationManager.getNotificationChannel(channelId));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.deleteNotificationChannel(channelId);

        assertNull(notificationManager.getNotificationChannel(channelId));

        if (Build.VERSION.SDK_INT >= 26) {
            assertNull(mPlatformNotificationManager.getNotificationChannel(channelId));
        }
    }

    @Test
    public void testDeleteUnlistedNotificationChannels() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        String solo1Id = genUniqueId(TYPE_CHANNEL);
        String parent2Id = genUniqueId(TYPE_CHANNEL);
        String child2Id = genUniqueId(TYPE_CHANNEL);
        String solo3Id = genUniqueId(TYPE_CHANNEL);
        String parent4Id = genUniqueId(TYPE_CHANNEL);
        String child4Id = genUniqueId(TYPE_CHANNEL);
        String shortcutId = genUniqueId(TYPE_SHORTCUT);

        // create 4 channels
        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(solo1Id, IMPORTANCE_DEFAULT)
                        .setName("solo1Name")
                        .build());
        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(parent2Id, IMPORTANCE_DEFAULT)
                        .setName("parent2Name")
                        .build());
        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(solo3Id, IMPORTANCE_DEFAULT)
                        .setName("solo3Name")
                        .build());
        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(parent4Id, IMPORTANCE_DEFAULT)
                        .setName("parent4Name")
                        .build());

        if (Build.VERSION.SDK_INT >= 30) {
            notificationManager.createNotificationChannel(
                    new NotificationChannelCompat.Builder(child2Id, IMPORTANCE_DEFAULT)
                            .setName("child2Name")
                            .setConversationId(parent2Id, shortcutId)
                            .build());
            notificationManager.createNotificationChannel(
                    new NotificationChannelCompat.Builder(child4Id, IMPORTANCE_DEFAULT)
                            .setName("child4Name")
                            .setConversationId(parent4Id, shortcutId)
                            .build());
        }

        // Check that channels exist on O+
        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(notificationManager.getNotificationChannel(solo1Id));
            assertNotNull(notificationManager.getNotificationChannel(parent2Id));
            assertNotNull(notificationManager.getNotificationChannel(solo3Id));
            assertNotNull(notificationManager.getNotificationChannel(parent4Id));
        }
        if (Build.VERSION.SDK_INT >= 30) {
            assertNotNull(notificationManager.getNotificationChannel(child2Id));
            NotificationChannel child2Result =
                    notificationManager.getNotificationChannel(parent2Id, shortcutId);
            assertNotNull(child2Result);
            assertEquals(child2Id, child2Result.getId());

            assertNotNull(notificationManager.getNotificationChannel(child4Id));
            NotificationChannel child4Result =
                    notificationManager.getNotificationChannel(parent4Id, shortcutId);
            assertNotNull(child4Result);
            assertEquals(child4Id, child4Result.getId());
        }

        // delete all except 1 and 2
        notificationManager.deleteUnlistedNotificationChannels(
                Arrays.asList(solo1Id, parent2Id));

        // Check that 1 and 2 exist on O+
        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(notificationManager.getNotificationChannel(solo1Id));
            assertNotNull(notificationManager.getNotificationChannel(parent2Id));
        }
        // Check that 3 and 4 do not exist
        assertNull(notificationManager.getNotificationChannel(solo3Id));
        assertNull(notificationManager.getNotificationChannel(parent4Id));
        // Check that the child of 4 was deleted, but not the child of 2
        if (Build.VERSION.SDK_INT >= 30) {
            assertNotNull(notificationManager.getNotificationChannel(child2Id));
            assertNotNull(notificationManager.getNotificationChannel(parent2Id, shortcutId));
            assertNull(notificationManager.getNotificationChannel(child4Id));
            assertNull(notificationManager.getNotificationChannel(parent4Id, shortcutId));
        }
    }

    @Test
    public void testDeleteNotificationChannelGroup() {
        String groupId = genUniqueId(TYPE_GROUP);

        // create a group, so we can delete it later
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannelGroup group = new NotificationChannelGroup(groupId, "groupName");
            mPlatformNotificationManager.createNotificationChannelGroup(group);
            // get group by its ID was added in SDK 28
            if (Build.VERSION.SDK_INT >= 28) {
                assertNotNull(mPlatformNotificationManager.getNotificationChannelGroup(groupId));
            } else {
                assertTrue(mPlatformNotificationManager.getNotificationChannelGroups()
                        .contains(group));
            }
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.deleteNotificationChannelGroup(groupId);

        assertNull(notificationManager.getNotificationChannelGroup(groupId));

        if (Build.VERSION.SDK_INT >= 28) {
            assertNull(mPlatformNotificationManager.getNotificationChannelGroup(groupId));
        }
    }

    @Test
    public void testGetNotificationChannelWithShortcut() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        String parentId = genUniqueId(TYPE_CHANNEL);
        String childId = genUniqueId(TYPE_CHANNEL);
        String shortcutId = genUniqueId(TYPE_SHORTCUT);

        // create channels
        notificationManager.createNotificationChannel(
                new NotificationChannelCompat.Builder(parentId, IMPORTANCE_DEFAULT)
                        .setName("channelName")
                        .build());

        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(notificationManager.getNotificationChannel(parentId));
            // Always return the parent when no matching child exists (if support channels)
            NotificationChannel childResult =
                    notificationManager.getNotificationChannel(parentId, shortcutId);
            assertNotNull(childResult);
            assertEquals(parentId, childResult.getId());
        } else {
            assertNull(notificationManager.getNotificationChannel(parentId));
            assertNull(notificationManager.getNotificationChannel(parentId, shortcutId));
        }

        // Make a child channel ONLY on platforms that support conversations
        if (Build.VERSION.SDK_INT >= 30) {
            NotificationChannel child = new NotificationChannel(childId,
                    "childName", IMPORTANCE_DEFAULT);
            child.setConversationId(parentId, shortcutId);
            notificationManager.createNotificationChannel(child);
            // make sure the child channel exists
            assertNotNull(notificationManager.getNotificationChannel(childId));
        }

        // Check behavior of getting the notification channel by the conversation
        NotificationChannel result =
                notificationManager.getNotificationChannel(parentId, shortcutId);
        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(result);
            if (Build.VERSION.SDK_INT >= 30) {
                assertEquals(childId, result.getId());
                assertEquals(parentId, result.getParentChannelId());
                assertEquals(shortcutId, result.getConversationId());
            } else {
                assertEquals(parentId, result.getId());
            }
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testGetNotificationChannelWithShortcutCompat() {
        NotificationManagerCompat nm = NotificationManagerCompat.from(mContext);
        String parentId = genUniqueId(TYPE_CHANNEL);
        String childId = genUniqueId(TYPE_CHANNEL);
        String shortcutId = genUniqueId(TYPE_SHORTCUT);

        // create channels
        nm.createNotificationChannel(
                new NotificationChannelCompat.Builder(parentId, IMPORTANCE_DEFAULT)
                        .setName("channelName")
                        .build());

        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(nm.getNotificationChannelCompat(parentId));
            // Always return the parent when no matching child exists (if support channels)
            NotificationChannelCompat childResult =
                    nm.getNotificationChannelCompat(parentId, shortcutId);
            assertNotNull(childResult);
            assertEquals(parentId, childResult.getId());
        } else {
            assertNull(nm.getNotificationChannelCompat(parentId));
            assertNull(nm.getNotificationChannelCompat(parentId, shortcutId));
        }

        // Make a child channel ONLY on platforms that support conversations
        if (Build.VERSION.SDK_INT >= 30) {
            NotificationChannel child = new NotificationChannel(childId,
                    "childName", IMPORTANCE_DEFAULT);
            child.setConversationId(parentId, shortcutId);
            nm.createNotificationChannel(child);
            // make sure the child channel exists
            assertNotNull(nm.getNotificationChannelCompat(childId));
        }

        // Check behavior of getting the notification channel by the conversation
        NotificationChannelCompat result = nm.getNotificationChannelCompat(parentId, shortcutId);
        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(result);
            if (Build.VERSION.SDK_INT >= 30) {
                assertEquals(childId, result.getId());
                assertEquals(parentId, result.getParentChannelId());
                assertEquals(shortcutId, result.getConversationId());
            } else {
                assertEquals(parentId, result.getId());
                assertNull(result.getParentChannelId());
                assertNull(result.getConversationId());
            }
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testGetNotificationChannels() {
        // create a channel, so we can get it later
        if (Build.VERSION.SDK_INT >= 26) {
            String channelId = genUniqueId(TYPE_CHANNEL);
            mPlatformNotificationManager.createNotificationChannel(new NotificationChannel(
                    channelId, "channelName", IMPORTANCE_DEFAULT));
            assertNotNull(mPlatformNotificationManager.getNotificationChannel(channelId));
        }

        List<NotificationChannel> channels = NotificationManagerCompat.from(mContext)
                .getNotificationChannels();

        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(mPlatformNotificationManager.getNotificationChannels().size(),
                    channels.size());
            assertEquals(mPlatformNotificationManager.getNotificationChannels(), channels);
        } else {
            // list should be empty on SDKs which don't support Notification Channels
            assertTrue(channels.isEmpty());
        }
    }

    @Test
    public void testGetNotificationChannelsCompat() {
        NotificationManagerCompat nm = NotificationManagerCompat.from(mContext);

        // create a channel, so we can get it later
        String channelId = genUniqueId(TYPE_CHANNEL);
        nm.createNotificationChannel(
                new NotificationChannelCompat.Builder(channelId, IMPORTANCE_DEFAULT)
                        .setName("channelName").build());

        // test getNotificationChannelCompat(String) works as expected
        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(nm.getNotificationChannelCompat(channelId));
        } else {
            assertNull(nm.getNotificationChannelCompat(channelId));
        }

        // test getNotificationChannelsCompat() works as expected
        List<NotificationChannelCompat> compatChannels = nm.getNotificationChannelsCompat();
        if (Build.VERSION.SDK_INT >= 26) {
            List<NotificationChannel> platformChannels =
                    mPlatformNotificationManager.getNotificationChannels();
            assertEquals(platformChannels.size(), compatChannels.size());
            assertTrue(listContainsCompat(platformChannels, compatChannels));
        } else {
            // list should be empty on SDKs which don't support Notification Channels
            assertTrue(compatChannels.isEmpty());
        }
    }

    @Test
    public void testGetNotificationChannelGroups() {
        // create a group, so we can get it later
        if (Build.VERSION.SDK_INT >= 26) {
            String groupId = genUniqueId(TYPE_GROUP);
            NotificationChannelGroup group = new NotificationChannelGroup(groupId, "groupName");
            mPlatformNotificationManager.createNotificationChannelGroup(group);
            // get group by its ID was added in SDK 28
            if (Build.VERSION.SDK_INT >= 28) {
                assertNotNull(mPlatformNotificationManager.getNotificationChannelGroup(groupId));
            } else {
                assertTrue(mPlatformNotificationManager.getNotificationChannelGroups()
                        .contains(group));
            }
        }

        List<NotificationChannelGroup> groups = NotificationManagerCompat.from(mContext)
                .getNotificationChannelGroups();

        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(mPlatformNotificationManager.getNotificationChannelGroups().size(),
                    groups.size());
            assertEquals(mPlatformNotificationManager.getNotificationChannelGroups(), groups);
        } else {
            // list should be empty on SDKs which don't support Notification Channels
            assertTrue(groups.isEmpty());
        }
    }

    @Test
    public void testGetNotificationChannelGroupsCompat() {
        NotificationManagerCompat nm = NotificationManagerCompat.from(mContext);

        // create a group, so we can get it later
        String groupId = genUniqueId(TYPE_GROUP);
        nm.createNotificationChannelGroup(new NotificationChannelGroupCompat.Builder(groupId)
                .setName("groupName").build());
        // create a channel, so we can get it later
        String channelId = genUniqueId(TYPE_CHANNEL);
        nm.createNotificationChannel(
                new NotificationChannelCompat.Builder(channelId, IMPORTANCE_DEFAULT)
                        .setName("channelName").setGroup(groupId).build());

        // test getNotificationChannelGroup(String)
        NotificationChannelGroupCompat compatGroup = nm.getNotificationChannelGroupCompat(groupId);
        if (Build.VERSION.SDK_INT >= 26) {
            assertNotNull(compatGroup);
            List<NotificationChannelCompat> compatChannels = compatGroup.getChannels();
            assertEquals(1, compatChannels.size());
            assertEquals(channelId, compatChannels.get(0).getId());
            assertTrue(areEqual(compatChannels.get(0),
                    mPlatformNotificationManager.getNotificationChannel(channelId)));
        } else {
            assertNull(compatGroup);
        }

        // test getNotificationChannelGroupsCompat() works as expected
        List<NotificationChannelGroupCompat> compatGroups = nm.getNotificationChannelGroupsCompat();
        if (Build.VERSION.SDK_INT >= 26) {
            List<NotificationChannelGroup> platformGroups =
                    mPlatformNotificationManager.getNotificationChannelGroups();
            assertEquals(platformGroups.size(), compatGroups.size());
            List<NotificationChannelCompat> compatChannels =
                    findGroupCompat(compatGroups, groupId).getChannels();
            assertEquals(1, compatChannels.size());
            assertEquals(channelId, compatChannels.get(0).getId());
            assertTrue(areEqual(compatChannels.get(0),
                    mPlatformNotificationManager.getNotificationChannel(channelId)));
        } else {
            // list should be empty on SDKs which don't support Notification Channels
            assertTrue(compatGroups.isEmpty());
        }
    }

    @Test
    public void testPostBatchNotifications() {
        String channelId = genUniqueId(TYPE_CHANNEL);

        NotificationManager fakeManager = mock(NotificationManager.class);
        NotificationManagerCompat notificationManager =
                new NotificationManagerCompat(fakeManager, mContext);

        final Notification notification =
                new NotificationCompat.Builder(mContext, channelId)
                        .setSmallIcon(1)
                        .build();
        final Notification notification2 =
                new NotificationCompat.Builder(mContext, channelId)
                        .setSmallIcon(1)
                        .build();
        NotificationManagerCompat.NotificationWithIdAndTag n1 =
                new NotificationManagerCompat.NotificationWithIdAndTag("tag1", 1,
                        notification);
        NotificationManagerCompat.NotificationWithIdAndTag n2 =
                new NotificationManagerCompat.NotificationWithIdAndTag(2,
                        notification2);
        List<NotificationManagerCompat.NotificationWithIdAndTag> notifications =
                Arrays.asList(n1, n2);

        notificationManager.notify(notifications);
        // Verifies that mNotificationManager has notify called on it twice, for each notification.
        verify(fakeManager, times(1)).notify("tag1", 1, notification);
        verify(fakeManager, times(1)).notify(null, 2, notification2);
    }

    @Test
    public void testCanUseFullScreenIntent() {
        NotificationManager fakeManager = mock(NotificationManager.class);

        Context spyContext = spy(mContext);

        NotificationManagerCompat notificationManagerCompat =
                new NotificationManagerCompat(fakeManager, spyContext);

        final boolean canUse = notificationManagerCompat.canUseFullScreenIntent();

        if (Build.VERSION.SDK_INT < 29) {
            assertTrue(canUse);

        } else if (Build.VERSION.SDK_INT < 34) {
            verify(spyContext, times(1))
                    .checkSelfPermission(Manifest.permission.USE_FULL_SCREEN_INTENT);
        } else {
            verify(fakeManager, times(1)).canUseFullScreenIntent();
        }
    }

    public void testGetActiveNotifications() {
        NotificationManager fakeManager = mock(NotificationManager.class);
        NotificationManagerCompat notificationManager =
                new NotificationManagerCompat(fakeManager, mContext);

        notificationManager.getActiveNotifications();

        verify(fakeManager, times(1)).getActiveNotifications();
    }

    @Test
    public void testInterruptionFilterConstantCorrespondence() {
        assertEquals(NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
                NotificationManagerCompat.INTERRUPTION_FILTER_UNKNOWN);
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                NotificationManagerCompat.INTERRUPTION_FILTER_ALL);
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                NotificationManagerCompat.INTERRUPTION_FILTER_PRIORITY);
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE,
                NotificationManagerCompat.INTERRUPTION_FILTER_NONE);
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALARMS,
                NotificationManagerCompat.INTERRUPTION_FILTER_ALARMS);
    }

    @Test
    public void testGetCurrentInterruptionFilter() {
        NotificationManagerCompat notificationManager = new NotificationManagerCompat(
                mPlatformNotificationManager,
                mContext);
        assertEquals(notificationManager.getCurrentInterruptionFilter(),
                mPlatformNotificationManager.getCurrentInterruptionFilter());
    }
}