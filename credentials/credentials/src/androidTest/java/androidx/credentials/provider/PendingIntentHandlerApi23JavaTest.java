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

import static androidx.credentials.internal.ConversionUtilsKt.getFinalCreateCredentialData;
import static androidx.credentials.provider.ui.UiUtils.constructActionEntry;
import static androidx.credentials.provider.ui.UiUtils.constructAuthenticationActionEntry;
import static androidx.credentials.provider.ui.UiUtils.constructPasswordCredentialEntryDefault;
import static androidx.credentials.provider.ui.UiUtils.constructRemoteEntry;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;

import androidx.credentials.CreateCredentialRequest;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreateCustomCredentialResponse;
import androidx.credentials.CreatePasswordRequest;
import androidx.credentials.CreatePasswordResponse;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.Credential;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetCustomCredentialOption;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.TestUtilsKt;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialInterruptedException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.domerrors.NotAllowedError;
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(maxSdkVersion = 33)
@SuppressWarnings("deprecation")
public class PendingIntentHandlerApi23JavaTest {

    private static final Icon ICON =
            Icon.createWithBitmap(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    private static final String TEST_JSON = "{\"key\":\"test_value\"}";

    private Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void retrieveProviderCreateCredentialRequest_success() throws Exception {
        CreatePasswordRequest callingRequest = new CreatePasswordRequest("id", "password",
                "origin", /* preferImmediatelyAvailableCredentials= */
                true, /* isAutoSelectAllowed= */ true);
        ProviderCreateCredentialRequest expectedRequest = new ProviderCreateCredentialRequest(
                CreateCredentialRequest.createFrom(callingRequest.getType(),
                        getFinalCreateCredentialData(callingRequest, mContext),
                        callingRequest.getCandidateQueryData(),
                        callingRequest.isSystemProviderRequired(), callingRequest.getOrigin()),
                getTestCallingAppInfo(callingRequest.getOrigin())
        );
        Intent intent = new Intent();
        PendingIntentHandler.Api23Impl.setProviderCreateCredentialRequest(intent, expectedRequest);

        ProviderCreateCredentialRequest actualRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent);

        assertThat(actualRequest).isNotNull();
        TestUtilsKt.assertEquals(mContext, actualRequest, expectedRequest);
    }

    @Test
    public void retrieveProviderCreateCredentialRequest_emptyIntent_returnsNull() throws Exception {
        Intent intent = new Intent();

        ProviderCreateCredentialRequest actualRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent);

        assertThat(actualRequest).isNull();
    }

    @Test
    public void retrieveProviderCreateCredentialRequest_invalidDataInIntent_returnsNull()
            throws Exception {
        GetPublicKeyCredentialDomException expected = new GetPublicKeyCredentialDomException(
                new NotAllowedError(), "Error msg");
        Intent intent = new Intent();
        PendingIntentHandler.setGetCredentialException(intent, expected);

        ProviderCreateCredentialRequest actualRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent);

        assertThat(actualRequest).isNull();
    }

    @Test
    public void setBeginGetCredentialResponse_success() throws Exception {
        GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(TEST_JSON,
                "client_data_hash".getBytes());
        BeginGetCredentialResponse.Builder responseBuilder =
                new BeginGetCredentialResponse.Builder()
                        .addCredentialEntry(constructPasswordCredentialEntryDefault("pwd-username"))
                        .addCredentialEntry(new PublicKeyCredentialEntry(
                                mContext, "username",
                                PendingIntent.getActivity(mContext, 0, new Intent(),
                                        PendingIntent.FLAG_IMMUTABLE),
                                new BeginGetPublicKeyCredentialOption(
                                        option.getCandidateQueryData(), "id",
                                        option.getRequestJson(), option.getClientDataHash()),
                                "displayname", null, ICON, true, true
                        ))
                        .addAction(constructActionEntry("action-title-1", "subtitle"))
                        .addAction(constructActionEntry("action-title-2", null))
                        .addAuthenticationAction(constructAuthenticationActionEntry("auth-title"))
                        .setRemoteEntry(constructRemoteEntry());
        if (Build.VERSION.SDK_INT >= 26) {
            responseBuilder.addCredentialEntry(
                    new CustomCredentialEntry(
                            mContext, "title",
                            PendingIntent.getActivity(mContext, 0, new Intent(),
                                    PendingIntent.FLAG_IMMUTABLE),
                            new BeginGetCustomCredentialOption("id", "custom-type", new Bundle()),
                            null,
                            null,
                            Instant.now(),
                            ICON,
                            false,
                            "entry-group-id",
                            false
                    )
            );
        }
        BeginGetCredentialResponse response = responseBuilder.build();
        Intent intent = new Intent();

        PendingIntentHandler.setBeginGetCredentialResponse(intent, response);

        BeginGetCredentialResponse actual =
                PendingIntentHandler.Api23Impl.extractBeginGetCredentialResponse(intent);
        assertThat(actual).isNotNull();
        TestUtilsKt.assertEquals(mContext, actual, response);
    }

    @Test
    public void retrieveBeginGetCredentialRequest_withoutCallingAppInfo_success() throws Exception {
        Intent intent = new Intent();
        GetPasswordOption pwdOption1 = new GetPasswordOption();
        GetPasswordOption pwdOption2 = new GetPasswordOption(Set.of("uid1", "uid2"), true,
                Set.of(new ComponentName("pkg1", "cls1")));
        GetPublicKeyCredentialOption passkeyOption = new GetPublicKeyCredentialOption(TEST_JSON,
                "hash".getBytes());
        Bundle customQueryData = new Bundle();
        customQueryData.putInt("key1", 1);
        customQueryData.putBinder("key2", new Binder());
        GetCustomCredentialOption customOption = new GetCustomCredentialOption(
                "custom_type", customQueryData, customQueryData, false
        );
        BeginGetCredentialRequest expectedRequest = new BeginGetCredentialRequest(
                List.of(
                        new BeginGetPasswordOption(pwdOption1.getAllowedUserIds(),
                                pwdOption1.getCandidateQueryData(), "id-1"),
                        new BeginGetPasswordOption(pwdOption2.getAllowedUserIds(),
                                pwdOption2.getCandidateQueryData(), "id-2"),
                        new BeginGetPublicKeyCredentialOption(passkeyOption.getCandidateQueryData(),
                                "id-3", passkeyOption.getRequestJson(),
                                passkeyOption.getClientDataHash()),
                        new BeginGetCustomCredentialOption("id-4", customOption.getType(),
                                customOption.getCandidateQueryData())
                )
        );
        PendingIntentHandler.Api23Impl.setBeginGetCredentialRequest(intent, expectedRequest);

        BeginGetCredentialRequest actual = PendingIntentHandler.retrieveBeginGetCredentialRequest(
                intent);

        assertThat(actual).isNotNull();
        TestUtilsKt.assertEquals(actual, expectedRequest);
    }

    @Test
    public void retrieveBeginGetCredentialRequest_withCallingAppInfo_success() throws Exception {
        Intent intent = new Intent();
        GetPasswordOption pwdOption = new GetPasswordOption();
        GetPublicKeyCredentialOption passkeyOption = new GetPublicKeyCredentialOption(TEST_JSON);
        Bundle customQueryData = new Bundle();
        customQueryData.putInt("key1", 1);
        customQueryData.putBinder("key2", new Binder());
        GetCustomCredentialOption customOption = new GetCustomCredentialOption(
                "custom_type", customQueryData, customQueryData, false
        );
        BeginGetCredentialRequest expectedRequest = new BeginGetCredentialRequest(
                List.of(
                        new BeginGetPasswordOption(pwdOption.getAllowedUserIds(),
                                pwdOption.getCandidateQueryData(), "id-1"),
                        new BeginGetPublicKeyCredentialOption(passkeyOption.getCandidateQueryData(),
                                "id-3", passkeyOption.getRequestJson(),
                                passkeyOption.getClientDataHash()),
                        new BeginGetCustomCredentialOption("id-4", customOption.getType(),
                                customOption.getCandidateQueryData())
                ),
                getTestCallingAppInfo(null)
        );
        PendingIntentHandler.Api23Impl.setBeginGetCredentialRequest(intent, expectedRequest);

        BeginGetCredentialRequest actual = PendingIntentHandler.retrieveBeginGetCredentialRequest(
                intent);

        assertThat(actual).isNotNull();
        TestUtilsKt.assertEquals(actual, expectedRequest);
    }

    @Test
    public void retrieveBeginGetCredentialRequest_emptyIntent_returnsNull() throws Exception {
        Intent intent = new Intent();

        BeginGetCredentialRequest actualRequest =
                PendingIntentHandler.retrieveBeginGetCredentialRequest(intent);

        assertThat(actualRequest).isNull();
    }

    @Test
    public void setCreateCredentialResponse_passkeyResponse_success() throws Exception {
        Intent intent = new Intent();
        CreateCredentialResponse expected = new CreatePublicKeyCredentialResponse(TEST_JSON);

        PendingIntentHandler.setCreateCredentialResponse(intent, expected);

        CreateCredentialResponse actual =
                PendingIntentHandler.Api23Impl.extractCreateCredentialResponse(intent);
        assertThat(actual).isNotNull();
        TestUtilsKt.assertEquals(actual, expected);
    }

    @Test
    public void setCreateCredentialResponse_passwordResponse_success() throws Exception {
        Intent intent = new Intent();
        CreateCredentialResponse expected = new CreatePasswordResponse();

        PendingIntentHandler.setCreateCredentialResponse(intent, expected);

        CreateCredentialResponse actual =
                PendingIntentHandler.Api23Impl.extractCreateCredentialResponse(intent);
        assertThat(actual).isNotNull();
        TestUtilsKt.assertEquals(actual, expected);
    }

    @Test
    public void setCreateCredentialResponse_customResponse_success() throws Exception {
        Intent intent = new Intent();
        Bundle customData = new Bundle();
        customData.putString("k1", "text");
        customData.putBinder("k2", new Binder());
        CreateCredentialResponse expected = new CreateCustomCredentialResponse("type", customData);

        PendingIntentHandler.setCreateCredentialResponse(intent, expected);

        CreateCredentialResponse actual =
                PendingIntentHandler.Api23Impl.extractCreateCredentialResponse(intent);
        assertThat(actual).isNotNull();
        TestUtilsKt.assertEquals(actual, expected);
    }

    @Test
    public void retrieveProviderGetCredentialRequest_success() throws Exception {
        Intent intent = new Intent();
        GetPasswordOption pwdOption1 = new GetPasswordOption();
        GetPasswordOption pwdOption2 = new GetPasswordOption(Set.of("uid1", "uid2"), true,
                Set.of(new ComponentName("pkg1", "cls1")));
        GetPublicKeyCredentialOption passkeyOption = new GetPublicKeyCredentialOption(TEST_JSON,
                "hash".getBytes());
        Bundle customQueryData = new Bundle();
        customQueryData.putInt("key1", 1);
        customQueryData.putBinder("key2", new Binder());
        GetCustomCredentialOption customOption = new GetCustomCredentialOption(
                "custom_type", customQueryData, customQueryData, false
        );
        ProviderGetCredentialRequest expectedRequest = new ProviderGetCredentialRequest(
                List.of(pwdOption1, pwdOption2, passkeyOption, customOption),
                getTestCallingAppInfo("origin")
        );
        PendingIntentHandler.Api23Impl.setProviderGetCredentialRequest(intent, expectedRequest);

        ProviderGetCredentialRequest actual =
                PendingIntentHandler.retrieveProviderGetCredentialRequest(intent);

        assertThat(actual).isNotNull();
        TestUtilsKt.assertEquals(actual, expectedRequest);
    }

    @Test
    public void retrieveProviderGetCredentialRequest_emptyIntent_returnsNull() throws Exception {
        Intent intent = new Intent();

        ProviderGetCredentialRequest actualRequest =
                PendingIntentHandler.retrieveProviderGetCredentialRequest(intent);

        assertThat(actualRequest).isNull();
    }

    @Test
    public void setGetCredentialResponse_passwordCredential_success() throws Exception {
        Credential cred = new PasswordCredential("username", "pwd");
        GetCredentialResponse expected = new GetCredentialResponse(cred);
        Intent intent = new Intent();

        PendingIntentHandler.setGetCredentialResponse(intent, expected);

        GetCredentialResponse actual = PendingIntentHandler.Api23Impl.extractGetCredentialResponse(
                intent);
        assertThat(actual).isNotNull();
        TestUtilsKt.equals(actual, expected);
    }

    @Test
    public void setGetCredentialResponse_passkeyCredential_success() throws Exception {
        Credential cred = new PublicKeyCredential(TEST_JSON);
        GetCredentialResponse expected = new GetCredentialResponse(cred);
        Intent intent = new Intent();

        PendingIntentHandler.setGetCredentialResponse(intent, expected);

        GetCredentialResponse actual = PendingIntentHandler.Api23Impl.extractGetCredentialResponse(
                intent);
        assertThat(actual).isNotNull();
        TestUtilsKt.equals(actual, expected);
    }

    @Test
    public void setGetCredentialResponse_customCredential_success() throws Exception {
        Bundle customData = new Bundle();
        customData.putString("k1", "text");
        customData.putBinder("k2", new Binder());
        Credential cred = new CustomCredential("test type", customData);
        GetCredentialResponse expected = new GetCredentialResponse(cred);
        Intent intent = new Intent();

        PendingIntentHandler.setGetCredentialResponse(intent, expected);

        GetCredentialResponse actual = PendingIntentHandler.Api23Impl.extractGetCredentialResponse(
                intent);
        assertThat(actual).isNotNull();
        TestUtilsKt.equals(actual, expected);
    }

    @Test
    public void setGetCredentialException_success() throws Exception {
        GetPublicKeyCredentialDomException expected = new GetPublicKeyCredentialDomException(
                new NotAllowedError(), "Error msg");
        Intent intent = new Intent();

        PendingIntentHandler.setGetCredentialException(intent, expected);

        GetCredentialException actual =
                PendingIntentHandler.Api23Impl.extractGetCredentialException(intent);
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(expected.getClass());
        assertThat(actual.getType()).isEqualTo(expected.getType());
        assertThat(actual.getErrorMessage()).isEqualTo(expected.getErrorMessage());
    }

    @Test
    public void setCreateCredentialException_success() throws Exception {
        CreateCredentialException expected = new CreateCredentialInterruptedException("Error msg");
        Intent intent = new Intent();

        PendingIntentHandler.setCreateCredentialException(intent, expected);

        CreateCredentialException actual =
                PendingIntentHandler.Api23Impl.extractCreateCredentialException(intent);
        assertThat(actual).isNotNull();
        assertThat(actual.getType()).isEqualTo(expected.getType());
        assertThat(actual.getErrorMessage()).isEqualTo(expected.getErrorMessage());
    }


    private CallingAppInfo getTestCallingAppInfo(@Nullable String origin) throws Exception {
        String packageName = mContext.getPackageName();
        if (Build.VERSION.SDK_INT >= 28) {
            PackageInfo packageInfo =
                    mContext.getPackageManager().getPackageInfo(packageName,
                            PackageManager.GET_SIGNING_CERTIFICATES);
            assertNotNull(packageInfo.signingInfo);
            return new CallingAppInfo(packageName, packageInfo.signingInfo, origin);
        } else {
            PackageInfo packageInfo =
                    mContext.getPackageManager().getPackageInfo(packageName,
                            PackageManager.GET_SIGNATURES);
            return new CallingAppInfo(packageName, Arrays.asList(packageInfo.signatures), origin);
        }
    }
}
