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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.media.MediaRoute2Info;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Test for {@link MediaRouter2Utils}. */
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(AndroidJUnit4.class)
public class MediaRouter2UtilsTest {

    private static final String FAKE_MEDIA_ROUTE_DESCRIPTOR_ID = "fake_id";
    private static final String FAKE_MEDIA_ROUTE_DESCRIPTOR_NAME = "fake_name";

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
}
