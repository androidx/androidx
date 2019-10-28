/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted.sharing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.browser.trusted.sharing.ShareTarget.FileFormField;
import androidx.browser.trusted.sharing.ShareTarget.Params;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link ShareTarget}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@SmallTest
public class ShareTargetTest {

    private static final String ACTION = "action.html";
    private static final String METHOD = "POST";
    private static final String ENCODING_TYPE = "multipart/form-data";

    private static final FileFormField FILE_FIELD_1 =
            new FileFormField("file1", Arrays.asList("type1", "type2"));

    private static final FileFormField FILE_FIELD_2 =
            new FileFormField("file2", Arrays.asList("type3", "type4"));

    private static final List<FileFormField> FILES =
            Arrays.asList(FILE_FIELD_1, FILE_FIELD_2);

    private static final Params DEFAULT_PARAMS = new Params("title", "text", FILES);
    private static final Params PARAMS_NO_FILES = new Params("title", "text", null);
    private static final Params PARAMS_NO_TITLE = new Params(null, "text", FILES);
    private static final Params PARAMS_NO_TEXT = new Params("title", null, FILES);

    @ParameterizedRobolectricTestRunner.Parameters(name = "{1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{new ShareTarget(ACTION, METHOD, ENCODING_TYPE, DEFAULT_PARAMS),
                        "All included"},
                new Object[]{new ShareTarget(ACTION, METHOD, ENCODING_TYPE, PARAMS_NO_FILES),
                        "No files"},
                new Object[]{new ShareTarget(ACTION, METHOD, ENCODING_TYPE, PARAMS_NO_TITLE),
                        "No title"},
                new Object[]{new ShareTarget(ACTION, METHOD, ENCODING_TYPE, PARAMS_NO_TEXT),
                        "No text"},
                new Object[]{new ShareTarget(ACTION, null, ENCODING_TYPE, DEFAULT_PARAMS),
                        "No method"},
                new Object[]{new ShareTarget(ACTION, METHOD, null, DEFAULT_PARAMS),
                        "No enc type"}
        );
    }

    private final ShareTarget mShareTarget;

    public ShareTargetTest(ShareTarget shareTarget, String testName) {
        mShareTarget = shareTarget;
    }

    @Test
    public void bundlingAndUnbundlingYieldsOriginalObject() {
        assertShareTargetEquals(mShareTarget, ShareTarget.fromBundle(mShareTarget.toBundle()));
    }

    private void assertShareTargetEquals(ShareTarget expected, ShareTarget actual) {
        assertEquals(expected.action, actual.action);
        assertEquals(expected.encodingType, actual.encodingType);
        assertEquals(expected.method, actual.method);
        assertParamsEqual(expected.params, actual.params);
    }

    private void assertParamsEqual(Params expected, Params actual) {
        assertEquals(expected.text, actual.text);
        assertEquals(expected.title, actual.title);
        assertFilesEqual(expected.files, actual.files);
    }

    private void assertFilesEqual(List<FileFormField> expected,
            List<FileFormField> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertFileEquals(expected.get(i), actual.get(i));
        }
    }

    private void assertFileEquals(FileFormField expected,
            FileFormField actual) {
        assertEquals(expected.name, actual.name);
        assertEquals(expected.acceptedTypes, actual.acceptedTypes);
    }
}
