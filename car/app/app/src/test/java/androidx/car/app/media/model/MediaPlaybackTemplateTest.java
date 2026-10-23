/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.car.app.media.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.StrokeCap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** tests for {@link MediaPlaybackTemplate}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class MediaPlaybackTemplateTest {

    public static final Header HEADER = new Header.Builder()
            .setTitle("header title")
            .setStartHeaderAction(Action.BACK)
            .addEndHeaderAction(new Action.Builder().setTitle("header end action title").build())
            .build();

    @Test
    public void createInstance_emptyTemplate_isValid() {
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder().build();

        assertEquals(template.getHeader(), null);
        assertEquals(template.getStyle(), null);
        assertFalse(template.isVideoEnabled());
        assertFalse(template.isAutoHideControlsEnabled());
    }

    @Test
    public void createInstance_nullHeader_isValid() {
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder().setHeader(null).build();

        assertEquals(template.getHeader(), null);
    }

    @Test
    public void createInstance_headerProvided_isValid() {
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder().setHeader(HEADER).build();

        assertEquals(template.getHeader(), HEADER);
    }

    @Test
    public void createInstance_styleProvided_isValid() {
        MediaPlaybackStyle style =
                new MediaPlaybackStyle.Builder()
                        .setProgressBarStrokeCap(StrokeCap.ROUND)
                        .build();
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder().setStyle(style).build();

        assertEquals(template.getStyle(), style);
    }

    @Test
    public void createInstance_videoEnabledProvided_isValid() {
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder().setVideoEnabled(true).build();

        assertTrue(template.isVideoEnabled());
    }

    @Test
    public void createInstance_autoHideControlsEnabledProvided_isValid() {
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder()
                        .setAutoHideControlsEnabled(true)
                        .build();

        assertTrue(template.isAutoHideControlsEnabled());
    }

    @Test
    public void equals() {
        MediaPlaybackTemplate template1 =
                new MediaPlaybackTemplate.Builder().setHeader(HEADER).build();

        MediaPlaybackTemplate template2 =
                new MediaPlaybackTemplate.Builder().setHeader(HEADER).build();

        assertEquals(template1, template2);
    }

    @Test
    public void equals_defaultInstances() {
        MediaPlaybackTemplate template1 =
                new MediaPlaybackTemplate.Builder().build();

        MediaPlaybackTemplate template2 =
                new MediaPlaybackTemplate.Builder().build();

        assertEquals(template1, template2);
    }

    @Test
    public void notEquals_differentHeaders() {
        MediaPlaybackTemplate template1 =
                new MediaPlaybackTemplate.Builder().setHeader(HEADER).build();

        MediaPlaybackTemplate template2 =
                new MediaPlaybackTemplate.Builder().setHeader(null).build();

        assertNotEquals(template1, template2);
    }

    @Test
    public void notEquals_differentStyles() {
        MediaPlaybackStyle style1 =
                new MediaPlaybackStyle.Builder()
                        .setProgressBarStrokeCap(StrokeCap.ROUND)
                        .build();

        MediaPlaybackStyle style2 =
                new MediaPlaybackStyle.Builder()
                        .setProgressBarStrokeCap(StrokeCap.SQUARE)
                        .build();

        MediaPlaybackTemplate template1 =
                new MediaPlaybackTemplate.Builder().setStyle(style1).build();
        MediaPlaybackTemplate template2 =
                new MediaPlaybackTemplate.Builder().setStyle(style2).build();

        assertNotEquals(template1, template2);
    }

    @Test
    public void copyBuilder_copiesStyle() {
        MediaPlaybackStyle style =
                new MediaPlaybackStyle.Builder()
                        .setProgressBarStrokeCap(StrokeCap.SQUARE)
                        .build();
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder().setStyle(style).build();
        MediaPlaybackTemplate copy =
                new MediaPlaybackTemplate.Builder(template).build();

        assertEquals(template, copy);
        assertEquals(copy.getStyle(), style);
    }

    @Test
    public void notEquals_differentVideoEnabled() {
        MediaPlaybackTemplate template1 =
                new MediaPlaybackTemplate.Builder().setVideoEnabled(true).build();
        MediaPlaybackTemplate template2 =
                new MediaPlaybackTemplate.Builder().setVideoEnabled(false).build();

        assertNotEquals(template1, template2);
    }

    @Test
    public void notEquals_differentAutoHideControlsEnabled() {
        MediaPlaybackTemplate template1 =
                new MediaPlaybackTemplate.Builder()
                        .setAutoHideControlsEnabled(true)
                        .build();
        MediaPlaybackTemplate template2 =
                new MediaPlaybackTemplate.Builder()
                        .setAutoHideControlsEnabled(false)
                        .build();

        assertNotEquals(template1, template2);
    }

    @Test
    public void copyBuilder_copiesVideoSettings() {
        MediaPlaybackTemplate template =
                new MediaPlaybackTemplate.Builder()
                        .setVideoEnabled(true)
                        .setAutoHideControlsEnabled(true)
                        .build();
        MediaPlaybackTemplate copy =
                new MediaPlaybackTemplate.Builder(template).build();

        assertEquals(template, copy);
        assertTrue(copy.isVideoEnabled());
        assertTrue(copy.isAutoHideControlsEnabled());
    }
}
