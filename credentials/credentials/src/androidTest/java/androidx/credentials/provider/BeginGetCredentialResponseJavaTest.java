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

package androidx.credentials.provider;

import static androidx.credentials.provider.ui.UiUtils.constructActionEntry;
import static androidx.credentials.provider.ui.UiUtils.constructAuthenticationActionEntry;
import static androidx.credentials.provider.ui.UiUtils.constructPasswordCredentialEntryDefault;
import static androidx.credentials.provider.ui.UiUtils.constructRemoteEntryDefault;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.credentials.PasswordCredential;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginGetCredentialResponseJavaTest {

    @Test
    public void constructor_success() {
        new BeginGetCredentialResponse();
    }

    @Test
    public void constructor_nullList_throws_allListsNull() {
        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginGetCredentialResponse(
                        null, null, null, constructRemoteEntryDefault())
        );
    }

    @Test
    public void constructor_nullList_throws_credEntriesNull() {
        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginGetCredentialResponse(
                        null, new ArrayList<>(), new ArrayList<>(), constructRemoteEntryDefault())
        );
    }

    @Test
    public void constructor_nullList_throws_actionsNull() {
        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginGetCredentialResponse(
                        new ArrayList<>(), null, new ArrayList<>(), constructRemoteEntryDefault())
        );
    }

    @Test
    public void constructor_nullList_throws_authActionsNull() {
        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginGetCredentialResponse(
                        new ArrayList<>(), new ArrayList<>(), null, constructRemoteEntryDefault())
        );
    }

    @Test
    public void buildConstruct_success() {
        new BeginGetCredentialResponse.Builder().build();
    }

    @Test
    public void buildConstruct_nullList_throws() {
        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginGetCredentialResponse.Builder().setCredentialEntries(null)
                        .setActions(null).setAuthenticationActions(null).build()
        );
    }

    @Test
    public void getter_credentialEntries() {
        int expectedSize = 1;
        String expectedType = PasswordCredential.TYPE_PASSWORD_CREDENTIAL;
        String expectedUsername = "f35";

        BeginGetCredentialResponse response = new BeginGetCredentialResponse(
                Collections.singletonList(constructPasswordCredentialEntryDefault(
                        expectedUsername)), Collections.emptyList(), Collections.emptyList(),
                null);
        int actualSize = response.getCredentialEntries().size();
        String actualType = response.getCredentialEntries().get(0).getType();
        String actualUsername = ((PasswordCredentialEntry) response.getCredentialEntries().get(0))
                .getUsername().toString();

        assertThat(actualSize).isEqualTo(expectedSize);
        assertThat(actualType).isEqualTo(expectedType);
        assertThat(actualUsername).isEqualTo(expectedUsername);
    }

    @Test
    public void getter_actionEntries() {
        int expectedSize = 1;
        String expectedTitle = "boeing";
        String expectedSubtitle = "737max";

        BeginGetCredentialResponse response = new BeginGetCredentialResponse(
                Collections.emptyList(),
                Collections.singletonList(constructActionEntry(expectedTitle, expectedSubtitle)),
                Collections.emptyList(), null);
        int actualSize = response.getActions().size();
        String actualTitle = response.getActions().get(0).getTitle().toString();
        String actualSubtitle = response.getActions().get(0).getSubtitle().toString();

        assertThat(actualSize).isEqualTo(expectedSize);
        assertThat(actualTitle).isEqualTo(expectedTitle);
        assertThat(actualSubtitle).isEqualTo(expectedSubtitle);
    }

    @Test
    public void getter_authActionEntries() {
        int expectedSize = 1;
        String expectedTitle = "boeing";

        BeginGetCredentialResponse response = new BeginGetCredentialResponse(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(constructAuthenticationActionEntry(expectedTitle)), null);
        int actualSize = response.getAuthenticationActions().size();
        String actualTitle = response.getAuthenticationActions().get(0).getTitle().toString();

        assertThat(actualSize).isEqualTo(expectedSize);
        assertThat(actualTitle).isEqualTo(expectedTitle);
    }

    @Test
    public void getter_remoteEntry_null() {
        RemoteEntry expectedRemoteEntry = null;

        BeginGetCredentialResponse response = new BeginGetCredentialResponse(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                expectedRemoteEntry
        );
        RemoteEntry actualRemoteEntry = response.getRemoteEntry();

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry);
    }

    @Test
    public void getter_remoteEntry_nonNull() {
        RemoteEntry expectedRemoteEntry = constructRemoteEntryDefault();

        BeginGetCredentialResponse response = new BeginGetCredentialResponse(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                expectedRemoteEntry
        );
        RemoteEntry actualRemoteEntry = response.getRemoteEntry();

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry);
    }
}
