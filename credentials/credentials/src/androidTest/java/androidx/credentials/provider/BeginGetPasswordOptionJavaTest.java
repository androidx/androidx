/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.provider;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.core.os.BuildCompat;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.TestUtilsKt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginGetPasswordOptionJavaTest {
    private static final String BUNDLE_ID_KEY =
            "android.service.credentials.BeginGetCredentialOption.BUNDLE_ID_KEY";
    private static final String BUNDLE_ID = "id";
    @Test
    public void getter_frameworkProperties() {
        if (BuildCompat.isAtLeastU()) {
            Set<String> expectedAllowedUserIds = ImmutableSet.of("id1", "id2", "id3");
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(GetPasswordOption.BUNDLE_KEY_ALLOWED_USER_IDS,
                    new ArrayList<>(expectedAllowedUserIds));

            BeginGetPasswordOption option = new BeginGetPasswordOption(expectedAllowedUserIds,
                    bundle, BUNDLE_ID);

            bundle.putString(BUNDLE_ID_KEY, BUNDLE_ID);
            assertThat(option.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
            assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), bundle)).isTrue();
            assertThat(option.getAllowedUserIds())
                    .containsExactlyElementsIn(expectedAllowedUserIds);
        }
    }

    // TODO ("Add framework conversion, createFrom tests")
}
