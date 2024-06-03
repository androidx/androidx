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
import static org.junit.Assert.assertNotEquals;

import androidx.car.app.model.Action;
import androidx.car.app.model.Header;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** tests for {@link MediaPlaybackTemplate}. */
@RunWith(RobolectricTestRunner.class)
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
}
