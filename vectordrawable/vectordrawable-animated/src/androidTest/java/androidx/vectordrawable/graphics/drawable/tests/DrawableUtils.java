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

package androidx.vectordrawable.graphics.drawable.tests;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class DrawableUtils {

    private static final String LOGTAG = DrawableUtils.class.getSimpleName();

    // This is only for debugging or golden image (re)generation purpose.
    public static void saveVectorDrawableIntoPNG(Resources resource, Bitmap bitmap, int resId,
            String filename) throws IOException {
        // Save the image to the disk.
        FileOutputStream out = null;
        try {
            String outputFolder = "/sdcard/temp/";
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            String fileTitle = "unname";
            if (resId >= 0) {
                String originalFilePath = resource.getString(resId);
                File originalFile = new File(originalFilePath);
                String fileFullName = originalFile.getName();
                fileTitle = fileFullName.substring(0, fileFullName.lastIndexOf("."));
                if (filename != null) {
                    fileTitle += "_" + filename;
                }
            } else if (filename != null) {
                fileTitle = filename;
            }
            String outputFilename = outputFolder + fileTitle + "_golden.png";
            File outputFile = new File(outputFilename);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            out = new FileOutputStream(outputFile, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.v(LOGTAG, "Write test No." + outputFilename + " to file successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
