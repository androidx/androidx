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

import android.app.NotificationChannelGroup;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationChannelGroupCompatTest {

    /**
     * Test that the NotificationChannelGroup.equals() method counts as equal the minimally-defined
     * channel using the platform API and the Builder.  Also tests visible fields on the Compat
     * object against the platform object.
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testDefaultBuilderEqualsPlatform() {
        String groupId = "groupId";
        String groupName = "groupName";

        NotificationChannelGroup platformGroup = new NotificationChannelGroup(groupId, groupName);

        NotificationChannelGroupCompat.Builder builder =
                new NotificationChannelGroupCompat.Builder(groupId)
                        .setName(groupName);

        NotificationChannelGroupCompat groupCompat = builder.build();
        TestHelper.assertGroupEquals(platformGroup, groupCompat);

        NotificationChannelGroup builderGroup = groupCompat.getNotificationChannelGroup();
        assertEquals(platformGroup, builderGroup);
    }

    /**
     * Test that the NotificationChannelGroup.equals() method counts as equal the maximally-defined
     * channel using the platform API and the Builder.  Also tests visible fields on the Compat
     * object against the platform object.
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testFullyDefinedBuilderEqualsPlatform() {
        String groupId = "groupId";
        String groupName = "groupName";
        String groupDescription = "groupDescription";

        NotificationChannelGroup platformGroup = new NotificationChannelGroup(groupId, groupName);
        if (Build.VERSION.SDK_INT >= 28) {
            platformGroup.setDescription(groupDescription);
        }

        NotificationChannelGroupCompat.Builder builder =
                new NotificationChannelGroupCompat.Builder(groupId)
                        .setName(groupName)
                        .setDescription(groupDescription);

        NotificationChannelGroupCompat groupCompat = builder.build();
        TestHelper.assertGroupEquals(platformGroup, groupCompat);

        NotificationChannelGroup builderGroup = groupCompat.getNotificationChannelGroup();
        assertEquals(platformGroup, builderGroup);
    }

    /*
     * On older versions of the OS that do not have the NotificationChannelGroup class,
     * loading this test class with a method that has NotificationChannelGroup in its parameters
     * signature will throw a NoClassDefFoundError at runtime.
     * To work around this, we use a separate inner class that provides static helper methods
     */
    static class TestHelper {
        private static void assertGroupEquals(NotificationChannelGroup expected,
                NotificationChannelGroupCompat actual) {
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getName(), actual.getName());
            if (Build.VERSION.SDK_INT >= 28) {
                assertEquals(expected.getDescription(), actual.getDescription());
            }
        }
    }

}
