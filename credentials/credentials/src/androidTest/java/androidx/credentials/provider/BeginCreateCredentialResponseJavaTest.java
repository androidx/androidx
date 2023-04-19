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

import static androidx.credentials.provider.ui.UiUtils.constructCreateEntryWithSimpleParams;
import static androidx.credentials.provider.ui.UiUtils.constructRemoteEntry;
import static androidx.credentials.provider.ui.UiUtils.constructRemoteEntryDefault;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.core.os.BuildCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginCreateCredentialResponseJavaTest {

    @Test
    public void constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        new BeginCreateCredentialResponse(
                Arrays.asList(constructCreateEntryWithSimpleParams("AccountName",
                        "Desc")),
                null
        );
    }

    @Test
    public void builder_createEntriesOnly_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        new BeginCreateCredentialResponse.Builder().setCreateEntries(
                Arrays.asList(constructCreateEntryWithSimpleParams("AccountName",
                        "Desc"))
        ).build();
    }

    @Test
    public void builder_remoteEntryOnly_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        new BeginCreateCredentialResponse.Builder().setRemoteEntry(
                constructRemoteEntry()
        ).build();
    }

    @Test
    public void constructor_nullList_throws() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginCreateCredentialResponse(
                        null, null)
        );
    }

    @Test
    public void buildConstruct_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        new BeginCreateCredentialResponse.Builder().setCreateEntries(
                Arrays.asList(constructCreateEntryWithSimpleParams("AccountName",
                        "Desc"))).build();
    }

    @Test
    public void buildConstruct_nullList_throws() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginCreateCredentialResponse.Builder().setCreateEntries(null).build()
        );
    }

    @Test
    public void getter_createEntry() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        String expectedAccountName = "AccountName";
        String expectedDescription = "Desc";

        BeginCreateCredentialResponse response = new BeginCreateCredentialResponse(
                Collections.singletonList(constructCreateEntryWithSimpleParams(expectedAccountName,
                        expectedDescription)), null);
        String actualAccountName = response.getCreateEntries().get(0).getAccountName().toString();
        String actualDescription = response.getCreateEntries().get(0).getDescription().toString();

        assertThat(actualAccountName).isEqualTo(expectedAccountName);
        assertThat(actualDescription).isEqualTo(expectedDescription);
    }

    @Test
    public void getter_remoteEntry_null() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        RemoteEntry expectedRemoteEntry = null;
        BeginCreateCredentialResponse response = new BeginCreateCredentialResponse(
                Arrays.asList(constructCreateEntryWithSimpleParams("AccountName",
                        "Desc")),
                expectedRemoteEntry
        );
        RemoteEntry actualRemoteEntry = response.getRemoteEntry();

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry);
    }

    @Test
    public void getter_remoteEntry_nonNull() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        RemoteEntry expectedRemoteEntry = constructRemoteEntryDefault();

        BeginCreateCredentialResponse response = new BeginCreateCredentialResponse(
                Arrays.asList(constructCreateEntryWithSimpleParams("AccountName",
                        "Desc")),
                expectedRemoteEntry
        );
        RemoteEntry actualRemoteEntry = response.getRemoteEntry();

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry);
    }
}
