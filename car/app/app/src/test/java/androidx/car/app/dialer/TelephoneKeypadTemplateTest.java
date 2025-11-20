/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.car.app.dialer;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Header;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class TelephoneKeypadTemplateTest {
    private final Header mTestHeader =
            new Header.Builder().setStartHeaderAction(Action.BACK).build();
    private final Action mTestPrimaryAction = new Action.Builder().setIcon(CarIcon.BACK).build();
    private final TelephoneKeypadCallback mTestKeypadCallback = mock(TelephoneKeypadCallback.class);
    private final TelephoneKeypadTemplate.PhoneNumberChangeListener mTestNumberChangeListener =
            mock(TelephoneKeypadTemplate.PhoneNumberChangeListener.class);
    private final Map<Integer, CarText> mTestSecondaryTextMap =
            new HashMap<Integer, CarText>() {
                {
                    put(TelephoneKeypadTemplate.KEY_FIVE, CarText.create("zzz"));
                    put(TelephoneKeypadTemplate.KEY_EIGHT, CarText.create("yyy"));
                }
            };
    private final OnDoneCallback mTestOnDoneCallback = new OnDoneCallback() {};

    @Test
    public void builder() {
        String testPhoneNumber = "2222222";
        TelephoneKeypadTemplate instance =
                new TelephoneKeypadTemplate.Builder(mTestPrimaryAction, mTestNumberChangeListener)
                        .setHeader(mTestHeader)
                        .setTelephoneKeypadCallback(mTestKeypadCallback)
                        .setKeySecondaryTexts(mTestSecondaryTextMap)
                        .setPhoneNumber(testPhoneNumber)
                        .build();

        assertThat(instance.getHeader()).isEqualTo(mTestHeader);
        assertThat(instance.getKeySecondaryTexts()).containsExactlyEntriesIn(mTestSecondaryTextMap);
        assertThat(instance.getPhoneNumber()).isEqualTo(testPhoneNumber);
        assertThat(instance.getPrimaryAction()).isEqualTo(mTestPrimaryAction);

        instance.getTelephoneKeypadCallbackDelegate()
                .sendKeyDown(TelephoneKeypadTemplate.KEY_TWO, mTestOnDoneCallback);
        verify(mTestKeypadCallback).onKeyDown(TelephoneKeypadTemplate.KEY_TWO);

        instance.getPhoneNumberChangedDelegate()
                .sendInputTextChanged("test number", mTestOnDoneCallback);
        verify(mTestNumberChangeListener).onPhoneNumberChanged("test number");
    }

    @Test
    public void builder_throws_withInvalidHeader() {
        Header header = new Header.Builder().setStartHeaderAction(Action.PAN).build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TelephoneKeypadTemplate.Builder(
                                        mTestPrimaryAction, mTestNumberChangeListener)
                                .setHeader(header)
                                .build());
    }

    @Test
    public void builder_throws_withInvalidPrimaryAction() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new TelephoneKeypadTemplate.Builder(
                                                Action.BACK, mTestNumberChangeListener)
                                        .build());

        assertThat(exception).hasMessageThat().contains("BACK is not allowed");
    }

    @Test
    public void builder_throws_whenAddingSecondaryTextToInvalidKey() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new TelephoneKeypadTemplate.Builder(
                                                mTestPrimaryAction, mTestNumberChangeListener)
                                        .addKeySecondaryText(-1, CarText.create("zzz"))
                                        .build());
        assertThat(exception).hasMessageThat().contains("Invalid key int");
    }

    @Test
    public void equals_hashCode() {
        CarIcon icon1 =
                TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(), "ic_test_1");
        TelephoneKeypadTemplate first =
                new TelephoneKeypadTemplate.Builder(
                                new Action.Builder().setIcon(icon1).build(), (number) -> {})
                        .setHeader(new Header.Builder().setTitle("Test").build())
                        .setTelephoneKeypadCallback(
                                new TelephoneKeypadCallback() {
                                    @Override
                                    public void onKeyLongPress(int key) {}

                                    @Override
                                    public void onKeyDown(int key) {}

                                    @Override
                                    public void onKeyUp(int key) {}
                                })
                        .setKeySecondaryTexts(
                                new HashMap<Integer, CarText>() {
                                    {
                                        put(
                                                TelephoneKeypadTemplate.KEY_NINE,
                                                CarText.create("eee"));
                                    }
                                })
                        .setPhoneNumber("(555) 555-5555")
                        .build();
        TelephoneKeypadTemplate second =
                new TelephoneKeypadTemplate.Builder(
                                new Action.Builder().setIcon(icon1).build(), (number) -> {})
                        .setHeader(new Header.Builder().setTitle("Test").build())
                        .setTelephoneKeypadCallback(
                                new TelephoneKeypadCallback() {
                                    @Override
                                    public void onKeyLongPress(int key) {}

                                    @Override
                                    public void onKeyDown(int key) {}

                                    @Override
                                    public void onKeyUp(int key) {}
                                })
                        .setKeySecondaryTexts(
                                new HashMap<Integer, CarText>() {
                                    {
                                        put(
                                                TelephoneKeypadTemplate.KEY_NINE,
                                                CarText.create("eee"));
                                    }
                                })
                        .setPhoneNumber("(555) 555-5555")
                        .build();

        assertThat(first.equals(second)).isTrue();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
