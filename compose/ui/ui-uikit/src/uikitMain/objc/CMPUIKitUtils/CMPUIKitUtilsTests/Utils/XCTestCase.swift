/*
 * Copyright 2022 The Android Open Source Project
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
    /// Non UI Thread blocking delay for at least given time interval.
    func delay(_ delay: TimeInterval) {
        let baseExpectation = expectation(description: name)
        DispatchQueue.global(qos: .userInitiated).asyncAfter(deadline: .now() + delay) {
            DispatchQueue.main.async {
                baseExpectation.fulfill()
            }
        }
        wait(for: [baseExpectation], timeout: delay + 5)
    }

    /// Awaits for expectation without blocking UI thread.
    func wait(for expectation: @escaping () -> Bool,
              timeout: TimeInterval = 1.0,
              file: String = #file,
              function: String = #function,
              line: Int = #line) {
        let start = Date()
        var isExpected = expectation()
        while !isExpected && Date().timeIntervalSince(start) < timeout {
            delay(0.001)
            isExpected = expectation()
        }
        if !isExpected {
            XCTFail("Timeout reached for expectation at \(file) - \(function) line \(line)")
        }
    }
}
