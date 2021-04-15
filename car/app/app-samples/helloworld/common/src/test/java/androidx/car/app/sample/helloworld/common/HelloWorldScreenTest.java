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

package androidx.car.app.sample.helloworld.common;

import static com.google.common.truth.Truth.assertThat;

import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.List;

/**
 * A sample test on {@link HelloWorldScreen}.
 *
 * <p>Demonstrating the usage of {@link TestCarContext} and validating that the returned
 * {@link androidx.car.app.model.Template} has the expected contents.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class HelloWorldScreenTest {
    private final TestCarContext mTestCarContext =
            TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

    @Test
    public void getTemplate_containsExpectedRow() {
        HelloWorldScreen screen = new HelloWorldScreen(mTestCarContext);
        PaneTemplate template = (PaneTemplate) screen.onGetTemplate();

        List<Row> rows = template.getPane().getRows();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).isEqualTo(new Row.Builder().setTitle("Hello AndroidX!").build());
    }
}
