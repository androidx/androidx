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

package androidx.car.app.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

/** Tests for {@link Header}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class HeaderTest {
    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(IllegalStateException.class,
                () -> new Header.Builder()
                        .addEndHeaderAction(Action.BACK)
                        .build());

        // Positive cases.
        new Header.Builder()
                .setTitle("Title")
                .build();
        new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .build();
    }

    @Test
    public void createInstance_header_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        CarText title2 = TestUtils.getCarTextVariantsWithColorSpan("Title");
        assertThrows(IllegalArgumentException.class,
                () -> new Header.Builder()
                        .setTitle(title));
        assertThrows(IllegalArgumentException.class,
                () -> new Header.Builder()
                        .setTitle(title2));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title3 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        CarText title4 = TestUtils.getCarTextVariantsWithDistanceAndDurationSpans("Title");
        new Header.Builder()
                .setTitle(title3)
                .build();
        new Header.Builder()
                .setTitle(title4)
                .build();
    }

    @Test
    public void createEmpty() {
        Header component = new Header.Builder()
                .setTitle("Title")
                .build();
        assertThat(component.getTitle().toString()).isEqualTo("Title");
        assertThat(component.getStartHeaderAction()).isNull();
        assertThat(component.getEndHeaderActions()).isEmpty();
    }

    @Test
    public void createInstance() {
        String title = "title";
        Header component = new Header.Builder()
                .setTitle(title)
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(Action.APP_ICON)
                .build();
        assertThat(component.getTitle().toString()).isEqualTo(title);
        assertThat(component.getEndHeaderActions()).isEqualTo(
                Collections.singletonList(Action.APP_ICON));
        assertThat(component.getStartHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_addEndHeaderAction_invalidActionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header.Builder()
                        .setTitle("Title")
                        .addEndHeaderAction(new Action.Builder()
                                .setTitle("Action")
                                .setOnClickListener(() -> {
                                })
                                .build())
                        .build());
    }

    @Test
    public void createInstance_setStartHeaderAction_invalidActionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header.Builder()
                        .setStartHeaderAction(new Action.Builder()
                                .setTitle("Action")
                                .setOnClickListener(() -> {
                                })
                                .build())
                        .build());
    }

    @Test
    public void createInstance_title_variants() {
        CarText title = new CarText.Builder("Very Long Title")
                .addVariant("Short Title")
                .build();

        Header component = new Header.Builder()
                .setTitle(title)
                .build();
        assertThat(component.getTitle()).isNotNull();
        assertThat(component.getTitle().toString()).isEqualTo("Very Long Title");
        assertThat(component.getTitle().getVariants().get(0).toString()).isEqualTo("Short Title");
    }

    @Test
    public void equals() {
        Header component = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(Action.APP_ICON)
                .setTitle("title")
                .build();

        assertThat(component).isEqualTo(new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(Action.APP_ICON)
                .setTitle("title")
                .build());
    }

    @Test
    public void notEquals_differentStartHeaderAction() {
        Header component = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .build();

        assertThat(component).isNotEqualTo(new Header.Builder()
                .setStartHeaderAction(Action.APP_ICON)
                .build());
    }

    @Test
    public void notEquals_differentEndHeaderActions() {
        Header component = new Header.Builder()
                .setStartHeaderAction(Action.PAN)
                .addEndHeaderAction(Action.BACK)
                .build();

        assertThat(component).isNotEqualTo(new Header.Builder()
                .setStartHeaderAction(Action.PAN)
                .addEndHeaderAction(Action.APP_ICON)
                .build());
    }


    @Test
    public void notEquals_differentTitle() {
        Header component = new Header.Builder()
                .setTitle("title")
                .build();

        assertThat(component).isNotEqualTo(new Header.Builder()
                .setTitle("other")
                .build());
    }
}
