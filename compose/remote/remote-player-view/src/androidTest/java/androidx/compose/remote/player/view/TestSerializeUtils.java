/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.view;

import static androidx.compose.remote.player.view.TestUtils.createDocument;

import android.content.Context;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.serialization.yaml.YAMLSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestSerializeUtils {

    /**
     * Create a byte buffer from a doc
     *
     * @param run
     * @return
     */
    public static byte[] createDoc(TestUtils.Callback run) {
        DebugPlayerContext debugContext = new DebugPlayerContext();
        RemoteComposeDocument doc = createDocument(debugContext, run);
        return doc.getDocument().getBuffer().getBuffer().cloneBytes();
    }

    /**
     * Convert to YAML
     *
     * @param rawDoc
     * @return
     */
    public static String toYamlString(byte[] rawDoc) {
        return toYamlString(rawDoc, null);
    }

    /**
     * Convert to YAML
     *
     * @param rawDoc
     * @param sub
     * @return
     */
    public static String toYamlString(byte[] rawDoc, String sub) {
        RemoteComposeDocument doc = new RemoteComposeDocument(rawDoc);

        DebugPlayerContext debugContext = new DebugPlayerContext();
        doc.paint(debugContext, Theme.UNSPECIFIED);
        YAMLSerializer serializer = new YAMLSerializer();

        doc.serialize(serializer.serializeMap());
        if (sub != null) {
            String s = serializer.toFlatString();
            return TestUtils.grep(s, sub).replace("  ", " ");
        }
        return serializer.toSimpleString();
    }

    static String loadFileFromRaw(Context context, int id) {
        try {
            int resourceId = id;

            System.out.println(
                    "read \"" + context.getResources().getResourceName(resourceId) + "\"");
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    context.getResources().openRawResource(resourceId)));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            reader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }
}
