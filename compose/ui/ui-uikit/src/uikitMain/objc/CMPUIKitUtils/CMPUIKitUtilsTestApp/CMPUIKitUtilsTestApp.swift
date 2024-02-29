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

import SwiftUI

@main
struct CMPUIKitUtilsTestApp: App {   
    let uiLogger = CMPOSLogger(categoryName: "androidx.compose.ui")
    let runtimeLogger = CMPOSLogger(categoryName: "androidx.compose.runtime")
    var body: some Scene {
        WindowGroup {
            Color.black
                .onAppear {
                    Task {
                        if #available(iOS 16.0, *) {
                            for i in 0..<100 {
                                let uiInterval = uiLogger.beginIntervalNamed("\(i)")
                                let runtimeInterval = runtimeLogger.beginIntervalNamed("\(i)")
                                try await Task.sleep(for: Duration.milliseconds(4))
                                uiLogger.end(uiInterval)
                                runtimeLogger.end(runtimeInterval)
                                try await Task.sleep(for: Duration.milliseconds(4))
                            }
                        }
                    }
                }
        }
    }
}
