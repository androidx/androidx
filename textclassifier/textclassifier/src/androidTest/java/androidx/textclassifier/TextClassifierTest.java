/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.textclassifier.TextClassifier.EntityConfig.Builder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

/** Instrumentation unit tests for {@link TextClassifier}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextClassifierTest {

    @Test
    public void testEntityConfig_withHints() {
        TextClassifier.EntityConfig entityConfig = new Builder().setHints(
                Arrays.asList("a", "b")).build();

        TextClassifier.EntityConfig entityConfigFromBundle =
                TextClassifier.EntityConfig.createFromBundle(entityConfig.toBundle());

        assertThat(entityConfig.getHints()).containsExactly("a", "b");
        assertThat(entityConfig.shouldIncludeTypesFromTextClassifier()).isTrue();
        assertThat(entityConfigFromBundle.resolveTypes(Arrays.asList("default")))
                .containsExactly("default");
    }

    @Test
    public void testEntityConfig_withIncludedExcludedAndHints() {
        TextClassifier.EntityConfig entityConfig = new Builder()
                .setHints(Arrays.asList("hints"))
                .setIncludedTypes(Arrays.asList("included", "overlap"))
                .setExcludedTypes(Arrays.asList("excluded", "overlap"))
                .build();

        TextClassifier.EntityConfig entityConfigFromBundle =
                TextClassifier.EntityConfig.createFromBundle(entityConfig.toBundle());

        assertThat(entityConfigFromBundle.getHints()).containsExactly("hints");
        assertThat(entityConfigFromBundle.shouldIncludeTypesFromTextClassifier()).isTrue();
        assertThat(entityConfigFromBundle.resolveTypes(Arrays.asList("default", "excluded")))
                .containsExactly("default", "included");
    }

    @Test
    public void testEntityConfig_setUseDefaultEntityTypes() {
        TextClassifier.EntityConfig entityConfig = new Builder()
                .includeTypesFromTextClassifier(false)
                .setIncludedTypes(Arrays.asList("included"))
                .build();

        TextClassifier.EntityConfig entityConfigFromBundle =
                TextClassifier.EntityConfig.createFromBundle(entityConfig.toBundle());

        assertThat(entityConfig.getHints()).isEmpty();
        assertThat(entityConfig.shouldIncludeTypesFromTextClassifier()).isFalse();
        assertThat(entityConfigFromBundle.resolveTypes(
                Arrays.asList("default"))).containsExactly("included");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testEntityConfig_toPlatform_explicit() {
        TextClassifier.EntityConfig entityConfig = new Builder()
                .includeTypesFromTextClassifier(false)
                .setIncludedTypes(Arrays.asList("included", "excluded"))
                .setExcludedTypes(Arrays.asList("excluded"))
                .build();

        android.view.textclassifier.TextClassifier.EntityConfig platformEntityConfig =
                entityConfig.toPlatform();

        assertThat(platformEntityConfig.getHints()).isEmpty();
        assertThat(platformEntityConfig.resolveEntityListModifications(Arrays.asList("extra")))
                .containsExactly("included");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testEntityConfig_toPlatform_withDefault() {
        TextClassifier.EntityConfig entityConfig = new Builder()
                .setIncludedTypes(Arrays.asList("included", "excluded"))
                .setExcludedTypes(Arrays.asList("excluded"))
                .setHints(Arrays.asList("hint"))
                .build();

        android.view.textclassifier.TextClassifier.EntityConfig platformEntityConfig =
                entityConfig.toPlatform();

        assertThat(platformEntityConfig.getHints()).containsExactly("hint");
        assertThat(platformEntityConfig.resolveEntityListModifications(Arrays.asList("extra")))
                .containsExactly("included", "extra");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testEntityConfig_fromPlatform_createWithHints() {
        android.view.textclassifier.TextClassifier.EntityConfig platformEntityConfig =
                android.view.textclassifier.TextClassifier.EntityConfig.createWithHints(
                        Collections.singletonList("hints"));

        TextClassifier.EntityConfig entityConfig =
                TextClassifier.EntityConfig.fromPlatform(platformEntityConfig);


        assertThat(entityConfig.getHints()).containsExactly("hints");
        assertThat(entityConfig.shouldIncludeTypesFromTextClassifier()).isTrue();
        assertThat(entityConfig.resolveTypes(Collections.singleton("default")))
                .containsExactly("default");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testEntityConfig_fromPlatform_createWithExplicitEntityList() {
        android.view.textclassifier.TextClassifier.EntityConfig platformEntityConfig =
                android.view.textclassifier.TextClassifier.EntityConfig
                        .createWithExplicitEntityList(
                                Collections.singletonList("explicit"));

        TextClassifier.EntityConfig entityConfig =
                TextClassifier.EntityConfig.fromPlatform(platformEntityConfig);


        assertThat(entityConfig.getHints()).isEmpty();
        assertThat(entityConfig.shouldIncludeTypesFromTextClassifier()).isFalse();
        assertThat(entityConfig.resolveTypes(Collections.singleton("default")))
                .containsExactly("explicit");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testEntityConfig_fromPlatform_create() {
        android.view.textclassifier.TextClassifier.EntityConfig platformEntityConfig =
                android.view.textclassifier.TextClassifier.EntityConfig.create(
                        Collections.singleton("hints"),
                        Collections.singleton("included"),
                        Collections.singleton("excluded"));

        TextClassifier.EntityConfig entityConfig =
                TextClassifier.EntityConfig.fromPlatform(platformEntityConfig);

        assertThat(entityConfig.getHints()).containsExactly("hints");
        assertThat(entityConfig.shouldIncludeTypesFromTextClassifier()).isTrue();
        assertThat(entityConfig.resolveTypes(Arrays.asList("default", "excluded")))
                .containsExactly("default", "included");
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testEntityConfig_fromPlatform_builder() {
        android.view.textclassifier.TextClassifier.EntityConfig platformEntityConfig =
                new android.view.textclassifier.TextClassifier.EntityConfig.Builder()
                .setIncludedTypes(Collections.singleton("included"))
                .setExcludedTypes(Collections.singleton("excluded"))
                .setHints(Collections.singleton("hints"))
                .build();

        TextClassifier.EntityConfig entityConfig =
                TextClassifier.EntityConfig.fromPlatform(platformEntityConfig);

        assertThat(entityConfig.getHints()).containsExactly("hints");
        assertThat(entityConfig.shouldIncludeTypesFromTextClassifier()).isTrue();
        assertThat(entityConfig.resolveTypes(Arrays.asList("default", "excluded")))
                .containsExactly("default", "included");
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testEntityConfig_fromPlatform_bundle() {
        android.view.textclassifier.TextClassifier.EntityConfig platformEntityConfig =
                android.view.textclassifier.TextClassifier.EntityConfig.create(
                        Collections.singleton("hints"),
                        Collections.singleton("included"),
                        Collections.singleton("excluded"));

        TextClassifier.EntityConfig entityConfig =
                TextClassifier.EntityConfig.createFromBundle(
                        TextClassifier.EntityConfig.fromPlatform(platformEntityConfig).toBundle());

        assertThat(entityConfig.getHints()).containsExactly("hints");
        assertThat(entityConfig.shouldIncludeTypesFromTextClassifier()).isTrue();
        assertThat(entityConfig.resolveTypes(Arrays.asList("default", "excluded")))
                .containsExactly("default", "included");
    }
}
