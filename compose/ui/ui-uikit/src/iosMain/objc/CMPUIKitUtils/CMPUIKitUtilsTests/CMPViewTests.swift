/*
 * Copyright 2025 The Android Open Source Project
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

import AVKit
import XCTest

final class CMPViewTests: XCTestCase {
    var appDelegate: MockAppDelegate!
    var rootView: UIView? {
        get {
            appDelegate.window!.rootViewController?.view.subviews.first
        }
        set {
            appDelegate.window!.rootViewController?.view.subviews.forEach {
                $0.removeFromSuperview()
            }
            if let newValue {
                appDelegate.window!.rootViewController?.view.addSubview(newValue)
                newValue.frame = appDelegate.window!.rootViewController?.view.bounds ?? .zero
            }
        }
    }

    override func setUpWithError() throws {
        super.setUp()

        appDelegate = MockAppDelegate()
        UIApplication.shared.delegate = appDelegate
        appDelegate.setUpClearWindow()
        TestView.counter = 1
    }

    override func tearDownWithError() throws {
        super.tearDown()

        appDelegate?.cleanUp()
        appDelegate = nil
    }
        
    @MainActor
    private func expect(
        view: TestView,
        toBeInHierarchy inHierarchy: Bool,
        line: Int = #line
    ) async {
        await expect(timeout: 5.0, line: line) {
            view.viewIsInWindowHierarchy == inHierarchy
        }
    }

    @MainActor
    private func expect(
        view: TestView,
        toBeAppeared isAppeared: Bool,
        line: Int = #line
    ) async {
        await expect(timeout: 5.0, line: line) {
            view.isViewAppeared == isAppeared
        }
    }
    
    @MainActor
    public func testNotAttached() async {
        let view = TestView()
        await expect(view: view, toBeInHierarchy: false)
    }
    
    @MainActor
    public func testViewAttach() async {
        let view = TestView()
        rootView = view
        await expect(view: view, toBeInHierarchy: true)
        
        rootView = nil
        await expect(view: view, toBeInHierarchy: false)
    }

    @MainActor
    public func testViewThroughSuperviewAttach() async {
        let view = TestView()
        view.frame = .init(x: 0, y: 0, width: 100, height: 100)
        let superview = UIView()
        superview.addSubview(view)
        
        await expect(view: view, toBeInHierarchy: false)

        rootView = superview
        await expect(view: view, toBeInHierarchy: true)
        
        rootView = nil
        await expect(view: view, toBeInHierarchy: false)
    }

    @MainActor
    public func testLifecycleDelegate() async {
        let delegate = LifecycleDelegate()

        autoreleasepool {
            let view = TestView(delegate: delegate)
            rootView = view
        }
        
        await expect { delegate.containerWillAppearCallsCount == 1 }
        
        rootView = nil

        await expect { delegate.containerWillAppearCallsCount == 1 }
        await expect { delegate.containerDidDisappearCallsCount == 1 }
        await expect { delegate.containerWillDeallocCallsCount == 1 }
    }
    
    @MainActor
    public func testViewAppeared() async {
        let view = TestView()
        rootView = view
        await expect(view: view, toBeAppeared: true)
        
        rootView = nil
        await expect(view: view, toBeAppeared: false)
    }
}

private class TestView: CMPView {
    public static var counter: Int = 1
    
    private let id: Int
    
    public var viewIsInWindowHierarchy: Bool = false
    public var isViewAppeared: Bool = false

    init(delegate: CMPComposeContainerLifecycleDelegate? = nil) {
        id = TestView.counter
        TestView.counter += 1
        super.init(lifecycleDelegate: delegate)
    }
    
    required init?(coder: NSCoder) {
        nil
    }
    
    override func viewDidAppear() {
        isViewAppeared = true
    }
    
    override func viewDidDisappear() {
        isViewAppeared = false
    }
    
    override func viewDidEnterWindowHierarchy() {
        print("TestView_\(id) didEnterWindowHierarchy")
        XCTAssertFalse(viewIsInWindowHierarchy)
        viewIsInWindowHierarchy = true
    }

    override func viewDidLeaveWindowHierarchy() {
        super.viewDidLeaveWindowHierarchy()
        print("TestView_\(id) didLeaveWindowHierarchy")
        XCTAssertTrue(viewIsInWindowHierarchy)
        viewIsInWindowHierarchy = false
    }
}
