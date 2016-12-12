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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiresApi(16)
class RemoteInputCompatJellybean {
    /** Label used to denote the clip data type used for remote input transport */
    public static final String RESULTS_CLIP_LABEL = "android.remoteinput.results";

    /** Extra added to a clip data intent object to hold the results bundle. */
    public static final String EXTRA_RESULTS_DATA = "android.remoteinput.resultsData";

    /** Extra added to a clip data intent object to hold the data results bundle. */
    private static final String EXTRA_DATA_TYPE_RESULTS_DATA =
            "android.remoteinput.dataTypeResultsData";

    private static final String KEY_RESULT_KEY = "resultKey";
    private static final String KEY_LABEL = "label";
    private static final String KEY_CHOICES = "choices";
    private static final String KEY_ALLOW_FREE_FORM_INPUT = "allowFreeFormInput";
    private static final String KEY_EXTRAS = "extras";
    private static final String KEY_ALLOWED_DATA_TYPES = "allowedDataTypes";

    static RemoteInputCompatBase.RemoteInput fromBundle(Bundle data,
            RemoteInputCompatBase.RemoteInput.Factory factory) {
        ArrayList<String> allowedDataTypesAsList = data.getStringArrayList(KEY_ALLOWED_DATA_TYPES);
        Set<String> allowedDataTypes = new HashSet<>();
        if (allowedDataTypesAsList != null) {
            for (String type : allowedDataTypesAsList) {
                allowedDataTypes.add(type);
            }
        }
        return factory.build(data.getString(KEY_RESULT_KEY),
                data.getCharSequence(KEY_LABEL),
                data.getCharSequenceArray(KEY_CHOICES),
                data.getBoolean(KEY_ALLOW_FREE_FORM_INPUT),
                data.getBundle(KEY_EXTRAS),
                allowedDataTypes);
    }

    static Bundle toBundle(RemoteInputCompatBase.RemoteInput remoteInput) {
        Bundle data = new Bundle();
        data.putString(KEY_RESULT_KEY, remoteInput.getResultKey());
        data.putCharSequence(KEY_LABEL, remoteInput.getLabel());
        data.putCharSequenceArray(KEY_CHOICES, remoteInput.getChoices());
        data.putBoolean(KEY_ALLOW_FREE_FORM_INPUT, remoteInput.getAllowFreeFormInput());
        data.putBundle(KEY_EXTRAS, remoteInput.getExtras());

        Set<String> allowedDataTypes = remoteInput.getAllowedDataTypes();
        if (allowedDataTypes != null && !allowedDataTypes.isEmpty()) {
            ArrayList<String> allowedDataTypesAsList = new ArrayList<>(allowedDataTypes.size());
            for (String type : allowedDataTypes) {
                allowedDataTypesAsList.add(type);
            }
            data.putStringArrayList(KEY_ALLOWED_DATA_TYPES, allowedDataTypesAsList);
        }
        return data;
    }

    static RemoteInputCompatBase.RemoteInput[] fromBundleArray(Bundle[] bundles,
            RemoteInputCompatBase.RemoteInput.Factory factory) {
        if (bundles == null) {
            return null;
        }
        RemoteInputCompatBase.RemoteInput[] remoteInputs = factory.newArray(bundles.length);
        for (int i = 0; i < bundles.length; i++) {
            remoteInputs[i] = fromBundle(bundles[i], factory);
        }
        return remoteInputs;
    }

    static Bundle[] toBundleArray(RemoteInputCompatBase.RemoteInput[] remoteInputs) {
        if (remoteInputs == null) {
            return null;
        }
        Bundle[] bundles = new Bundle[remoteInputs.length];
        for (int i = 0; i < remoteInputs.length; i++) {
            bundles[i] = toBundle(remoteInputs[i]);
        }
        return bundles;
    }

    static Bundle getResultsFromIntent(Intent intent) {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) {
            return null;
        }
        return clipDataIntent.getExtras().getParcelable(EXTRA_RESULTS_DATA);
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

    static void addResultsToIntent(RemoteInputCompatBase.RemoteInput[] remoteInputs, Intent intent,
            Bundle results) {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) {
            clipDataIntent = new Intent();  // First time we've added a result.
        }
        Bundle resultsBundle = clipDataIntent.getBundleExtra(EXTRA_RESULTS_DATA);
        if (resultsBundle == null) {
            resultsBundle = new Bundle();
        }
        for (RemoteInputCompatBase.RemoteInput remoteInput : remoteInputs) {
            Object result = results.get(remoteInput.getResultKey());
            if (result instanceof CharSequence) {
                resultsBundle.putCharSequence(remoteInput.getResultKey(), (CharSequence) result);
            }
        }
        clipDataIntent.putExtra(EXTRA_RESULTS_DATA, resultsBundle);
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
    }

    /**
     * Same as {@link #addResultsToIntent} but for setting data results.
     * @param remoteInput The remote input for which results are being provided
     * @param intent The intent to add remote input results to. The {@link ClipData}
     *               field of the intent will be modified to contain the results.
     * @param results A map of mime type to the Uri result for that mime type.
     */
    public static void addDataResultToIntent(RemoteInput remoteInput, Intent intent,
            Map<String, Uri> results) {
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
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
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
        if (!clipDescription.getLabel().equals(RESULTS_CLIP_LABEL)) {
            return null;
        }
        return clipData.getItemAt(0).getIntent();
    }
}
