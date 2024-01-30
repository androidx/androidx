/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.mediarouter.media;

import static androidx.mediarouter.media.MediaRouter2Utils.KEY_CONTROL_FILTERS;
import static androidx.mediarouter.media.MediaRouter2Utils.KEY_DEVICE_TYPE;
import static androidx.mediarouter.media.MediaRouter2Utils.KEY_EXTRAS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.media.MediaRoute2Info;
import android.os.Build;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;

/** Test for {@link MediaRouter2Utils}. */
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(AndroidJUnit4.class)
public class MediaRouter2UtilsTest {

    private static final String FAKE_MEDIA_ROUTE_DESCRIPTOR_ID = "fake_id";
    private static final String FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME = "fake_name";
    /**
     * Placeholder bundle for {@link MediaRouter2Utils#toMediaRouteDescriptor} to accept passed
     * {@link MediaRoute2Info} instances as valid.
     */
    private static final Bundle PLACEHOLDER_EXTRAS_BUNDLE;

    static {
        PLACEHOLDER_EXTRAS_BUNDLE = new Bundle();
        PLACEHOLDER_EXTRAS_BUNDLE.putBundle(KEY_EXTRAS, new Bundle());
        PLACEHOLDER_EXTRAS_BUNDLE.putInt(
                KEY_DEVICE_TYPE, MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN);
        PLACEHOLDER_EXTRAS_BUNDLE.putParcelableArrayList(KEY_CONTROL_FILTERS, new ArrayList<>());
    }

    @Test
    public void toFwkMediaRoute2Info_withValidDescriptor_returnsInstance() {
        MediaRouteDescriptor descriptor =
                new MediaRouteDescriptor.Builder(
                                FAKE_MEDIA_ROUTE_DESCRIPTOR_ID, FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME)
                        .build();
        MediaRoute2Info mediaRoute2Info = MediaRouter2Utils.toFwkMediaRoute2Info(descriptor);
        assertEquals(FAKE_MEDIA_ROUTE_DESCRIPTOR_ID, mediaRoute2Info.getId());
        assertEquals(FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME, mediaRoute2Info.getName());
    }

    @Test
    public void toFwkMediaRoute2Info_withEmptyIdOrName_returnsNull() {
        MediaRouteDescriptor descriptorWithEmptyId =
                new MediaRouteDescriptor.Builder(/* id= */ "", FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME)
                        .build();
        assertNull(MediaRouter2Utils.toFwkMediaRoute2Info(descriptorWithEmptyId));
        MediaRouteDescriptor descriptorWithEmptyName =
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_DESCRIPTOR_ID, /* name= */ "")
                        .build();
        assertNull(MediaRouter2Utils.toFwkMediaRoute2Info(descriptorWithEmptyName));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    @Test
    public void toFwkMediaRoute2Info_withDeduplicationIds() {
        HashSet<String> dedupIds = new HashSet<>();
        dedupIds.add("dedup_id1");
        dedupIds.add("dedup_id2");
        MediaRouteDescriptor descriptor =
                new MediaRouteDescriptor.Builder(
                                FAKE_MEDIA_ROUTE_DESCRIPTOR_ID, FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME)
                        .setDeduplicationIds(dedupIds)
                        .build();
        assertTrue(
                MediaRouter2Utils.toFwkMediaRoute2Info(descriptor)
                        .getDeduplicationIds()
                        .equals(dedupIds));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    @Test
    public void toMediaRouteDescriptor_withDeduplicationIds() {
        HashSet<String> dedupIds = new HashSet<>();
        dedupIds.add("dedup_id1");
        dedupIds.add("dedup_id2");

        MediaRoute2Info routeInfo =
                new MediaRoute2Info.Builder(
                                FAKE_MEDIA_ROUTE_DESCRIPTOR_ID, FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME)
                        .addFeature(MediaRoute2Info.FEATURE_REMOTE_PLAYBACK)
                        .setDeduplicationIds(dedupIds)
                        .setExtras(PLACEHOLDER_EXTRAS_BUNDLE)
                        .build();
        assertTrue(
                MediaRouter2Utils.toMediaRouteDescriptor(routeInfo)
                        .getDeduplicationIds()
                        .equals(dedupIds));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    @Test
    public void toMediaRouteDescriptor_withDeviceType_setsCorrectType() {
        MediaRoute2Info routeInfo =
                new MediaRoute2Info.Builder(
                                FAKE_MEDIA_ROUTE_DESCRIPTOR_ID, FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME)
                        .addFeature(MediaRoute2Info.FEATURE_REMOTE_PLAYBACK)
                        .setType(MediaRoute2Info.TYPE_REMOTE_AUDIO_VIDEO_RECEIVER)
                        .setExtras(PLACEHOLDER_EXTRAS_BUNDLE)
                        .build();

        assertEquals(
                MediaRouter.RouteInfo.DEVICE_TYPE_AUDIO_VIDEO_RECEIVER,
                MediaRouter2Utils.toMediaRouteDescriptor(routeInfo).getDeviceType());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    @Test
    public void toFwkMediaRoute2Info_withType_setsCorrectDeviceType() {
        MediaRouteDescriptor descriptor =
                new MediaRouteDescriptor.Builder(
                                FAKE_MEDIA_ROUTE_DESCRIPTOR_ID, FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME)
                        .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_TV)
                        .build();
        assertEquals(
                MediaRoute2Info.TYPE_REMOTE_TV,
                MediaRouter2Utils.toFwkMediaRoute2Info(descriptor).getType());
    }
}
