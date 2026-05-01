/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public final class SectionHeaderTest {
    private final CarIcon mIcon = TestUtils.getTestCarIcon(
            ApplicationProvider.getApplicationContext(), "ic_test_1");

    @Test
    public void build_withTitle() {
        String title = "title";
        SectionHeader header = new SectionHeader.Builder(title).build();
        assertThat(header.getTitle()).isNotNull();
        assertThat(header.getTitle().toString()).isEqualTo(title);
        assertThat(header.getSubtitle()).isNull();
    }

    @Test
    public void build_withSubtitle() {
        String title = "title";
        String subtitle = "subtitle";
        SectionHeader header = new SectionHeader.Builder(title)
                .setSubtitle(subtitle)
                .build();
        assertThat(header.getSubtitle()).isNotNull();
        assertThat(header.getSubtitle().toString()).isEqualTo(subtitle);
    }

    @Test
    public void build_withCarTextSubtitle() {
        String title = "title";
        CarText subtitle = CarText.create("subtitle");
        SectionHeader header = new SectionHeader.Builder(title)
                .setSubtitle(subtitle)
                .build();
        assertThat(header.getSubtitle()).isNotNull();
        assertThat(header.getSubtitle()).isEqualTo(subtitle);
    }

    @Test
    public void setSubtitle_null() {
        SectionHeader header = new SectionHeader.Builder("title")
                .setSubtitle("subtitle")
                .setSubtitle((CharSequence) null)
                .build();
        assertThat(header.getSubtitle()).isNull();

        header = new SectionHeader.Builder("title")
                .setSubtitle(CarText.create("subtitle"))
                .setSubtitle((CarText) null)
                .build();
        assertThat(header.getSubtitle()).isNull();
    }

    @Test
    public void build_emptyTitle_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SectionHeader.Builder(""));
    }

    @Test
    public void setStartIcon() {
        SectionHeader header = new SectionHeader.Builder("title")
                .setStartIcon(mIcon, SectionHeader.IMAGE_TYPE_LARGE)
                .build();
        assertThat(header.getStartIcon()).isEqualTo(mIcon);
        assertThat(header.getStartIconType()).isEqualTo(SectionHeader.IMAGE_TYPE_LARGE);
    }

    @Test
    public void setStartIcon_defaultType() {
        SectionHeader header = new SectionHeader.Builder("title")
                .setStartIcon(mIcon)
                .build();
        assertThat(header.getStartIcon()).isEqualTo(mIcon);
        assertThat(header.getStartIconType()).isEqualTo(SectionHeader.IMAGE_TYPE_SMALL);
    }

    @Test
    public void setStartIcon_nonCustomIcon_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SectionHeader.Builder("title").setStartIcon(CarIcon.BACK));
        assertThrows(IllegalArgumentException.class,
                () -> new SectionHeader.Builder("title").setStartIcon(CarIcon.BACK,
                        SectionHeader.IMAGE_TYPE_LARGE));
    }

    @Test
    public void setEndIcon() {
        SectionHeader header = new SectionHeader.Builder("title")
                .setEndIcon(mIcon)
                .build();
        assertThat(header.getEndIcon()).isEqualTo(mIcon);
    }

    @Test
    public void setEndIcon_nonCustomIcon_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SectionHeader.Builder("title").setEndIcon(CarIcon.BACK));
    }

    @Test
    public void setOnClickListener() {
        OnClickListener listener = () -> { };
        SectionHeader header = new SectionHeader.Builder("title")
                .setOnClickListener(listener)
                .build();
        assertThat(header.getOnClickDelegate()).isNotNull();
    }

    @Test
    public void equals() {
        OnClickListener listener = () -> { };
        SectionHeader header1 = new SectionHeader.Builder("title")
                .setSubtitle("subtitle")
                .setStartIcon(mIcon, SectionHeader.IMAGE_TYPE_SMALL)
                .setEndIcon(mIcon)
                .setOnClickListener(listener)
                .build();
        SectionHeader header2 = new SectionHeader.Builder("title")
                .setSubtitle("subtitle")
                .setStartIcon(mIcon, SectionHeader.IMAGE_TYPE_SMALL)
                .setEndIcon(mIcon)
                .setOnClickListener(listener)
                .build();

        assertThat(header1).isEqualTo(header2);
    }

    @Test
    public void notEquals_differentTitle() {
        SectionHeader header1 = new SectionHeader.Builder("title1").build();
        SectionHeader header2 = new SectionHeader.Builder("title2").build();
        assertThat(header1).isNotEqualTo(header2);
    }

    @Test
    public void notEquals_differentSubtitle() {
        SectionHeader header1 = new SectionHeader.Builder("title")
                .setSubtitle("subtitle1")
                .build();
        SectionHeader header2 = new SectionHeader.Builder("title")
                .setSubtitle("subtitle2")
                .build();
        assertThat(header1).isNotEqualTo(header2);
    }

    @Test
    public void notEquals_differentStartIcon() {
        SectionHeader header1 = new SectionHeader.Builder("title")
                .setStartIcon(mIcon, SectionHeader.IMAGE_TYPE_SMALL)
                .build();
        SectionHeader header2 = new SectionHeader.Builder("title").build();
        assertThat(header1).isNotEqualTo(header2);
    }

    @Test
    public void notEquals_differentEndIcon() {
        SectionHeader header1 = new SectionHeader.Builder("title")
                .setEndIcon(mIcon)
                .build();
        SectionHeader header2 = new SectionHeader.Builder("title").build();
        assertThat(header1).isNotEqualTo(header2);
    }

    @Test
    public void notEquals_differentStartIconType() {
        SectionHeader header1 = new SectionHeader.Builder("title")
                .setStartIcon(mIcon, SectionHeader.IMAGE_TYPE_SMALL)
                .build();
        SectionHeader header2 = new SectionHeader.Builder("title")
                .setStartIcon(mIcon, SectionHeader.IMAGE_TYPE_LARGE)
                .build();
        assertThat(header1).isNotEqualTo(header2);
    }

    @Test
    public void notEquals_differentOnClickListener() {
        SectionHeader header1 = new SectionHeader.Builder("title")
                .setOnClickListener(() -> { })
                .build();
        SectionHeader header2 = new SectionHeader.Builder("title").build();
        assertThat(header1).isNotEqualTo(header2);
    }
}
