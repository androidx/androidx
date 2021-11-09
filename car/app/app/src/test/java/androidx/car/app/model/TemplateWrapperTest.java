/*
 * Copyright 2020 The Android Open Source Project
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link TemplateWrapper}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TemplateWrapperTest {
    @Test
    public void createInstance() {
        ListTemplate template =
                new ListTemplate.Builder().setTitle("Title").setSingleList(
                        new ItemList.Builder().build()).build();
        TemplateWrapper wrapper = TemplateWrapper.wrap(template);
        assertThat(wrapper.getTemplate()).isEqualTo(template);

        wrapper = TemplateWrapper.wrap(template, "1");
        assertThat(wrapper.getTemplate()).isEqualTo(template);
        assertThat(wrapper.getId()).isEqualTo("1");
    }

    @Test
    public void createInstance_thenUpdate() {
        ListTemplate template =
                new ListTemplate.Builder().setTitle("Title").setSingleList(
                        new ItemList.Builder().build()).build();
        ListTemplate template2 =
                new ListTemplate.Builder().setTitle("Title").setSingleList(
                        new ItemList.Builder().build()).build();

        TemplateWrapper wrapper = TemplateWrapper.wrap(template);
        String id = wrapper.getId();
        assertThat(wrapper.getTemplate()).isEqualTo(template);
        assertThat(wrapper.getCurrentTaskStep()).isEqualTo(0);

        wrapper.setTemplate(template2);
        assertThat(wrapper.getTemplate()).isEqualTo(template2);
        assertThat(wrapper.getCurrentTaskStep()).isEqualTo(0);
        assertThat(wrapper.getId()).isEqualTo(id);

        wrapper.setCurrentTaskStep(2);
        assertThat(wrapper.getTemplate()).isEqualTo(template2);
        assertThat(wrapper.getCurrentTaskStep()).isEqualTo(2);
        assertThat(wrapper.getId()).isEqualTo(id);

        wrapper.setRefresh(true);
        assertThat(wrapper.isRefresh()).isTrue();
        assertThat(wrapper.getTemplate()).isEqualTo(template2);
        assertThat(wrapper.getCurrentTaskStep()).isEqualTo(2);
        assertThat(wrapper.getId()).isEqualTo(id);

        wrapper.setId("1");
        assertThat(wrapper.getId()).isEqualTo("1");
    }

    @Test
    public void copyOf() {
        ListTemplate template =
                new ListTemplate.Builder().setTitle("Title").setSingleList(
                        new ItemList.Builder().build()).build();
        TemplateWrapper source = TemplateWrapper.wrap(template, "ID");
        source.setCurrentTaskStep(45);
        source.setRefresh(true);

        TemplateWrapper dest = TemplateWrapper.copyOf(source);
        assertThat(dest.getTemplate()).isEqualTo(template);
        assertThat(dest.getCurrentTaskStep()).isEqualTo(45);
        assertThat(dest.getId()).isEqualTo("ID");
        assertThat(dest.isRefresh()).isTrue();
    }
}
