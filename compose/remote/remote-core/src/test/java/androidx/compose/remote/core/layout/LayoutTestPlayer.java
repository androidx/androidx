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

package androidx.compose.remote.core.layout;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.serialization.yaml.YAMLSerializer;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LayoutTestPlayer {

    /**
     * Utility test function executing TestOperation on a document
     *
     * @param writer the writer generating the tested document
     * @param ops a list of TestOperation
     * @param testParameters parameters for the test
     */
    @SuppressWarnings("unchecked")
    public static void play(RemoteComposeWriter writer, ArrayList<TestOperation> ops,
            TestParameters testParameters) {
        List<Map<String, Object>> commands = new ArrayList<>();

        int tw1 = 1000;
        int th1 = 1000;
        byte[] byteBuffer = writer.buffer();
        int bufferSize = writer.bufferSize();
        CoreDocument doc = new CoreDocument();
        RemoteComposeBuffer buffer = RemoteComposeBuffer.fromInputStream(
                new ByteArrayInputStream(byteBuffer, 0, bufferSize));
        doc.initFromBuffer(buffer);
        MockRemoteContext debugContext = new MockRemoteContext();
        debugContext.setAnimationEnabled(false);
        debugContext.setDensity(1f);
        debugContext.mWidth = tw1;
        debugContext.mHeight = th1;
        doc.initializeContext(debugContext);

        doc.paint(debugContext, Theme.UNSPECIFIED);
        int needsRepaint = doc.needsRepaint();
        int count = 0;
        int max = 100;

        // Initial paint of the document
        while (needsRepaint != 0 && count < max) {
            debugContext.currentTime += needsRepaint;
            doc.paint(debugContext, Theme.UNSPECIFIED);
            count++;
            needsRepaint = doc.needsRepaint();
        }

        YAMLSerializer serializer = new YAMLSerializer();
        doc.serialize(serializer.serializeMap());
        Map<String, Object> snapshot = (Map<String, Object>) serializer.toObject();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", "Initial State");
        map.put("command", "Capture");
        map.put("result", snapshot);
        commands.add(map);

        // Apply the test operations
        for (TestOperation op : ops) {
            boolean forceRepaint = op.apply(debugContext, doc, testParameters, commands);

            // repaint as needed
            needsRepaint = doc.needsRepaint();
            if (needsRepaint == 0 && forceRepaint) {
                needsRepaint = 1;
            }
            count = 0;
            while (needsRepaint != 0 && count < max) {
                debugContext.currentTime += needsRepaint;
                doc.paint(debugContext, Theme.UNSPECIFIED);
                count++;
                needsRepaint = doc.needsRepaint();
            }
        }

        Yaml yaml = new Yaml();
        String yamlString = yaml.dump(commands);
        String testName = testParameters.getName();
        if (testParameters.captureGoldFiles()) {
            File file = new File("tests/" + testName + ".layout");
            System.out.println("Write file to " + file.getPath());
            try {
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(yamlString);
                osw.flush();
                osw.close();
                fos.close();
            } catch (Exception e) {
                // handle exception
            }
        } else {
            Path filePath = Paths.get("tests/" + testName + ".layout");
            try {
                String fileContent = Files.readString(filePath);
                assertEquals("Layout Test", fileContent, yamlString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
