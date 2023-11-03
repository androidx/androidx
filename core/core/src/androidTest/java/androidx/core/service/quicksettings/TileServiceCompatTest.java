/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.service.quicksettings;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for {@link TileServiceCompat}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TileServiceCompatTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        TileServiceCompat.clearTileServiceWrapper();
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    public void startActivityAndCollapse_usesPendingIntent() {
        TileServiceCompat.TileServiceWrapper tileServiceWrapper =
                mock(TileServiceCompat.TileServiceWrapper.class);
        TileService tileService = mock(TileService.class);
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        PendingIntentActivityWrapper wrapper = new PendingIntentActivityWrapper(mContext,
                requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT, options, /* isMutable = */
                false);
        TileServiceCompat.setTileServiceWrapper(tileServiceWrapper);

        TileServiceCompat.startActivityAndCollapse(tileService, wrapper);

        verify(tileServiceWrapper).startActivityAndCollapse(wrapper.getPendingIntent());
    }

    @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 33)
    @Test
    public void startActivityAndCollapse_usesIntent() {
        TileServiceCompat.TileServiceWrapper tileServiceWrapper =
                mock(TileServiceCompat.TileServiceWrapper.class);
        TileService tileService = mock(TileService.class);
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        PendingIntentActivityWrapper wrapper = new PendingIntentActivityWrapper(mContext,
                requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT, options, /* isMutable = */
                true);
        TileServiceCompat.setTileServiceWrapper(tileServiceWrapper);

        TileServiceCompat.startActivityAndCollapse(tileService, wrapper);

        verify(tileServiceWrapper).startActivityAndCollapse(intent);
    }
}
