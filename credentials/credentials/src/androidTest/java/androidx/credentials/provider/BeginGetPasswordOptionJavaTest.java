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

import androidx.credentials.GetPasswordOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.TestUtilsKt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
@SmallTest
public class BeginGetPasswordOptionJavaTest {
    private static final String BUNDLE_ID_KEY =
            "android.service.credentials.BeginGetCredentialOption.BUNDLE_ID_KEY";
    private static final String BUNDLE_ID = "id";
    private static final Set<String> EXPECTED_ALLOWED_USER_IDS = generateExpectedAllowedUserIds();

    @Test
    public void constructor_success() {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(
                GetPasswordOption.BUNDLE_KEY_ALLOWED_USER_IDS,
                new ArrayList<>(EXPECTED_ALLOWED_USER_IDS));

        BeginGetPasswordOption option =
                new BeginGetPasswordOption(EXPECTED_ALLOWED_USER_IDS, bundle, BUNDLE_ID);

        bundle.putString(BUNDLE_ID_KEY, BUNDLE_ID);
        assertThat(option.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), bundle)).isTrue();
        assertThat(option.getAllowedUserIds()).containsExactlyElementsIn(EXPECTED_ALLOWED_USER_IDS);
    }

    @Test
    public void createFrom_success() {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(
                GetPasswordOption.BUNDLE_KEY_ALLOWED_USER_IDS,
                new ArrayList<>(EXPECTED_ALLOWED_USER_IDS));

        BeginGetPasswordOption option = BeginGetPasswordOption.createForTest(bundle, "id");
        assertThat(option.getId()).isEqualTo("id");
    }

    private static Set<String> generateExpectedAllowedUserIds() {
        Set<String> ids = new HashSet<>();
        ids.add("id1");
        ids.add("id2");
        ids.add("id3");
        return ids;
    }
}
