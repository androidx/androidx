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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteInputTest extends BaseInstrumentationTestCase<TestActivity> {
    private static final String RESULT_KEY = "result_key";  // value doesn't matter
    private static final String MIME_TYPE = "mimeType";  // value doesn't matter

    public RemoteInputTest() {
        super(TestActivity.class);
    }

    @Test
    public void testRemoteInputBuilder_setDataOnly() throws Throwable {
        RemoteInput input = newDataOnlyRemoteInput();

        assertTrue(input.isDataOnly());
        assertFalse(input.getAllowFreeFormInput());
        assertTrue(input.getChoices() == null || input.getChoices().length == 0);
        assertEquals(1, input.getAllowedDataTypes().size());
        assertTrue(input.getAllowedDataTypes().contains(MIME_TYPE));
    }

    @Test
    public void testRemoteInputBuilder_setTextOnly() throws Throwable {
        RemoteInput input = newTextRemoteInput();

        assertFalse(input.isDataOnly());
        assertTrue(input.getAllowFreeFormInput());
        assertTrue(input.getChoices() == null || input.getChoices().length == 0);
        assertTrue(input.getAllowedDataTypes() == null || input.getAllowedDataTypes().isEmpty());
    }

    @Test
    public void testRemoteInputBuilder_setChoicesOnly() throws Throwable {
        RemoteInput input = newChoicesOnlyRemoteInput();

        assertFalse(input.isDataOnly());
        assertFalse(input.getAllowFreeFormInput());
        assertTrue(input.getChoices() != null && input.getChoices().length > 0);
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
        assertEquals(1, input.getAllowedDataTypes().size());
        assertTrue(input.getAllowedDataTypes().contains(MIME_TYPE));
    }

    @SdkSuppress(minSdkVersion = 17)
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

    @SdkSuppress(minSdkVersion = 17)
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

    @SdkSuppress(minSdkVersion = 17)
    @Test
    public void testRemoteInputBuilder_addAndGetDataAndTextResultsFromIntentDataFirst()
            throws Throwable {
        CharSequence charSequence = "value doesn't matter";
        Uri uri = Uri.parse("Some Uri");
        RemoteInput input =
                new RemoteInput.Builder(RESULT_KEY)
                .setAllowDataType(MIME_TYPE, true)
                .build();
        Intent intent = new Intent();

        Map<String, Uri> dataResults = new HashMap<>();
        dataResults.put(MIME_TYPE, uri);
        RemoteInput.addDataResultToIntent(input, intent, dataResults);

        Bundle textResults = new Bundle();
        textResults.putCharSequence(input.getResultKey(), charSequence);
        RemoteInput[] arr = new RemoteInput[1];
        arr[0] = input;
        RemoteInput.addResultsToIntent(arr, intent, textResults);

        verifyIntentHasTextResults(intent, charSequence);
        verifyIntentHasDataResults(intent, uri);
    }

    @SdkSuppress(minSdkVersion = 17)
    @Test
    public void testRemoteInputBuilder_addAndGetDataAndTextResultsFromIntentTextFirst()
            throws Throwable {
        CharSequence charSequence = "value doesn't matter";
        Uri uri = Uri.parse("Some Uri");
        RemoteInput input =
                new RemoteInput.Builder(RESULT_KEY)
                .setAllowDataType(MIME_TYPE, true)
                .build();
        Intent intent = new Intent();

        Bundle textResults = new Bundle();
        textResults.putCharSequence(input.getResultKey(), charSequence);
        RemoteInput[] arr = new RemoteInput[1];
        arr[0] = input;
        RemoteInput.addResultsToIntent(arr, intent, textResults);

        Map<String, Uri> dataResults = new HashMap<>();
        dataResults.put(MIME_TYPE, uri);
        RemoteInput.addDataResultToIntent(input, intent, dataResults);

        verifyIntentHasTextResults(intent, charSequence);
        verifyIntentHasDataResults(intent, uri);
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
}
