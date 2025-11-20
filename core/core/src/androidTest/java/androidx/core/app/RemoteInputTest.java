/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RemoteInputTest extends BaseInstrumentationTestCase<TestActivity> {
    private static final String RESULT_KEY = "result_key";  // value doesn't matter
    private static final String MIME_TYPE = "mimeType";  // value doesn't matter

    public RemoteInputTest() {
        super(TestActivity.class);
    }

    @Test
    public void testRemoteInputBuilder_toAndFromPlatform() throws Throwable {
        RemoteInput originalInput = new RemoteInput.Builder(RESULT_KEY)
                .setAllowFreeFormInput(false)
                .setAllowDataType(MIME_TYPE, true)
                .setEditChoicesBeforeSending(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_DISABLED)
                .setChoices(new CharSequence[]{"first", "second"})
                .build();

        assertFalse(originalInput.isDataOnly());
        assertFalse(originalInput.getAllowFreeFormInput());
        assertEquals(2, originalInput.getChoices().length);
        assertEquals("first", originalInput.getChoices()[0]);
        assertEquals("second", originalInput.getChoices()[1]);
        assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_DISABLED,
                originalInput.getEditChoicesBeforeSending());
        assertEquals(1, originalInput.getAllowedDataTypes().size());
        assertTrue(originalInput.getAllowedDataTypes().contains(MIME_TYPE));

        android.app.RemoteInput platformInput =
                RemoteInput.fromCompat(originalInput);
        if (Build.VERSION.SDK_INT >= 26) {
            assertFalse(platformInput.isDataOnly());
        }
        assertFalse(platformInput.getAllowFreeFormInput());
        assertEquals(2, platformInput.getChoices().length);
        assertEquals("first", platformInput.getChoices()[0]);
        assertEquals("second", platformInput.getChoices()[1]);
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_DISABLED,
                    platformInput.getEditChoicesBeforeSending());
        }
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(1, platformInput.getAllowedDataTypes().size());
            assertTrue(platformInput.getAllowedDataTypes().contains(MIME_TYPE));
        }

        RemoteInput compatInput = RemoteInput.fromPlatform(platformInput);
        assertFalse(compatInput.isDataOnly());
        assertFalse(compatInput.getAllowFreeFormInput());
        assertEquals(2, compatInput.getChoices().length);
        assertEquals("first", compatInput.getChoices()[0]);
        assertEquals("second", compatInput.getChoices()[1]);
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_DISABLED,
                    compatInput.getEditChoicesBeforeSending());
        }
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(1, compatInput.getAllowedDataTypes().size());
            assertTrue(compatInput.getAllowedDataTypes().contains(MIME_TYPE));
        }
    }

    @Test
    public void testRemoteInputBuilder_setDataOnly() throws Throwable {
        RemoteInput input = newDataOnlyRemoteInput();

        assertTrue(input.isDataOnly());
        assertFalse(input.getAllowFreeFormInput());
        assertTrue(input.getChoices() == null || input.getChoices().length == 0);
        assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO,
                input.getEditChoicesBeforeSending());
        assertEquals(1, input.getAllowedDataTypes().size());
        assertTrue(input.getAllowedDataTypes().contains(MIME_TYPE));
    }

    @Test
    public void testRemoteInputBuilder_setTextOnly() throws Throwable {
        RemoteInput input = newTextRemoteInput();

        assertFalse(input.isDataOnly());
        assertTrue(input.getAllowFreeFormInput());
        assertTrue(input.getChoices() == null || input.getChoices().length == 0);
        assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO,
                input.getEditChoicesBeforeSending());
        assertTrue(input.getAllowedDataTypes() == null || input.getAllowedDataTypes().isEmpty());
    }

    @Test
    public void testRemoteInputBuilder_setChoicesOnly() throws Throwable {
        RemoteInput input = newChoicesOnlyRemoteInput();

        assertFalse(input.isDataOnly());
        assertFalse(input.getAllowFreeFormInput());
        assertTrue(input.getChoices() != null && input.getChoices().length > 0);
        assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO,
                input.getEditChoicesBeforeSending());
        assertTrue(input.getAllowedDataTypes() == null || input.getAllowedDataTypes().isEmpty());
    }

    @Test
    public void testRemoteInputBuilder_setDataAndTextAndChoices() throws Throwable {
        CharSequence[] choices = new CharSequence[2];
        choices[0] = "first";
        choices[1] = "second";
        RemoteInput input =
                new RemoteInput.Builder(RESULT_KEY)
                .setChoices(choices)
                .setAllowDataType(MIME_TYPE, true)
                .build();

        assertFalse(input.isDataOnly());
        assertTrue(input.getAllowFreeFormInput());
        assertTrue(input.getChoices() != null && input.getChoices().length > 0);
        assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO,
                input.getEditChoicesBeforeSending());
        assertEquals(1, input.getAllowedDataTypes().size());
        assertTrue(input.getAllowedDataTypes().contains(MIME_TYPE));
    }

    public void testRemoteInputBuilder_setEditChoicesBeforeSending() throws Throwable {
        RemoteInput input =
                new RemoteInput.Builder(RESULT_KEY)
                        .setChoices(new CharSequence[]{"first", "second"})
                        .setEditChoicesBeforeSending(
                                RemoteInput.EDIT_CHOICES_BEFORE_SENDING_ENABLED)
                        .build();
        assertEquals(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_ENABLED,
                input.getEditChoicesBeforeSending());
    }

    public void testRemoteInputBuilder_setEditChoicesBeforeSendingRequiresFreeInput()
            throws Throwable {
        RemoteInput.Builder builder =
                new RemoteInput.Builder(RESULT_KEY)
                        .setEditChoicesBeforeSending(
                                RemoteInput.EDIT_CHOICES_BEFORE_SENDING_ENABLED)
                        .setAllowFreeFormInput(false);
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            // expected.
        }
    }

    @Test
    public void testRemoteInputBuilder_addAndGetDataResultsFromIntent() throws Throwable {
        Uri uri = Uri.parse("Some Uri");
        RemoteInput input = newDataOnlyRemoteInput();
        Intent intent = new Intent();
        Map<String, Uri> putResults = new HashMap<>();
        putResults.put(MIME_TYPE, uri);
        RemoteInput.addDataResultToIntent(input, intent, putResults);

        verifyIntentHasDataResults(intent, uri);
    }

    @Test
    public void testRemoteInputBuilder_addAndGetTextResultsFromIntent() throws Throwable {
        CharSequence charSequence = "value doesn't matter";
        RemoteInput input = newTextRemoteInput();
        Intent intent = new Intent();
        Bundle putResults = new Bundle();
        putResults.putCharSequence(input.getResultKey(), charSequence);
        RemoteInput[] arr = new RemoteInput[1];
        arr[0] = input;
        RemoteInput.addResultsToIntent(arr, intent, putResults);

        verifyIntentHasTextResults(intent, charSequence);
    }

    @Test
    public void testRemoteInputBuilder_addAndGetDataAndTextResultsFromIntentDataFirst()
            throws Throwable {
        CharSequence charSequence = "value doesn't matter";
        Uri uri = Uri.parse("Some Uri");
        RemoteInput input = newTextAndDataRemoteInput();
        Intent intent = new Intent();

        addDataResultsToIntent(input, intent, uri);
        addTextResultsToIntent(input, intent, charSequence);
        RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT);

        verifyIntentHasTextResults(intent, charSequence);
        verifyIntentHasDataResults(intent, uri);
    }

    @Test
    public void testRemoteInputBuilder_addAndGetDataAndTextResultsFromIntentTextFirst()
            throws Throwable {
        CharSequence charSequence = "value doesn't matter";
        Uri uri = Uri.parse("Some Uri");
        RemoteInput input = newTextAndDataRemoteInput();
        Intent intent = new Intent();

        addTextResultsToIntent(input, intent, charSequence);
        addDataResultsToIntent(input, intent, uri);
        RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_CHOICE);

        verifyIntentHasTextResults(intent, charSequence);
        verifyIntentHasDataResults(intent, uri);
    }

    @Test
    public void testGetResultsSource_emptyIntent() {
        Intent intent = new Intent();

        assertEquals(RemoteInput.SOURCE_FREE_FORM_INPUT, RemoteInput.getResultsSource(intent));
    }

    @Test
    public void testGetResultsSource_addDataAndTextResults() {
        CharSequence charSequence = "value doesn't matter";
        Uri uri = Uri.parse("Some Uri");
        RemoteInput input = newTextAndDataRemoteInput();
        Intent intent = new Intent();

        addTextResultsToIntent(input, intent, charSequence);
        addDataResultsToIntent(input, intent, uri);

        assertEquals(RemoteInput.SOURCE_FREE_FORM_INPUT, RemoteInput.getResultsSource(intent));
    }

    @Test
    public void testGetResultsSource_setSource() {
        Intent intent = new Intent();

        RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_CHOICE);

        assertEquals(RemoteInput.SOURCE_CHOICE, RemoteInput.getResultsSource(intent));
    }

    @Test
    public void testGetResultsSource_setSourceAndAddDataAndTextResults() {
        CharSequence charSequence = "value doesn't matter";
        Uri uri = Uri.parse("Some Uri");
        RemoteInput input = newTextAndDataRemoteInput();
        Intent intent = new Intent();

        RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_CHOICE);
        addTextResultsToIntent(input, intent, charSequence);
        addDataResultsToIntent(input, intent, uri);

        assertEquals(RemoteInput.SOURCE_CHOICE, RemoteInput.getResultsSource(intent));
    }

    private static void addTextResultsToIntent(RemoteInput input, Intent intent,
            CharSequence charSequence) {
        Bundle textResults = new Bundle();
        textResults.putCharSequence(input.getResultKey(), charSequence);
        RemoteInput[] arr = new RemoteInput[1];
        arr[0] = input;
        RemoteInput.addResultsToIntent(arr, intent, textResults);
    }

    private static void addDataResultsToIntent(RemoteInput input, Intent intent, Uri uri) {
        Map<String, Uri> dataResults = new HashMap<>();
        dataResults.put(MIME_TYPE, uri);
        RemoteInput.addDataResultToIntent(input, intent, dataResults);
    }

    private static void verifyIntentHasTextResults(Intent intent, CharSequence expected) {
        Bundle getResults = RemoteInput.getResultsFromIntent(intent);
        assertNotNull(getResults);
        assertTrue(getResults.containsKey(RESULT_KEY));
        assertEquals(expected, getResults.getCharSequence(RESULT_KEY, "default"));
    }

    private static void verifyIntentHasDataResults(Intent intent, Uri expectedUri) {
        Map<String, Uri> getResults = RemoteInput.getDataResultsFromIntent(intent, RESULT_KEY);
        assertNotNull(getResults);
        assertEquals(1, getResults.size());
        assertTrue(getResults.containsKey(MIME_TYPE));
        assertEquals(expectedUri, getResults.get(MIME_TYPE));
    }

    private static RemoteInput newTextRemoteInput() {
        return new RemoteInput.Builder(RESULT_KEY).build();  // allowFreeForm defaults to true
    }

    private static RemoteInput newChoicesOnlyRemoteInput() {
        CharSequence[] choices = new CharSequence[2];
        choices[0] = "first";
        choices[1] = "second";
        return new RemoteInput.Builder(RESULT_KEY)
            .setAllowFreeFormInput(false)
            .setChoices(choices)
            .build();
    }

    private static RemoteInput newDataOnlyRemoteInput() {
        return new RemoteInput.Builder(RESULT_KEY)
            .setAllowFreeFormInput(false)
            .setAllowDataType(MIME_TYPE, true)
            .build();
    }

    private static RemoteInput newTextAndDataRemoteInput() {
        return new RemoteInput.Builder(RESULT_KEY)
            .setAllowFreeFormInput(true)
            .setAllowDataType(MIME_TYPE, true)
            .build();
    }
}
