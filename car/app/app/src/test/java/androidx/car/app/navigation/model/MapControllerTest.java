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

package androidx.car.app.navigation.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.model.ActionStrip;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link MapController}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class MapControllerTest {
    private final ActionStrip mActionStrip = new ActionStrip.Builder()
            .addAction(TestUtils.createAction("test", null))
            .build();
    private final ActionStrip mMapActionStrip = new ActionStrip.Builder()
            .addAction(TestUtils.createAction(null,
                    TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                            "ic_test_1")))
            .build();

    @Test
    public void textButtonInMapActionStrip_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new MapController.Builder().setMapActionStrip(mActionStrip));
    }

    @Test
    public void createEmpty() {
        MapController component = new MapController.Builder().build();
        assertThat(component.getPanModeDelegate()).isNull();
        assertThat(component.getMapActionStrip()).isNull();
    }

    @Test
    public void createInstance() {
        MapController component = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        assertThat(component.getMapActionStrip()).isEqualTo(mMapActionStrip);
    }

    @Test
    public void equals() {
        MapController component = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .setPanModeListener((panModechanged) -> {
                })
                .build();

        assertThat(component).isEqualTo(new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .setPanModeListener((panModechanged) -> {
                })
                .build());
    }

    @Test
    public void notEquals_differentMapActionStrip() {
        MapController component = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();

        assertThat(component).isNotEqualTo(new MapController.Builder()
                .setMapActionStrip(new ActionStrip.Builder()
                        .addAction(TestUtils.createAction(null, TestUtils.getTestCarIcon(
                                ApplicationProvider.getApplicationContext(), "ic_test_2")))
                        .build())
                .build());
    }

    @Test
    public void notEquals_panModeListenerChange() {
        MapController component = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .setPanModeListener((panModechanged) -> {
                })
                .build();

        assertThat(component).isNotEqualTo(new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build());
    }
}
