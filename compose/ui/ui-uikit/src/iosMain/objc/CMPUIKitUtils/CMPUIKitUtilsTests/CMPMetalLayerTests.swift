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

import XCTest
import Metal
import QuartzCore

final class CMPMetalLayerTests: XCTestCase {
    var layer: CMPMetalLayer!

    override func setUpWithError() throws {
        try super.setUpWithError()
        layer = CMPMetalLayer()
        XCTAssertNotNil(layer.device, "Metal device should be initialized")
    }

    override func tearDownWithError() throws {
        layer = nil
        try super.tearDownWithError()
    }

    // MARK: - Basic Initialization Tests

    func testInitialization() {
        XCTAssertNotNil(layer.device, "Device should be initialized")
        XCTAssertEqual(layer.drawableSize, .zero, "Initial drawable size should be zero")
    }

    func testSetDrawableSize() {
        let size = CGSize(width: 100, height: 100)
        layer.drawableSize = size
        XCTAssertEqual(layer.drawableSize, size, "Drawable size should be set")
    }

    // MARK: - Basic Drawable Acquisition Tests

    func testNextDrawableWithValidSize() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable = layer.nextDrawable()
        XCTAssertNotNil(drawable, "Should get a drawable with valid size")
        XCTAssertNotNil(drawable?.surface, "Drawable should have a surface")
        XCTAssertEqual(drawable?.textureSize.width, 100, "Texture width should match")
        XCTAssertEqual(drawable?.textureSize.height, 100, "Texture height should match")
    }

    func testNextDrawableWithZeroSize() {
        layer.drawableSize = .zero

        let drawable = layer.nextDrawable()
        XCTAssertNil(drawable, "Should return nil for zero size")
    }

    func testNextDrawableWithInvalidSize() {
        layer.drawableSize = CGSize(width: -10, height: -10)

        let drawable = layer.nextDrawable()
        XCTAssertNil(drawable, "Should return nil for negative size")
    }

    // MARK: - Pool Exhaustion Tests

    func testPoolCreatesUpToThreeDrawables() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable1 = layer.nextDrawable()
        let drawable2 = layer.nextDrawable()
        let drawable3 = layer.nextDrawable()

        XCTAssertNotNil(drawable1, "Should get first drawable")
        XCTAssertNotNil(drawable2, "Should get second drawable")
        XCTAssertNotNil(drawable3, "Should get third drawable")

        // Verify they are different objects by comparing texture pointers
        XCTAssertTrue(
            drawable1?.surface !== drawable2?.surface,
            "Drawables should have different surfaces"
        )
        XCTAssertTrue(
            drawable2?.surface !== drawable3?.surface,
            "Drawables should have different surfaces"
        )
    }

    func testPoolExhaustionTimeout() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        // Acquire all 3 drawables
        let drawable1 = layer.nextDrawable()
        let drawable2 = layer.nextDrawable()
        let drawable3 = layer.nextDrawable()

        XCTAssertNotNil(drawable1)
        XCTAssertNotNil(drawable2)
        XCTAssertNotNil(drawable3)

        // Try to get a 4th drawable - should timeout and return nil
        let start = Date()
        let drawable4 = layer.nextDrawable()
        let elapsed = Date().timeIntervalSince(start)

        XCTAssertNil(drawable4, "Should return nil when pool is exhausted")
        XCTAssertGreaterThanOrEqual(elapsed, 1.0, "Should wait for timeout (~1 second)")
        XCTAssertLessThan(elapsed, 1.5, "Timeout should not be excessively long")
    }

    // MARK: - Drawable Reuse Tests

    func testDrawableReuseAfterPresent() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable1 = layer.nextDrawable()
        XCTAssertNotNil(drawable1)

        // Present the drawable
        let expectation = self.expectation(description: "Present completion")
        layer.present(drawable1!) {
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)

        // Now we should be able to get another drawable
        let drawable2 = layer.nextDrawable()
        XCTAssertNotNil(drawable2, "Should get a drawable after presenting")
    }

    func testDrawableReuseAfterReturn() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable1 = layer.nextDrawable()
        let drawable2 = layer.nextDrawable()
        let drawable3 = layer.nextDrawable()

        XCTAssertNotNil(drawable1)
        XCTAssertNotNil(drawable2)
        XCTAssertNotNil(drawable3)

        // Return a drawable to the pool
        layer.release(drawable1!)

        // Should be able to get another drawable now
        let drawable4 = layer.nextDrawable()
        XCTAssertNotNil(drawable4, "Should get a drawable after returning one to pool")
    }

    // MARK: - Drawable Size Change Tests

    func testDrawableSizeChangeInvalidatesPreviousDrawables() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable1 = layer.nextDrawable()
        XCTAssertNotNil(drawable1)
        XCTAssertEqual(drawable1?.textureSize.width, 100)
        XCTAssertEqual(drawable1?.textureSize.height, 100)

        // Change size - should invalidate pool
        layer.drawableSize = CGSize(width: 200, height: 200)

        let drawable2 = layer.nextDrawable()
        XCTAssertNotNil(drawable2)
        XCTAssertEqual(drawable2?.textureSize.width, 200, "New drawable should have new size")
        XCTAssertEqual(drawable2?.textureSize.height, 200, "New drawable should have new size")
    }

    func testPresentingWrongSizeDrawableIsIgnored() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable = layer.nextDrawable()
        XCTAssertNotNil(drawable)

        // Change size
        layer.drawableSize = CGSize(width: 200, height: 200)

        // Try to present old drawable with wrong size
        var completionCalled = false
        layer.present(drawable!) {
            completionCalled = true
        }

        // Should be ignored (completion might or might not be called depending on implementation)
        // Just verify it doesn't crash
        XCTAssert(true, "Should not crash when presenting wrong-sized drawable")
        XCTAssertFalse(completionCalled, "Should not call completion when present wrong-sized drawable")
    }

    // MARK: - GPU Completion Tracking Tests

    func testPrepareDrawableForPresent() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable = layer.nextDrawable()
        XCTAssertNotNil(drawable)

        // Create a command buffer
        let commandQueue = layer.device!.makeCommandQueue()
        let commandBuffer = commandQueue?.makeCommandBuffer()
        XCTAssertNotNil(commandBuffer)

        // Initial state
        XCTAssertFalse(
            drawable!.isWaitingForCommandBufferCompletion,
            "Should not be waiting initially"
        )

        // Prepare for present
        layer.prepareDrawable(forPresent: drawable!, commandBuffer: commandBuffer!)

        XCTAssertTrue(
            drawable!.isWaitingForCommandBufferCompletion,
            "Should be waiting after prepare"
        )

        // Commit and wait for completion
        commandBuffer!.commit()
        commandBuffer!.waitUntilCompleted()

        XCTAssertFalse(drawable!.isWaitingForCommandBufferCompletion,"Should not be waiting after completion")
    }

    func testCannotGetDrawableWhenLastPresentedIsWaitingForGPU() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable1 = layer.nextDrawable()
        XCTAssertNotNil(drawable1)

        // Create a command buffer but don't complete it yet
        let commandQueue = layer.device!.makeCommandQueue()
        let commandBuffer = commandQueue?.makeCommandBuffer()
        XCTAssertNotNil(commandBuffer)

        // Prepare and present (but don't complete GPU work)
        layer.prepareDrawable(forPresent: drawable1!, commandBuffer: commandBuffer!)

        let expectation1 = self.expectation(description: "Present completion")
        layer.present(drawable1!) {
            expectation1.fulfill()
        }
        wait(for: [expectation1], timeout: 1.0)

        // Commit buffer but don't wait for it
        commandBuffer!.commit()

        // Try to get more drawables - should still work for drawable 2 and 3
        let drawable2 = layer.nextDrawable()
        let drawable3 = layer.nextDrawable()
        XCTAssertNotNil(drawable2)
        XCTAssertNotNil(drawable3)

        // But the 4th drawable should fail because drawable1 is still waiting
        let start = Date()
        let drawable4 = layer.nextDrawable()
        let elapsed = Date().timeIntervalSince(start)

        XCTAssertGreaterThanOrEqual(elapsed, 0.5, "Should have waited before timeout")
        XCTAssertNil(drawable4)
    }

    // MARK: - Thread Safety Tests

    func testConcurrentDrawableAcquisition() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let expectation = self.expectation(description: "Concurrent acquisition")
        expectation.expectedFulfillmentCount = 3

        var drawables: [CMPDrawable?] = []
        let lockQueue = DispatchQueue(label: "test.lock")

        // Try to acquire drawables from multiple threads
        for _ in 0..<3 {
            DispatchQueue.global().async {
                let drawable = self.layer.nextDrawable()
                lockQueue.sync {
                    drawables.append(drawable)
                }
                expectation.fulfill()
            }
        }

        wait(for: [expectation], timeout: 5.0)

        XCTAssertEqual(drawables.count, 3, "Should have 3 acquisition attempts")

        let nonNilDrawables = drawables.compactMap { $0 }
        XCTAssertEqual(nonNilDrawables.count, 3, "All acquisitions should succeed")
    }

    func testConcurrentPresentAndAcquire() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable1 = layer.nextDrawable()
        XCTAssertNotNil(drawable1)

        let expectation1 = self.expectation(description: "Present")
        let expectation2 = self.expectation(description: "Acquire")

        // Present from background thread
        DispatchQueue.global().async {
            self.layer.present(drawable1!) {
                expectation1.fulfill()
            }
        }

        // Try to acquire from another background thread
        DispatchQueue.global().async {
            let drawable2 = self.layer.nextDrawable()
            XCTAssertNotNil(drawable2)
            expectation2.fulfill()
        }

        wait(for: [expectation1, expectation2], timeout: 5.0)
    }

    func testPresentDrawableCallsCompletion() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable = layer.nextDrawable()
        XCTAssertNotNil(drawable)

        let expectation = self.expectation(description: "Completion called")
        var completionCalled = false

        layer.present(drawable!) {
            completionCalled = true
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 2.0)
        XCTAssertTrue(completionCalled, "Completion should be called")
    }

    func testMultipleSizeChanges() {
        let sizes = [
            CGSize(width: 100, height: 120),
            CGSize(width: 120, height: 200),
            CGSize(width: 50, height: 75),
            CGSize(width: 2000, height: 3000)
        ]

        for size in sizes {
            layer.drawableSize = size
            let drawable = layer.nextDrawable()
            XCTAssertNotNil(drawable, "Should get drawable for size \(size)")
            XCTAssertEqual(drawable?.textureSize.width, size.width)
            XCTAssertEqual(drawable?.textureSize.height, size.height)
        }
    }

    func testDrawablesGenerationChangeWhenSizeChanges() {
        var previousGeneration = layer.drawablesGeneration
        
        layer.drawableSize = CGSize(width: 100, height: 120)
        XCTAssertNotEqual(layer.drawablesGeneration, previousGeneration)
        
        previousGeneration = layer.drawablesGeneration
        
        layer.drawableSize = CGSize(width: 120, height: 200)
        XCTAssertNotEqual(layer.drawablesGeneration, previousGeneration)
        
        previousGeneration = layer.drawablesGeneration
        
        layer.drawableSize = CGSize(width: 2000, height: 3000)
        XCTAssertNotEqual(layer.drawablesGeneration, previousGeneration)
    }

    func testReturnDrawableMultipleTimes() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable = layer.nextDrawable()
        XCTAssertNotNil(drawable)

        // Return the same drawable multiple times - should not crash
        layer.release(drawable!)
        layer.release(drawable!)
        layer.release(drawable!)

        XCTAssert(true, "Should not crash when returning drawable multiple times")
    }

    // MARK: - Frame Ordering Tests

    func testOlderFrameIsDroppedWhenNewerFrameAlreadyPresented() {
        layer.drawableSize = CGSize(width: 100, height: 100)

        let drawable1 = layer.nextDrawable()!
        let drawable2 = layer.nextDrawable()!

        let backgroundPresented = DispatchSemaphore(value: 0)
        var drawable1CompletionCalled = false
        var drawable2CompletionCalled = false

        // Schedule frame from background queue. presentDrawable sets drawable1.presentedTime = t1
        // and then dispatches the actual on-screen presentation to the main thread asynchronously.
        DispatchQueue.global().async {
            self.layer.present(drawable1) {
                drawable1CompletionCalled = true
            }
            // Signal only after presentDrawable has set presentedTime and queued the main-thread block.
            backgroundPresented.signal()
        }

        // Block main thread until t1 is fixed and drawable1's presentation block is in the main queue.
        backgroundPresented.wait()

        // Present drawable2 synchronously from main thread: sets presentedTime = t2 > t1
        // and calls presentOnMainThread immediately (before drawable1's queued block can run).
        layer.present(drawable2) {
            drawable2CompletionCalled = true
        }

        XCTAssertTrue(drawable2CompletionCalled, "Newer frame should be presented immediately")
        XCTAssertFalse(drawable1CompletionCalled, "Older frame should not have been presented yet")

        // Drain the main queue so drawable1's pending block executes.
        let expectation = self.expectation(description: "Main run loop processes queued block")
        DispatchQueue.main.async {
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)

        // drawable1's block should have been dropped: its presentedTime (t1) is older than
        // the already-presented drawable2's time (t2), so completion must not be called.
        XCTAssertFalse(drawable1CompletionCalled, "Frame scheduled before a newer presented frame must be dropped")
    }

    // MARK: - Stress Test

    func testConcurrentAcquireReturnAcquirePattern() {
        layer.drawableSize = CGSize(width: 100, height: 100)
        var expectations: [XCTestExpectation] = []

        for i in 0..<100 {
            let queue = i % 3 == 0 ? DispatchQueue.main : DispatchQueue.global()
            let expectation = self.expectation(description: "Present \(i)")
            expectations.append(expectation)
            queue.async {
                let drawable = self.layer.nextDrawable()
                if let drawable {
                    if i % 2 == 0 {
                        self.layer.release(drawable)
                    } else {
                        self.layer.present(drawable) {}
                    }
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                expectation.fulfill()
            }
            // The test blocks main thread.
            // Perform scheduled tasks to let the layer free used buffers.
            RunLoop.main.run(until: Date())
        }
        
        // Should not crash
        self.wait(for: expectations, timeout: 10.0)
    }
}
