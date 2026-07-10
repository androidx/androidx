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

package androidx.text.vertical;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// NOTE: Do not convert this code to Kotlin -- This test suite is intentionally
// created as Java file to ensure interoperability with legacy Java code.
@RunWith(JUnit4.class)
public class RubySpanJavaTest {
    @Test
    public void constructor_allArgs() {
        RubySpan span = new RubySpan("ruby", AnnotationPosition.After, TextOrientation.Upright,
                0.6f);

        assertThat(span.getText().toString()).isEqualTo("ruby");
        assertThat(span.getPosition()).isEqualTo(AnnotationPosition.After);
        assertThat(span.getOrientation()).isEqualTo(TextOrientation.Upright);
        assertThat(span.getTextScale()).isEqualTo(0.6f);
    }

    @Test
    public void constructor_defaultScale() {
        RubySpan span = new RubySpan("ruby", AnnotationPosition.After, TextOrientation.Upright);

        assertThat(span.getText().toString()).isEqualTo("ruby");
        assertThat(span.getPosition()).isEqualTo(AnnotationPosition.After);
        assertThat(span.getOrientation()).isEqualTo(TextOrientation.Upright);
        assertThat(span.getTextScale()).isEqualTo(0.5f);
    }

    @Test
    public void constructor_defaultOrientationAndScale() {
        RubySpan span = new RubySpan("ruby", AnnotationPosition.After);

        assertThat(span.getText().toString()).isEqualTo("ruby");
        assertThat(span.getPosition()).isEqualTo(AnnotationPosition.After);
        assertThat(span.getOrientation()).isEqualTo(TextOrientation.Mixed);
        assertThat(span.getTextScale()).isEqualTo(0.5f);
    }

    @Test
    public void constructor_defaultPositionOrientationAndScale() {
        RubySpan span = new RubySpan("ruby");

        assertThat(span.getText().toString()).isEqualTo("ruby");
        assertThat(span.getPosition()).isEqualTo(AnnotationPosition.Before);
        assertThat(span.getOrientation()).isEqualTo(TextOrientation.Mixed);
        assertThat(span.getTextScale()).isEqualTo(0.5f);
    }
}
