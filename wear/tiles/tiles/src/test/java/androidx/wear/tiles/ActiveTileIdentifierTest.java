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

package androidx.wear.tiles;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import android.content.ComponentName;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowSystemClock;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowSystemClock.class})
@DoNotInstrument
public class ActiveTileIdentifierTest {
    private static final int TILE_ID = 123;
    private static final String PACKAGE_NAME = "fakePackageName";
    private static final String CLASS_NAME = "fakeClassName";
    private static final String FLATTENED_STRING = TILE_ID + ":" + PACKAGE_NAME + "/" + CLASS_NAME;

    @Test
    public void activeTileIdentifier_flattenToString() {
        assertEquals(
                new ActiveTileIdentifier(new ComponentName(PACKAGE_NAME, CLASS_NAME), TILE_ID)
                        .flattenToString(),
                FLATTENED_STRING);
    }

    @Test
    public void activeTileIdentifier_unFlattenFromString() {
        assertThat(ActiveTileIdentifier.unflattenFromString(FLATTENED_STRING).getInstanceId())
                .isEqualTo(TILE_ID);
        assertEquals(
                ActiveTileIdentifier.unflattenFromString(FLATTENED_STRING)
                        .getComponentName()
                        .getPackageName(),
                PACKAGE_NAME);
        assertEquals(
                ActiveTileIdentifier.unflattenFromString(FLATTENED_STRING)
                        .getComponentName()
                        .getClassName(),
                CLASS_NAME);
        assertEquals(
                ActiveTileIdentifier.unflattenFromString(FLATTENED_STRING).flattenToString(),
                FLATTENED_STRING);
    }
}
