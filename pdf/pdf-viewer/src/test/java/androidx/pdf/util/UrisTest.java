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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Unit tests for {@link Uris}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class UrisTest {

    @Test
    public void testExtractFileName() {
        assertThat(Uris.extractFileName(Uri.parse("http://example.com/bigtable.pdf")))
                .isEqualTo("bigtable.pdf");
        assertThat(Uris.extractFileName(Uri.parse("http://example.com")))
                .isEqualTo("http://example.com");
    }

    @Test
    public void testHttp() {
        Uri http = Uri.parse("http://example.com/bigtable.pdf");
        assertThat(Uris.isHttp(http)).isTrue();
        assertThat(Uris.isHttps(http)).isFalse();
        assertThat(Uris.isRemote(http)).isTrue();
        assertThat(Uris.isLocal(http)).isFalse();
        assertThat(Uris.extractFileName(http)).isEqualTo("bigtable.pdf");
    }

    @Test
    public void testHttps() {
        Uri https = Uri.parse("https://example.com/bigtable.pdf");
        assertThat(Uris.isHttps(https)).isTrue();
        assertThat(Uris.isHttp(https)).isFalse();
        assertThat(Uris.isRemote(https)).isTrue();
        assertThat(Uris.isLocal(https)).isFalse();

        assertThat(Uris.extractFileName(https)).isEqualTo("bigtable.pdf");
    }

    @Test
    public void testContent() {
        Uri content = Uri.parse("content:app/res");
        assertThat(Uris.isHttp(content)).isFalse();
        assertThat(Uris.isHttps(content)).isFalse();
        assertThat(Uris.isRemote(content)).isFalse();
        assertThat(Uris.isLocal(content)).isTrue();
        assertThat(Uris.isContentUri(content)).isTrue();
        assertThat(Uris.isFileUri(content)).isFalse();
    }

    @Test
    public void testFile() {
        Uri file = Uri.parse("file://sdcard/file.png");
        assertThat(Uris.isHttp(file)).isFalse();
        assertThat(Uris.isHttps(file)).isFalse();
        assertThat(Uris.isRemote(file)).isFalse();
        assertThat(Uris.isLocal(file)).isTrue();
        assertThat(Uris.isContentUri(file)).isFalse();
        assertThat(Uris.isFileUri(file)).isTrue();

        assertThat(Uris.extractFileName(file)).isEqualTo("file.png");
    }
}
