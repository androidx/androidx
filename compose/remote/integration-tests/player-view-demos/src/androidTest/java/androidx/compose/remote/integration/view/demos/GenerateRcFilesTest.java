/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.integration.view.demos.examples.Cube3DKt;
import androidx.compose.remote.integration.view.demos.examples.DemoGraphsKt;
import androidx.compose.remote.integration.view.demos.examples.DemoUtilsKt;
import androidx.compose.remote.integration.view.demos.examples.PressureGaugeKt;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test to generate .rc files from the demo examples.
 */
public class GenerateRcFilesTest {

    @Test
    public void generateRcFiles() {
        DemoUtilsKt.addHeaderParam(Header.TEST_TIME, System.currentTimeMillis());
        DemoUtilsKt.addHeaderParam(Header.TEST_AFTER, 5.0f);

        saveRcFile("pressure_gauge.rc", PressureGaugeKt.demoPressureGauge());
        saveRcFile("demo_graphs.rc", DemoGraphsKt.demoGraphs());
        saveRcFile("demo_graphs2.rc", DemoGraphsKt.demoGraphs2());
        saveRcFile("cube_3d.rc", Cube3DKt.cube3d().getWriter());
    }

    private void saveRcFile(String fileName, RemoteComposeWriter writer) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                fileName);
        byte[] buffer = writer.buffer();
        int size = writer.bufferSize();

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(buffer, 0, size);
            Log.d("GenerateRcFilesTest", "Saved " + file.getAbsolutePath());
            System.out.println("Generated: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("GenerateRcFilesTest", "Failed to save .rc file: " + fileName, e);
        }
    }
}
