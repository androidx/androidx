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

package androidx.core.view.inputmethod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.ClipDescription;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;

import androidx.core.app.TestActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;

import java.util.Objects;


@RunWith(AndroidJUnit4.class)
@MediumTest
public class InputConnectionCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    public InputConnectionCompatTest() {
        super(TestActivity.class);
    }

    private static final String COMMIT_CONTENT_ACTION =
            "androidx.core.view.inputmethod.InputConnectionCompat.COMMIT_CONTENT";
    private static final String COMMIT_CONTENT_INTEROP_ACTION =
            "android.support.v13.view.inputmethod.InputConnectionCompat.COMMIT_CONTENT";
    private static final String COMMIT_CONTENT_CONTENT_URI_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_URI";
    private static final String COMMIT_CONTENT_CONTENT_URI_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_URI";
    private static final String COMMIT_CONTENT_DESCRIPTION_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_DESCRIPTION";
    private static final String COMMIT_CONTENT_DESCRIPTION_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_DESCRIPTION";
    private static final String COMMIT_CONTENT_LINK_URI_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_LINK_URI";
    private static final String COMMIT_CONTENT_LINK_URI_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_LINK_URI";
    private static final String COMMIT_CONTENT_OPTS_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_OPTS";
    private static final String COMMIT_CONTENT_OPTS_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_OPTS";
    private static final String COMMIT_CONTENT_FLAGS_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_FLAGS";
    private static final String COMMIT_CONTENT_FLAGS_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_FLAGS";

    private static final String[] TEST_MIME_TYPES = new String[]{"image/gif"};
    private static final ClipDescription TEST_CLIP_DESCRIPTION =
            new ClipDescription("test", TEST_MIME_TYPES);

    private static final Uri TEST_CONTENT_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("androidx.core.view.inputmethod.test")
            .appendPath("foobar")
            .build();

    private static final Uri TEST_LINK_URI = Uri.parse("https://example.com");

    private static final InputContentInfoCompat TEST_INPUT_CONTENT_INFO =
            new InputContentInfoCompat(
                    TEST_CONTENT_URI, TEST_CLIP_DESCRIPTION, TEST_LINK_URI);

    private static final Bundle TEST_BUNDLE = new Bundle();
    private static final int TEST_FLAGS = 0;

    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void commitContentPlatformApi() {
        EditorInfo editorInfo = new EditorInfo();
        EditorInfoCompat.setContentMimeTypes(editorInfo, TEST_MIME_TYPES);

        InputConnection ic = mock(InputConnection.class);
        doReturn(true).when(ic).commitContent(
                any(InputContentInfo.class), anyInt(), any(Bundle.class));

        InputConnectionCompat.commitContent(
                ic, editorInfo, TEST_INPUT_CONTENT_INFO, TEST_FLAGS, TEST_BUNDLE);

        verify(ic).commitContent(
                argThat(new ArgumentMatcher<InputContentInfo>() {
                    @Override
                    public boolean matches(InputContentInfo info) {
                        return Objects.equals(
                                TEST_INPUT_CONTENT_INFO.getContentUri(), info.getContentUri())
                                && Objects.equals(TEST_INPUT_CONTENT_INFO.getDescription(),
                                info.getDescription())
                                && Objects.equals(TEST_INPUT_CONTENT_INFO.getLinkUri(),
                                info.getLinkUri());
                    }
                }),
                eq(TEST_FLAGS),
                eq(TEST_BUNDLE)
        );
    }

    @Test
    @SdkSuppress(maxSdkVersion = 24)
    public void commitContentSupportLib() {
        verifyCommitContentCompat(EditorInfoCompat.Protocol.SupportLib);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 24)
    public void commitContentAndroidX100() {
        verifyCommitContentCompat(EditorInfoCompat.Protocol.AndroidX_1_0_0);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 24)
    public void commitContentAndroidX110() {
        verifyCommitContentCompat(EditorInfoCompat.Protocol.AndroidX_1_1_0);
    }

    private void verifyCommitContentCompat(int protocol) {
        EditorInfo editorInfo = EditorInfoTestUtils.createEditorInfoForTest(
                TEST_MIME_TYPES, protocol);

        InputConnection ic = mock(InputConnection.class);
        doReturn(true).when(ic).performPrivateCommand(anyString(), any(Bundle.class));

        InputContentInfoCompat inputContentInfoCompat = new InputContentInfoCompat(
                TEST_CONTENT_URI, TEST_CLIP_DESCRIPTION, TEST_LINK_URI);
        InputConnectionCompat.commitContent(
                ic, editorInfo, inputContentInfoCompat, TEST_FLAGS, TEST_BUNDLE);

        verify(ic).performPrivateCommand(
                eq(getActionName(protocol)), argThat(getBundleMatcher(protocol)));
    }

    private static String getActionName(int protocol) {
        switch (protocol) {
            case EditorInfoCompat.Protocol.SupportLib:
                // If the target app is based on support-lib version, use legacy action name.
                return COMMIT_CONTENT_INTEROP_ACTION;
            case EditorInfoCompat.Protocol.AndroidX_1_0_0:
            case EditorInfoCompat.Protocol.AndroidX_1_1_0:
                // Otherwise, use new action name.
                return COMMIT_CONTENT_ACTION;
            default:
                throw new UnsupportedOperationException("Unsupported protocol=" + protocol);
        }
    }

    private static ArgumentMatcher<Bundle> getBundleMatcher(int protocol) {
        final String contentUriKey;
        final String descriptionKey;
        final String linkUriKey;
        final String optsKey;
        final String flagsKey;
        switch (protocol) {
            case EditorInfoCompat.Protocol.SupportLib:
                // If the target app is based on support-lib version, use legacy keys.
                contentUriKey = COMMIT_CONTENT_CONTENT_URI_INTEROP_KEY;
                descriptionKey = COMMIT_CONTENT_DESCRIPTION_INTEROP_KEY;
                linkUriKey = COMMIT_CONTENT_LINK_URI_INTEROP_KEY;
                flagsKey = COMMIT_CONTENT_FLAGS_INTEROP_KEY;
                optsKey = COMMIT_CONTENT_OPTS_INTEROP_KEY;
                break;
            case EditorInfoCompat.Protocol.AndroidX_1_0_0:
            case EditorInfoCompat.Protocol.AndroidX_1_1_0:
                // Otherwise, use new keys.
                contentUriKey = COMMIT_CONTENT_CONTENT_URI_KEY;
                descriptionKey = COMMIT_CONTENT_DESCRIPTION_KEY;
                linkUriKey = COMMIT_CONTENT_LINK_URI_KEY;
                flagsKey = COMMIT_CONTENT_FLAGS_KEY;
                optsKey = COMMIT_CONTENT_OPTS_KEY;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported protocol=" + protocol);
        }
        return new ArgumentMatcher<Bundle>() {
            @Override
            public boolean matches(Bundle data) {
                final Uri contentUri = data.getParcelable(contentUriKey);
                final ClipDescription description = data.getParcelable(descriptionKey);
                final Uri linkUri = data.getParcelable(linkUriKey);
                final int flags = data.getInt(flagsKey);
                final Bundle opts = data.getParcelable(optsKey);
                return TEST_CONTENT_URI.equals(contentUri)
                        && TEST_CLIP_DESCRIPTION.equals(description)
                        && TEST_LINK_URI.equals(linkUri)
                        && flags == TEST_FLAGS
                        && opts.equals(TEST_BUNDLE);
            }
        };
    }
}
