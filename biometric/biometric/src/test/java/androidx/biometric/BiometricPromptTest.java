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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
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
    public void testPromptInfo_FailsToBuild_WithNoNegativeText() {
        new BiometricPrompt.PromptInfo.Builder().setTitle("Title").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPromptInfo_FailsToBuild_WithEmptyNegativeText() {
        new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .setNegativeButtonText("")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPromptInfo_FailsToBuild_WithNegativeTextAndDeviceCredential() {
        new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL)
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
