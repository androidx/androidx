/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.integration.testapp.sherlockholmes;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.model.Arguments;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * A Worker that counts words of length > 3 and stores the results.
 */
public class TextMappingWorker extends Worker {

    private static final String INPUT_FILE = "input_file";
    private static final String OUTPUT_FILE = "output_file";

    private Map<String, Integer> mWordCount = new HashMap<>();

    /**
     * Creates a {@link Work.Builder} with the necessary arguments.
     *
     * @param inputFile The input file to process
     * @param outputFile The output file for the processed results
     * @return A {@link Work.Builder} with these arguments
     */
    public static Work.Builder create(String inputFile, String outputFile) {
        Arguments args = new Arguments.Builder()
                .putString(INPUT_FILE, inputFile)
                .putString(OUTPUT_FILE, outputFile)
                .build();
        return new Work.Builder(TextMappingWorker.class).withArguments(args);
    }

    @Override
    public @WorkerResult int doWork() {
        Arguments args = getArguments();
        String inputFileName = args.getString(INPUT_FILE, null);
        String outputFileName = args.getString(OUTPUT_FILE, null);

        AssetManager assetManager = getAppContext().getAssets();
        InputStream inputStream = null;
        Scanner scanner = null;
        try {
            inputStream = assetManager.open(inputFileName);
            scanner = new Scanner(inputStream);
            while (scanner.hasNext()) {
                String word = scanner.next();
                if (word.length() > 3) {
                    int count = 1;
                    if (mWordCount.containsKey(word)) {
                        count = mWordCount.get(word) + 1;
                    }
                    mWordCount.put(word, count);
                }
            }
        } catch (IOException e) {
            return WORKER_RESULT_FAILURE;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }

        FileOutputStream fileOutputStream = null;
        DataOutputStream dataOutputStream = null;
        try {
            fileOutputStream = getAppContext().openFileOutput(outputFileName, Context.MODE_PRIVATE);
            dataOutputStream = new DataOutputStream(fileOutputStream);
            for (Map.Entry<String, Integer> entry : mWordCount.entrySet()) {
                dataOutputStream.writeUTF(entry.getKey());
                dataOutputStream.writeInt(entry.getValue());
            }
        } catch (IOException e) {
            return WORKER_RESULT_FAILURE;
        } finally {
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }

        Log.d("Map", "Mapping finished for " + inputFileName);
        return WORKER_RESULT_SUCCESS;
    }
}
