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

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.app.NotificationChannel;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationChannelCompat.Builder;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationChannelCompatTest {

    /**
     * Test that the NotificationChannel.equals() method counts as equal the minimally-defined
     * channel using the platform API and the Builder.  Also tests visible fields on the Compat
     * object against the platform object.
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testDefaultBuilderEqualsPlatform() {
        String channelId = "channelId";
        String channelName = "channelName";

        NotificationChannel platformChannel =
                new NotificationChannel(channelId, channelName, IMPORTANCE_HIGH);

        Builder builder = new Builder(channelId, IMPORTANCE_HIGH)
                .setName(channelName);

        NotificationChannelCompat compatChannel = builder.build();
        TestHelper.assertChannelEquals(platformChannel, compatChannel);

        NotificationChannel builderChannel = compatChannel.getNotificationChannel();
        assertEquals(platformChannel, builderChannel);

        // Test that the toBuilder().build() cycle causes all editable fields to remain equal.
        NotificationChannelCompat rebuilt = compatChannel.toBuilder().build();
        assertNotSame(compatChannel, rebuilt);
        TestHelper.assertChannelEquals(platformChannel, rebuilt);
    }

    /**
     * Test that the NotificationChannel.equals() method counts as equal the maximally-defined
     * channel using the platform API and the Builder.  Also tests visible fields on the Compat
     * object against the platform object.
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testFullyDefinedBuilderEqualsPlatform() {
        String channelId = "channelId";
        String groupId = "groupId";
        String parentChannelId = "parentChannelId";
        String shortcutId = "shortcutId";
        String channelName = "channelName";
        String channelDescription = "channelDescription";
        int lightColor = 1234567;
        Uri soundUri = Uri.parse("http://foo.com/sound");
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .build();
        long[] vibrationPattern = {100, 200, 100, 100};

        NotificationChannel platformChannel =
                new NotificationChannel(channelId, channelName, IMPORTANCE_HIGH);
        platformChannel.setGroup(groupId);
        platformChannel.setDescription(channelDescription);
        platformChannel.setLightColor(lightColor);
        platformChannel.enableLights(true);
        platformChannel.setShowBadge(false);
        platformChannel.setSound(soundUri, audioAttributes);
        platformChannel.setVibrationPattern(vibrationPattern);
        if (Build.VERSION.SDK_INT >= 30) {
            platformChannel.setConversationId(parentChannelId, shortcutId);
        }

        Builder builder = new Builder(channelId, IMPORTANCE_HIGH)
                .setName(channelName)
                .setGroup(groupId)
                .setDescription(channelDescription)
                .setLightColor(lightColor)
                .setLightsEnabled(true)
                .setShowBadge(false)
                .setSound(soundUri, audioAttributes)
                .setVibrationPattern(vibrationPattern);
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setConversationId(parentChannelId, shortcutId);
        }

        NotificationChannelCompat compatChannel = builder.build();
        TestHelper.assertChannelEquals(platformChannel, compatChannel);

        NotificationChannel builderChannel = compatChannel.getNotificationChannel();
        assertEquals(platformChannel, builderChannel);

        // Test that the toBuilder().build() cycle causes all editable fields to remain equal.
        NotificationChannelCompat rebuilt = compatChannel.toBuilder().build();
        assertNotSame(compatChannel, rebuilt);
        TestHelper.assertChannelEquals(platformChannel, rebuilt);
    }

    /*
     * On older versions of the OS that do not have the NotificationChannel class,
     * loading this test class with a method that has NotificationChannel in its parameters
     * signature will throw a NoClassDefFoundError at runtime.
     * To work around this, we use a separate inner class that provides static helper methods
     */
    static class TestHelper {
        static boolean listContains(Collection<NotificationChannel> containsAll,
                Collection<NotificationChannel> containedList) {
            boolean contains = false;
            for (NotificationChannel nc : containedList) {
                for (NotificationChannel member : containsAll) {
                    contains |= areEqual(nc, member);
                }
                if (!contains) {
                    return false;
                }
                contains = false;
            }
            return true;
        }

        static boolean listContainsCompat(Collection<NotificationChannel> containsAll,
                Collection<NotificationChannelCompat> containedList) {
            boolean contains = false;
            for (NotificationChannelCompat nc : containedList) {
                for (NotificationChannel member : containsAll) {
                    contains |= areEqual(nc, member);
                }
                if (!contains) {
                    return false;
                }
                contains = false;
            }
            return true;
        }

        static boolean areEqual(NotificationChannel nc1, NotificationChannel nc2) {
            boolean equality = nc1.getImportance() == nc2.getImportance()
                    && nc1.canBypassDnd() == nc2.canBypassDnd()
                    && nc1.getLockscreenVisibility() == nc2.getLockscreenVisibility()
                    && nc1.getLightColor() == nc2.getLightColor()
                    && Objects.equals(nc1.getId(), nc2.getId())
                    && Objects.equals(nc1.getName(), nc2.getName())
                    && Objects.equals(nc1.getDescription(), nc2.getDescription())
                    && Objects.equals(nc1.getSound(), nc2.getSound())
                    && Arrays.equals(nc1.getVibrationPattern(), nc2.getVibrationPattern())
                    && Objects.equals(nc1.getGroup(), nc2.getGroup())
                    && Objects.equals(nc1.getAudioAttributes(), nc2.getAudioAttributes());
            if (Build.VERSION.SDK_INT >= 30) {
                equality = equality
                        && Objects.equals(nc1.getParentChannelId(), nc2.getParentChannelId())
                        && Objects.equals(nc1.getConversationId(), nc2.getConversationId());
            }
            return equality;
        }

        static boolean areEqual(NotificationChannelCompat nc1, NotificationChannel nc2) {
            return areEqual(nc1.getNotificationChannel(), nc2);
        }

        static void assertChannelEquals(NotificationChannel expected,
                NotificationChannelCompat actual) {
            assertEquals(expected.getImportance(), actual.getImportance());
            assertEquals(expected.getLightColor(), actual.getLightColor());
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getDescription(), actual.getDescription());
            assertEquals(expected.getSound(), actual.getSound());
            assertArrayEquals(expected.getVibrationPattern(), actual.getVibrationPattern());
            assertEquals(expected.getGroup(), actual.getGroup());
            assertEquals(expected.getAudioAttributes(), actual.getAudioAttributes());
            if (Build.VERSION.SDK_INT >= 30) {
                assertEquals(expected.getParentChannelId(), actual.getParentChannelId());
                assertEquals(expected.getConversationId(), actual.getConversationId());
            }
            assertTrue(areEqual(actual, expected));
        }
    }
}
