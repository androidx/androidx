/*
 * Copyright 2024 The Android Open Source Project
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

import org.gradle.api.Project

class BuildDirectoryHelper {
    private static File getOutDirectory(File checkoutRoot) {
        def outDir = System.env.OUT_DIR
        if (outDir == null) {
            outDir = new File("${checkoutRoot}/out")
        } else {
            outDir = new File(outDir)
        }
        return outDir
    }

    static void chooseBuildDirectory(File checkoutRoot, String rootProjectName, Project project) {
        File outDir = getOutDirectory(checkoutRoot)
        project.ext.outDir = outDir
        // Expected out directory structure for :foo:bar is out/androidx/foo/bar
        project.layout.buildDirectory.set(
                new File(outDir, "$rootProjectName/${project.path.replace(":", "/")}/build").canonicalFile
        )
    }
}

def init = new Properties()
ext.init = init
ext.init.chooseBuildDirectory = (new BuildDirectoryHelper()).&chooseBuildDirectory
