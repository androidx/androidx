/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.util.DocumentIdUtil;

import org.junit.Test;

/*@exportToFramework:SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)*/
public class DocumentIdUtilCtsTest {

    @Test
    public void testQualifiedIdCreation() {
        final String packageName = "pkg";

        // The delimiter requires just a single backslash to escape it, represented by two in Java.
        String qualifiedId = DocumentIdUtil.createQualifiedId(packageName, "data#base",
                "name#space", "id#entifier");
        assertThat(qualifiedId).isEqualTo("pkg$data\\#base/name\\#space#id\\#entifier");

        // The raw namespace contains a backslash followed by a delimiter. As both the backslash
        // and the delimiter are escaped, the result will have a backslash to escape the
        // original backslash, the original backslash, and a backslash to escape the delimiter,
        // and finally the delimiter. It will look like \\\#, which in Java is represented with six
        // backslashes followed by a delimiter.
        qualifiedId = DocumentIdUtil.createQualifiedId(packageName, "data\\#base",
                "name\\#space", "id\\#entifier");
        assertThat(qualifiedId).isEqualTo("pkg$data\\\\\\#base/name\\\\\\#space#id\\\\\\#entifier");

        // Here, the four backlashes represent two backslashes, a backslash to escape the
        // original backslash as well as the original backslash. The number of backslashes gets
        // doubled in both the Java representation as well as the raw String.
        qualifiedId = DocumentIdUtil.createQualifiedId(packageName, "data\\base",
                "name\\space", "id\\entifier");
        assertThat(qualifiedId).isEqualTo("pkg$data\\\\base/name\\\\space#id\\\\entifier");

        qualifiedId = DocumentIdUtil.createQualifiedId(packageName, "data\\\\base",
                "name\\\\space", "id\\\\entifier");
        assertThat(qualifiedId)
                .isEqualTo("pkg$data\\\\\\\\base/name\\\\\\\\space#id\\\\\\\\entifier");

        qualifiedId = DocumentIdUtil.createQualifiedId(packageName, "data\\\\\\base",
                "name\\\\\\space", "id\\\\\\entifier");
        assertThat(qualifiedId)
                .isEqualTo("pkg$data\\\\\\\\\\\\base/name\\\\\\\\\\\\space#id\\\\\\\\\\\\entifier");
    }

    @Test
    public void testQualifiedIdFromDocument() {
        final String packageName = "pkg";
        final String databaseName = "database";

        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "type").build();
        String qualifiedId = DocumentIdUtil.createQualifiedId(packageName, databaseName, document);
        assertThat(qualifiedId).isEqualTo("pkg$database/namespace#id");
    }
}
