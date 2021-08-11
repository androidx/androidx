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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GetByDocumentIdRequest;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class GetByDocumentIdRequestCtsTest {
    @Test
    public void testBuildRequest() {
        List<String> expectedPropertyPaths1 = Arrays.asList("path1", "path2");
        List<String> expectedPropertyPaths2 = Arrays.asList("path3", "path4");
        GetByDocumentIdRequest getByDocumentIdRequest =
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("uri1", "uri2")
                        .addIds(Arrays.asList("uri3", "uri4"))
                        .addProjection("schemaType1", expectedPropertyPaths1)
                        .addProjection("schemaType2", expectedPropertyPaths2)
                        .build();

        assertThat(getByDocumentIdRequest.getIds()).containsExactly(
                "uri1", "uri2", "uri3", "uri4");
        assertThat(getByDocumentIdRequest.getNamespace()).isEqualTo("namespace");
        assertThat(getByDocumentIdRequest.getProjections())
                .containsExactly("schemaType1", expectedPropertyPaths1, "schemaType2",
                        expectedPropertyPaths2);
    }
}
