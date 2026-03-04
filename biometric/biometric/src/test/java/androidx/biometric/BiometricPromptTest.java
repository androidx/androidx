/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.biometric.BiometricManager.Authenticators;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class BiometricPromptTest {
    @Test
    public void testPromptInfo_CanSetAndGetOptions() {
        final String title = "Title";
        final String subtitle = "Subtitle";
        final String description = "Description";
        final String negativeButtonText = "Negative";
        final boolean isConfirmationRequired = false;
        @BiometricManager.AuthenticatorTypes final int allowedAuthenticators =
                Authenticators.BIOMETRIC_STRONG;

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setConfirmationRequired(isConfirmationRequired)
                .setAllowedAuthenticators(allowedAuthenticators)
                .build();

        assertThat(info.getTitle()).isEqualTo(title);
        assertThat(info.getSubtitle()).isEqualTo(subtitle);
        assertThat(info.getDescription()).isEqualTo(description);
        assertThat(info.getNegativeButtonText()).isEqualTo(negativeButtonText);
        assertThat(isConfirmationRequired).isEqualTo(isConfirmationRequired);
        assertThat(allowedAuthenticators).isEqualTo(allowedAuthenticators);
    }

    @Test
    public void testPromptInfo_CanSetAndGetOptions_logoResAndDescription() {
        final int logoRes = R.drawable.fingerprint_dialog_fp_icon;
        final String logoDescription = "logo description";
        final String title = "Title";
        final String negativeButtonText = "Negative";

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setLogoRes(logoRes)
                .setLogoDescription(logoDescription)
                .setLogoDescription(logoDescription)
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .build();

        assertThat(info.getLogoRes()).isEqualTo(logoRes);
        assertThat(info.getLogoDescription()).isEqualTo(logoDescription);
    }

    @Test
    public void testPromptInfo_CanSetAndGetOptions_logoBitmap() {
        final Bitmap logoBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565);
        final String logoDescription = "logo description";
        final String title = "Title";
        final String negativeButtonText = "Negative";

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setLogoBitmap(logoBitmap)
                .setLogoDescription(logoDescription)
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .build();

        assertThat(info.getLogoBitmap()).isEqualTo(logoBitmap);
    }

    @Test
    public void testPromptInfo_CanSetAndGetOptions_verticalListContent() {
        final String contentDescription = "test description";
        final String itemOne = "content item 1";
        final String itemTwo = "content item 2";
        final PromptVerticalListContentView contentView =
                new PromptVerticalListContentView.Builder()
                        .setDescription(contentDescription)
                        .addListItem(new PromptContentItemBulletedText(itemOne))
                        .addListItem(new PromptContentItemBulletedText(itemTwo), 1).build();
        final String title = "Title";
        final String negativeButtonText = "Negative";

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .setContentView(contentView)
                .build();

        assertThat(info.getContentView()).isEqualTo(contentView);
        final PromptVerticalListContentView realContentView =
                (PromptVerticalListContentView) info.getContentView();
        assertThat(realContentView.getDescription()).isEqualTo(contentDescription);
        final PromptContentItemBulletedText realItemOne =
                (PromptContentItemBulletedText) realContentView.getListItems().get(0);
        assertThat(realItemOne.getText()).isEqualTo(itemOne);
        final PromptContentItemBulletedText realItemTwo =
                (PromptContentItemBulletedText) realContentView.getListItems().get(1);
        assertThat(realItemTwo.getText()).isEqualTo(itemTwo);

    }

    @Test
    public void testPromptInfo_CanSetAndGetOptions_contentViewMoreOptionsButton() {
        final String contentDescription = "test description";
        final PromptContentViewWithMoreOptionsButton contentView =
                new PromptContentViewWithMoreOptionsButton.Builder().setDescription(
                        contentDescription).build();
        final String title = "Title";
        final String negativeButtonText = "Negative";

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .setContentView(contentView)
                .build();

        assertThat(info.getContentView()).isEqualTo(contentView);
        assertThat(
                ((PromptContentViewWithMoreOptionsButton) info.getContentView())
                        .getDescription()).isEqualTo(contentDescription);
    }

    @Test
    public void testPromptInfo_CanSetAndGetOptions_fallbackOptions() {
        final String title = "Title";
        final String negativeButtonText = "Negative";
        final AuthenticationRequest.Biometric.Fallback.CustomOption fallback =
                new AuthenticationRequest.Biometric.Fallback.CustomOption("fallback",
                        BiometricPrompt.ICON_TYPE_ACCOUNT);

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .addFallbackOption(fallback)
                .build();

        assertThat(info.getFallbackOptionList()).containsExactly(fallback);
    }

    @Test
    public void testPromptInfo_CanSetAndGetOptions_multipleFallbackOptions() {
        final String title = "Title";
        final AuthenticationRequest.Biometric.Fallback.CustomOption fallback1 =
                new AuthenticationRequest.Biometric.Fallback.CustomOption("fallback 1",
                        BiometricPrompt.ICON_TYPE_PASSWORD);
        final AuthenticationRequest.Biometric.Fallback.CustomOption fallback2 =
                new AuthenticationRequest.Biometric.Fallback.CustomOption("fallback 2",
                        BiometricPrompt.ICON_TYPE_ACCOUNT);

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .addFallbackOption(fallback1)
                .addFallbackOption(fallback2)
                .build();

        assertThat(info.getFallbackOptionList()).containsExactly(fallback1, fallback2);
    }

    @Test
    public void testPromptInfo_CanSetAndGetOptions_iconTypes() {
        final AuthenticationRequest.Biometric.Fallback.CustomOption fallback =
                new AuthenticationRequest.Biometric.Fallback.CustomOption("fallback",
                        BiometricPrompt.ICON_TYPE_QR_CODE);

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .addFallbackOption(fallback)
                .build();

        assertThat(((AuthenticationRequest.Biometric.Fallback.CustomOption)
                info.getFallbackOptionList().get(0)).getIconType())
                .isEqualTo(BiometricPrompt.ICON_TYPE_QR_CODE);
    }

    @Test
    public void testPromptInfo_DefaultFallbackOptionListIsNull() {
        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .setNegativeButtonText("Negative")
                .build();

        assertThat(info.getFallbackOptionList()).isNull();
    }

    @Test
    public void testPromptInfo_CanBuildWithMixedFallbackOptions() {
        final AuthenticationRequest.Biometric.Fallback.CustomOption fallback1 =
                new AuthenticationRequest.Biometric.Fallback.CustomOption("fallback 1");
        final AuthenticationRequest.Biometric.Fallback.DeviceCredential fallback2 =
                AuthenticationRequest.Biometric.Fallback.DeviceCredential.INSTANCE;

        final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .addFallbackOption(fallback1)
                .addFallbackOption(fallback2)
                .build();

        assertThat(info.getFallbackOptionList()).containsExactly(fallback1, fallback2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPromptInfo_FailsToBuild_WithNoTitle() {
        new BiometricPrompt.PromptInfo.Builder().setNegativeButtonText("Cancel").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPromptInfo_FailsToBuild_WithEmptyTitle() {
        new BiometricPrompt.PromptInfo.Builder()
                .setTitle("")
                .setNegativeButtonText("Cancel")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    @Config(maxSdk = Build.VERSION_CODES.Q)
    public void testPromptInfo_FailsToBuild_WithUnsupportedAuthenticatorCombination() {
        new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .setAllowedAuthenticators(Authenticators.DEVICE_CREDENTIAL)
                .build();
    }
}
