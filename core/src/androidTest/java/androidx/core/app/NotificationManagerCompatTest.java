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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@RunWith(AndroidJUnit4.class)
@MediumTest
public class NotificationManagerCompatTest {

    private static final String TAG = NotificationManagerCompatTest.class.getSimpleName();

    private static final String TYPE_CHANNEL = "channel";
    private static final String TYPE_GROUP = "group";

    private Context mContext;
    private NotificationManager mPlatformNotificationManager;


    /**
     * Generate unique ID for Channels and Groups to prevent conflicts between tests
     * @param type Type of ID. Channel, group or any string
     * @return Unique ID
     */
    private static String genUniqueId(String type) {
        return TAG + "_" + UUID.randomUUID() + "_" + type + "_id";
    }


    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
        mPlatformNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    @AfterClass
    public static void clean() {
        // Delete all channels and groups created during tests

        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager notificationManager = (NotificationManager) InstrumentationRegistry
                .getTargetContext().getSystemService(Context.NOTIFICATION_SERVICE);
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


    @Test
    public void testGetNotificationChannel() {
        String channelId = genUniqueId(TYPE_CHANNEL);
        String channelName = "channelName";
        String channelDescription = "channelDescription";

        // create a channel, so we can get it later
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT);
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
            assertEquals(NotificationManagerCompat.IMPORTANCE_DEFAULT, result.getImportance());
            assertEquals(channelDescription, result.getDescription());
        } else {
            // should be null on SDKs which doesn't support Notification Channels
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

            NotificationChannel channel = new NotificationChannel(groupChannelId, groupChannelName,
                    NotificationManager.IMPORTANCE_LOW);
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
            assertEquals(NotificationManagerCompat.IMPORTANCE_LOW, resultChannel.getImportance());
            assertEquals(groupChannelDescription, resultChannel.getDescription());
        } else {
            // should be null on SDKs which doesn't support Notification Channels
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

        notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                channelName, NotificationManagerCompat.IMPORTANCE_DEFAULT));

        NotificationChannel channel =
                mPlatformNotificationManager.getNotificationChannel(channelId);

        assertNotNull(channel);
        assertEquals(channelId, channel.getId());
        assertEquals(channelName, channel.getName());
        assertEquals(NotificationManagerCompat.IMPORTANCE_DEFAULT, channel.getImportance());
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

        NotificationChannel channel = new NotificationChannel(channelId, channelName,
                NotificationManagerCompat.IMPORTANCE_HIGH);
        channel.setDescription(channelDescription);
        channel.enableLights(true);
        channel.setLightColor(channelLightColor);
        channel.setSound(Uri.EMPTY, audioAttributes);

        notificationManager.createNotificationChannel(channel);

        NotificationChannel result = mPlatformNotificationManager.getNotificationChannel(channelId);
        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals(channelName, result.getName());
        assertEquals(NotificationManagerCompat.IMPORTANCE_HIGH, result.getImportance());
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
    public void testCreateNotificationChannelGroup() {
        String groupId = genUniqueId(TYPE_GROUP);
        String groupName = "groupName";

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        NotificationChannelGroup group = new NotificationChannelGroup(groupId, groupName);
        notificationManager.createNotificationChannelGroup(group);

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
        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                "groupChannelName", NotificationManager.IMPORTANCE_LOW);
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
        assertEquals(notificationChannel, resultChannel);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannels() {
        String channelOneId = genUniqueId(TYPE_CHANNEL);
        String channelTwoId = genUniqueId(TYPE_CHANNEL);
        NotificationChannel channelOne = new NotificationChannel(channelOneId, "oneName",
                NotificationManagerCompat.IMPORTANCE_DEFAULT);
        NotificationChannel channelTwo = new NotificationChannel(channelTwoId, "twoName",
                NotificationManagerCompat.IMPORTANCE_MIN);
        List<NotificationChannel> channels = Arrays.asList(channelOne, channelTwo);

        int channelsBefore = mPlatformNotificationManager.getNotificationChannels().size();

        NotificationManagerCompat.from(mContext).createNotificationChannels(channels);

        // check if channels were created
        List<NotificationChannel> result = mPlatformNotificationManager.getNotificationChannels();
        assertEquals(channelsBefore + channels.size(), result.size());
        assertTrue(result.containsAll(channels));

        // just to be sure
        NotificationChannel channel =
                mPlatformNotificationManager.getNotificationChannel(channelOneId);
        assertNotNull(channel);
        assertEquals(channelOne.getName(), channel.getName());
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.getImportance());

        channel = mPlatformNotificationManager.getNotificationChannel(channelTwoId);
        assertNotNull(channel);
        assertEquals(channelTwo.getName(), channel.getName());
        assertEquals(NotificationManager.IMPORTANCE_MIN, channel.getImportance());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testCreateNotificationChannelGroups() {
        String groupOneId = genUniqueId(TYPE_GROUP);
        String channelGroupOneId = genUniqueId(TYPE_CHANNEL);
        NotificationChannelGroup groupOne = new NotificationChannelGroup(groupOneId,
                "groupOneName");
        NotificationChannel channelGroupOne = new NotificationChannel(channelGroupOneId,
                "channelGroupOneName", NotificationManagerCompat.IMPORTANCE_MIN);
        channelGroupOne.setGroup(groupOneId);

        String channelGroupTwoId = genUniqueId(TYPE_CHANNEL);
        String secondChannelGroupTwoId = genUniqueId(TYPE_CHANNEL);
        NotificationChannelGroup groupTwo = new NotificationChannelGroup(genUniqueId(TYPE_GROUP),
                "groupTwoName");
        NotificationChannel channelGroupTwo = new NotificationChannel(channelGroupTwoId,
                "channelGroupTwoName", NotificationManagerCompat.IMPORTANCE_DEFAULT);
        channelGroupTwo.setGroup(groupTwo.getId());
        NotificationChannel secondChannelGroupTwo = new NotificationChannel(
                secondChannelGroupTwoId, "secondChannelGroupTwoName",
                NotificationManagerCompat.IMPORTANCE_MAX);
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
        assertEquals(channelGroupOne, resultChannel);

        NotificationChannelGroup resultTwo = notificationManager.getNotificationChannelGroup(
                groupTwo.getId());
        assertNotNull(resultTwo);
        assertEquals(groupTwo.getName(), resultTwo.getName());
        //assertEquals(Arrays.asList(channelGroupTwo, secondChannelGroupTwo),
        //        resultTwo.getChannels());

        assertTrue(mPlatformNotificationManager.getNotificationChannels().containsAll(channels));
    }

    @Test
    public void testDeleteNotificationChannel() {
        String channelId = genUniqueId(TYPE_CHANNEL);

        // create a channel, so we can delete it later
        if (Build.VERSION.SDK_INT >= 26) {
            mPlatformNotificationManager.createNotificationChannel(new NotificationChannel(
                    channelId, "channelName", NotificationManager.IMPORTANCE_DEFAULT));
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
    public void testGetNotificationChannels() {
        // create a channel, so we can get it later
        if (Build.VERSION.SDK_INT >= 26) {
            String channelId = genUniqueId(TYPE_CHANNEL);
            mPlatformNotificationManager.createNotificationChannel(new NotificationChannel(
                    channelId, "channelName", NotificationManager.IMPORTANCE_DEFAULT));
            assertNotNull(mPlatformNotificationManager.getNotificationChannel(channelId));
        }

        List<NotificationChannel> channels = NotificationManagerCompat.from(mContext)
                .getNotificationChannels();

        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(mPlatformNotificationManager.getNotificationChannels().size(),
                    channels.size());
            assertEquals(mPlatformNotificationManager.getNotificationChannels(), channels);
        } else {
            // list should be empty on SDKs which doesn't support Notification Channels
            assertTrue(channels.isEmpty());
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
            // list should be empty on SDKs which doesn't support Notification Channels
            assertTrue(groups.isEmpty());
        }
    }

}
