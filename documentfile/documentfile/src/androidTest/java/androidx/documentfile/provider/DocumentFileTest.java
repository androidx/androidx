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

package androidx.documentfile.provider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DocumentFileTest {
    private static final Uri CONTENT_TREE_ROOT_URI =
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A");
    private static final Uri EXT_ROOT_TREE_URI =
            Uri.parse(CONTENT_TREE_ROOT_URI + "/document/primary%3A");

    private static final Uri DOWNLOAD_URI =
            Uri.parse(CONTENT_TREE_ROOT_URI + "/document/primary%3ADownload");

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testFromTreeUriUsesFullDocumentId() {
        Context context = ApplicationProvider.getApplicationContext();
        DocumentFile rootDoc = DocumentFile.fromTreeUri(context, CONTENT_TREE_ROOT_URI);
        assertThat(rootDoc.getUri(), equalTo(EXT_ROOT_TREE_URI));

        DocumentFile subDirDoc = DocumentFile.fromTreeUri(context, DOWNLOAD_URI);
        assertThat(subDirDoc.getUri(), equalTo(DOWNLOAD_URI));
    }
}
