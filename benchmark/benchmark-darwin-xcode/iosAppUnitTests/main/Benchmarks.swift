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

import Foundation
import XCTest
import AndroidXDarwinBenchmarks

class BenchmarkTest: XCTestCase {
    var testCase: TestCase? = nil
    override func setUpWithError() throws {
        testCase!.setUp()
    }

    override class var defaultTestSuite: XCTestSuite {
        let suite = XCTestSuite(forTestCaseClass: BenchmarkTest.self)
        let testCases = TestCases.shared.benchmarkTests()
        for testCase in testCases {
            let test = BenchmarkTest(selector: #selector(runBenchmark))
            test.testCase = testCase
            suite.addTest(test)
        }
        return suite
    }

    @objc func runBenchmark() {
        // https://masilotti.com/xctest-name-readability/
        XCTContext.runActivity(named: testCase!.testDescription()) { _ -> Void in
            // Run the actual benchmark
            let context = TestCaseContextWrapper(context: self)
            testCase?.benchmark(context: context)
        }
    }
}
