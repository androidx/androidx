/*
 * Copyright 2023 The Android Open Source Project
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

import XCTest

extension XCTestCase {
    /// Awaits for expectation without blocking UI thread.
    @MainActor
    func expect(        
        timeout: TimeInterval,
        line: Int,
        expectation: @escaping () -> Bool
    ) async {
        let start = Date()
        var isExpectationMet = expectation()
        
        while !isExpectationMet && Date().timeIntervalSince(start) < timeout {
            try? await Task.sleep(nanoseconds: 100_000) // 100ms
            isExpectationMet = expectation()            
        }
        
        if !isExpectationMet {
            XCTFail("Timeout at line \(line)")
        }
    }
}
