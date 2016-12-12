/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.app;

import android.app.RemoteInput;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Map;

@RequiresApi(20)
class RemoteInputCompatApi20 {
    /** Extra added to a clip data intent object to hold the data results bundle. */
    private static final String EXTRA_DATA_TYPE_RESULTS_DATA =
            "android.remoteinput.dataTypeResultsData";

    static RemoteInputCompatBase.RemoteInput[] toCompat(RemoteInput[] srcArray,
            RemoteInputCompatBase.RemoteInput.Factory factory) {
        if (srcArray == null) {
            return null;
        }
        RemoteInputCompatBase.RemoteInput[] result = factory.newArray(srcArray.length);
        for (int i = 0; i < srcArray.length; i++) {
            RemoteInput src = srcArray[i];
            result[i] = factory.build(src.getResultKey(), src.getLabel(), src.getChoices(),
                    src.getAllowFreeFormInput(), src.getExtras(), null);
        }
        return result;
    }

    static RemoteInput[] fromCompat(RemoteInputCompatBase.RemoteInput[] srcArray) {
        if (srcArray == null) {
            return null;
        }
        RemoteInput[] result = new RemoteInput[srcArray.length];
        for (int i = 0; i < srcArray.length; i++) {
            RemoteInputCompatBase.RemoteInput src = srcArray[i];
            result[i] = new RemoteInput.Builder(src.getResultKey())
                    .setLabel(src.getLabel())
                    .setChoices(src.getChoices())
                    .setAllowFreeFormInput(src.getAllowFreeFormInput())
                    .addExtras(src.getExtras())
                    .build();
        }
        return result;
    }

    static Bundle getResultsFromIntent(Intent intent) {
        return RemoteInput.getResultsFromIntent(intent);
    }

    static Map<String, Uri> getDataResultsFromIntent(Intent intent, String remoteInputResultKey) {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) {
            return null;
        }
        Map<String, Uri> results = new HashMap<>();
        Bundle extras = clipDataIntent.getExtras();
        for (String key : extras.keySet()) {
            if (key.startsWith(EXTRA_DATA_TYPE_RESULTS_DATA)) {
                String mimeType = key.substring(EXTRA_DATA_TYPE_RESULTS_DATA.length());
                if (mimeType == null || mimeType.isEmpty()) {
                    continue;
                }
                Bundle bundle = clipDataIntent.getBundleExtra(key);
                String uriStr = bundle.getString(remoteInputResultKey);
                if (uriStr == null || uriStr.isEmpty()) {
                    continue;
                }
                results.put(mimeType, Uri.parse(uriStr));
            }
        }
        return results.isEmpty() ? null : results;
    }

    static void addResultsToIntent(RemoteInputCompatBase.RemoteInput[] remoteInputs,
            Intent intent, Bundle results) {
        // Implementations of RemoteInput#addResultsToIntent prior to SDK 26 don't actually add
        // results, they wipe out old results and insert the new one. Work around that by preserving
        // old results.
        Bundle existingTextResults = getResultsFromIntent(intent);
        if (existingTextResults == null) {
            existingTextResults = results;
        } else {
            existingTextResults.putAll(results);
        }
        for (RemoteInputCompatBase.RemoteInput input : remoteInputs) {
            // Data results are also wiped out. So grab them and add them back in.
            Map<String, Uri> existingDataResults =
                    getDataResultsFromIntent(intent, input.getResultKey());
            RemoteInputCompatBase.RemoteInput[] arr = new RemoteInputCompatBase.RemoteInput[1];
            arr[0] = input;
            RemoteInput.addResultsToIntent(fromCompat(arr), intent, existingTextResults);
            if (existingDataResults != null) {
                addDataResultToIntent(input, intent, existingDataResults);
            }
        }
    }

    /**
     * Same as {@link #addResultsToIntent} but for setting data results.
     * @param remoteInput The remote input for which results are being provided
     * @param intent The intent to add remote input results to. The {@link ClipData}
     *               field of the intent will be modified to contain the results.
     * @param results A map of mime type to the Uri result for that mime type.
     */
    public static void addDataResultToIntent(RemoteInputCompatBase.RemoteInput remoteInput,
            Intent intent, Map<String, Uri> results) {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) {
            clipDataIntent = new Intent();  // First time we've added a result.
        }
        for (Map.Entry<String, Uri> entry : results.entrySet()) {
            String mimeType = entry.getKey();
            Uri uri = entry.getValue();
            if (mimeType == null) {
                continue;
            }
            Bundle resultsBundle =
                    clipDataIntent.getBundleExtra(getExtraResultsKeyForData(mimeType));
            if (resultsBundle == null) {
                resultsBundle = new Bundle();
            }
            resultsBundle.putString(remoteInput.getResultKey(), uri.toString());
            clipDataIntent.putExtra(getExtraResultsKeyForData(mimeType), resultsBundle);
        }
        intent.setClipData(ClipData.newIntent(RemoteInput.RESULTS_CLIP_LABEL, clipDataIntent));
    }

    private static String getExtraResultsKeyForData(String mimeType) {
        return EXTRA_DATA_TYPE_RESULTS_DATA + mimeType;
    }

    private static Intent getClipDataIntentFromIntent(Intent intent) {
        ClipData clipData = intent.getClipData();
        if (clipData == null) {
            return null;
        }
        ClipDescription clipDescription = clipData.getDescription();
        if (!clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT)) {
            return null;
        }
        if (!clipDescription.getLabel().equals(RemoteInput.RESULTS_CLIP_LABEL)) {
            return null;
        }
        return clipData.getItemAt(0).getIntent();
    }
}
