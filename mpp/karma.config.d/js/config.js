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


const path = require("node:path");

config.browserConsoleLogOptions.level = "debug";

const basePath = config.basePath;
const rootPath = path.resolve(basePath, "..", "..", "..", "..", "..", "..");
const karmaPath = path.resolve(rootPath, "mpp", "karma.config.d")
const configPath = path.resolve(karmaPath, "js");

const {configLaunchers} = require(path.resolve(karmaPath, "web", "commonKarmaConfig.js"))

const debug = message => console.error(`[karma-config] ${message}`);
debug(`karma basePath: ${basePath}`);
debug(`karma rootPath: ${rootPath}`);

// This enables running tests on a custom html page without iframe
const staticFilesDir =  path.resolve(configPath, "static");
config.customContextFile = path.resolve(staticFilesDir, "compose_context.html");

// https://github.com/JetBrains/compose-multiplatform-core/pull/1008#issuecomment-1956354231
config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 10000;

function KarmaWebpackOutputFramework(config) {
    // This controller is instantiated and set during the preprocessor phase by the karma-webpack plugin
    const controller = config.__karmaWebpackController;

    // only if webpack has instantiated its controller
    if (!controller) {
        console.warn(
            "Webpack has not instantiated controller yet.\n" +
            "Check if you have enabled webpack preprocessor and framework before this framework"
        )
        return
    }

    config.files.push({
        pattern: `${staticFilesDir}/**/*.js`,
        included: true,
        served: true,
        watched: false
    });

    config.files.push({
        pattern: `${controller.outputPath}/**/*`,
        included: false,
        served: true,
        watched: false
    });
}

const KarmaWebpackOutputPlugin = {
    'framework:webpack-output': ['factory', KarmaWebpackOutputFramework],
};

config.plugins.push(KarmaWebpackOutputPlugin);
config.frameworks.push("webpack-output");

config.files.push(
    {pattern: path.resolve(basePath, "kotlin", "skiko.wasm"), included: false, served: true, watched: false},
    {pattern: path.resolve(basePath, "kotlin", "skiko.mjs"), included: true, served: true, watched: false, type: 'module'},
    {pattern: path.resolve(basePath, "kotlin", "js-reexport-symbols.mjs"), included: false, served: true, watched: false, type: 'module'},
);

config.proxies = {
    "/skiko.mjs": path.resolve(basePath, "kotlin", "skiko.mjs"),
    "/skiko.wasm": path.resolve(basePath, "kotlin", "skiko.wasm"),
    "/js-reexport-symbols.mjs": path.resolve(basePath, "kotlin", "js-reexport-symbols.mjs"),
}



configLaunchers(config);

// A workaround from https://android-review.googlesource.com/c/platform/frameworks/support/+/3413540
(function() {
    const originalExit = process.exit;
    process.exit = function(code) {
        console.log('Delaying exit for logs...');
        // This extra time allows any pending I/O operations (such as printing logs) to complete,
        // preventing flakiness when Kotlin marks a test as complete.
        setTimeout(() => {
            originalExit(code);
        }, 5000);
    };
})();
