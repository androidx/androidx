/*
 * Copyright (C) 2024 The Android Open Source Project
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

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("com.android.application")
    id("kotlin-android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "androidx.camera.integration.testingtestapp"

    defaultConfig {
        testInstrumentationRunner = "androidx.camera.integration.testingtestapp.utils.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }
}

dependencies {

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Hilt Dependency Injection
    implementation(libs.hiltAndroid)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
    ksp(libs.hiltCompiler)

    // Arch Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Compose
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-core:1.6.8")

    // Camera
    implementation(project(":camera:camera-extensions"))
    implementation(project(":camera:camera-view"))
    implementation(project(":camera:camera-camera2"))
    implementation(project(":camera:camera-lifecycle"))

     // Testing
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.testRules)
    androidTestImplementation(libs.testRunner)
    implementation(libs.testRunner)
    implementation(libs.hiltAndroidTesting)
    implementation(libs.testCore)
}
