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

package androidx.credentials;

import static androidx.credentials.internal.ConversionUtilsKt.getFinalCreateCredentialData;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.graphics.drawable.Icon;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreateCredentialRequestDisplayInfoJavaTest {
    private Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    @Test
    public void constructor_nullUserId_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreateCredentialRequest.DisplayInfo(null)
        );
    }

    @Test
    public void constructor_emptyUserId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateCredentialRequest.DisplayInfo("")
        );
    }

    @Test
    public void constructWithUserIdOnly_success() {
        String expectedUserId = "userId";

        CreateCredentialRequest.DisplayInfo displayInfo =
                new CreateCredentialRequest.DisplayInfo(expectedUserId);

        assertThat(displayInfo.getUserId()).isEqualTo(expectedUserId);
        assertThat(displayInfo.getUserDisplayName()).isNull();
        assertThat(displayInfo.getCredentialTypeIcon()).isNull();
    }

    @Test
    public void constructWithUserIdAndDisplayName_success() {
        CharSequence expectedUserId = "userId";
        CharSequence expectedDisplayName = "displayName";

        CreateCredentialRequest.DisplayInfo displayInfo =
                new CreateCredentialRequest.DisplayInfo(expectedUserId,
                        expectedDisplayName);

        assertThat(displayInfo.getUserId()).isEqualTo(expectedUserId);
        assertThat(displayInfo.getUserDisplayName()).isEqualTo(expectedDisplayName);
        assertThat(displayInfo.getCredentialTypeIcon()).isNull();
        assertThat(displayInfo.getPreferDefaultProvider()).isNull();
    }

    @SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
    @Test
    public void constructWithUserIdAndDisplayNameAndDefaultProvider_success() {
        CharSequence expectedUserId = "userId";
        CharSequence expectedDisplayName = "displayName";
        String expectedDefaultProvider = "com.test/com.test.TestProviderComponent";

        CreateCredentialRequest.DisplayInfo displayInfo =
                new CreateCredentialRequest.DisplayInfo(expectedUserId,
                        expectedDisplayName, expectedDefaultProvider);

        assertThat(displayInfo.getUserId()).isEqualTo(expectedUserId);
        assertThat(displayInfo.getUserDisplayName()).isEqualTo(expectedDisplayName);
        assertThat(displayInfo.getCredentialTypeIcon()).isNull();
        assertThat(displayInfo.getPreferDefaultProvider()).isEqualTo(expectedDefaultProvider);
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void constructWithOptionalParameters_success() {
        CharSequence expectedUserId = "userId";
        CharSequence expectedDisplayName = "displayName";
        Icon expectedIcon = Icon.createWithResource(mContext, R.drawable.ic_passkey);
        String expectedDefaultProvider = "defaultProvider";

        CreateCredentialRequest.DisplayInfo displayInfo =
                new CreateCredentialRequest.DisplayInfo(expectedUserId,
                        expectedDisplayName, expectedIcon, expectedDefaultProvider);

        assertThat(displayInfo.getUserId()).isEqualTo(expectedUserId);
        assertThat(displayInfo.getUserDisplayName()).isEqualTo(expectedDisplayName);
        assertThat(displayInfo.getCredentialTypeIcon()).isEqualTo(expectedIcon);
        assertThat(displayInfo.getPreferDefaultProvider()).isEqualTo(expectedDefaultProvider);
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    public void constructFromBundle_success() {
        String expectedUserId = "userId";
        CreatePasswordRequest request = new CreatePasswordRequest(expectedUserId, "password");

        CreateCredentialRequest.DisplayInfo displayInfo =
                CreateCredentialRequest.DisplayInfo.createFrom(
                        getFinalCreateCredentialData(
                                request, mContext)
                );

        assertThat(displayInfo.getUserId()).isEqualTo(expectedUserId);
        assertThat(displayInfo.getUserDisplayName()).isNull();
        assertThat(displayInfo.getCredentialTypeIcon().getResId()).isEqualTo(
                R.drawable.ic_password);
        assertThat(displayInfo.getPreferDefaultProvider()).isNull();
    }
}
