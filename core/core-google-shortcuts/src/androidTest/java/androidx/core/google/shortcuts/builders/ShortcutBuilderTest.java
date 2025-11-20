/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.google.shortcuts.builders;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.appindex.Indexable;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShortcutBuilderTest {

    @SmallTest
    @Test
    public void testBuildShortcut() {
        Indexable shortcutIndexable = new ShortcutBuilder()
                .setId("id")
                .setShortcutLabel("shortcut label")
                .setShortcutDescription("shortcut description")
                .setUrl("url")
                .setShortcutUrl("shortcut url")
                .build();

        assertThat(shortcutIndexable).isEqualTo(new Indexable.Builder("Shortcut")
                .setId("id")
                .setName("shortcut label")
                .setDescription("shortcut description")
                .setUrl("url")
                .put("shortcutLabel", "shortcut label")
                .put("shortcutDescription", "shortcut description")
                .put("shortcutUrl", "shortcut url")
                .build());
    }

    @SmallTest
    @Test
    public void testBuildShortcutWithCapability() throws Exception {
        Indexable shortcutIndexable = new ShortcutBuilder()
                .setCapability(
                        new CapabilityBuilder().setName("capability1"),
                        new CapabilityBuilder().setName("capability2"))
                .build();

        assertThat(shortcutIndexable).isEqualTo(new Indexable.Builder("Shortcut")
                .put("capability",
                        new Indexable.Builder("Capability").setName("capability1").build(),
                        new Indexable.Builder("Capability").setName("capability2").build())
                .build());
    }

    @SmallTest
    @Test
    public void testBuildShortcutWithCapabilityAndParameter() throws Exception {
        Indexable shortcutIndexable = new ShortcutBuilder()
                .setCapability(new CapabilityBuilder()
                        .setName("capability")
                        .setParameter(
                                new ParameterBuilder()
                                        .setName("parameter1")
                                        .setValue("value1", "value2"),
                                new ParameterBuilder()
                                        .setName("parameter2")
                                        .setValue("value3", "value4")))
                .build();

        assertThat(shortcutIndexable).isEqualTo(new Indexable.Builder("Shortcut")
                .put("capability", new Indexable.Builder("Capability")
                        .setName("capability")
                        .put("parameter",
                                new Indexable.Builder("Parameter")
                                        .setName("parameter1")
                                        .put("value", "value1", "value2")
                                        .build(),
                                new Indexable.Builder("Parameter")
                                        .setName("parameter2")
                                        .put("value", "value3", "value4")
                                        .build())
                        .build())
                .build());
    }
}
