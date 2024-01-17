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

final class CMPViewControllerTests: XCTestCase {
    var appDelegate: MockAppDelegate!

    override func setUpWithError() throws {
        super.setUp()

        appDelegate = MockAppDelegate()
        UIApplication.shared.delegate = appDelegate
        appDelegate.setUpClearWindow()
        TestViewController.counter = 1
    }

    override func tearDownWithError() throws {
        super.tearDown()

        appDelegate?.cleanUp()
        appDelegate = nil
    }
    
    private func expect(viewController: TestViewController, toBeInHierarchy inHierarchy: Bool) {
        wait(for: {
            viewController.viewIsInWindowHierarchy == inHierarchy
        }, timeout: 5.0)
    }
    
    private func expect(viewControllers: [TestViewController], toBeInHierarchy inHierarchy: Bool) {
        for viewController in viewControllers {
            wait(for: {
                viewController.viewIsInWindowHierarchy == inHierarchy
            }, timeout: 5.0)
        }
    }

    public func testControllerPresent() {
        let viewController = TestViewController()
        XCTAssertFalse(viewController.viewIsInWindowHierarchy)

        appDelegate.window?.rootViewController?.present(viewController, animated: true)
        expect(viewController: viewController, toBeInHierarchy: true)

        appDelegate.window?.rootViewController?.dismiss(animated: true)
        expect(viewController: viewController, toBeInHierarchy: false)
    }

    public func testChildController() {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()

        appDelegate.window?.rootViewController?.present(viewController1, animated: true)
        expect(viewController: viewController1, toBeInHierarchy: true)
        expect(viewController: viewController2, toBeInHierarchy: false)

        viewController1.addChild(viewController2)
        viewController2.didMove(toParent: viewController1)
        viewController1.view.addSubview(viewController2.view)
        expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)

        viewController2.removeFromParent()
        viewController2.view.removeFromSuperview()
        expect(viewController: viewController1, toBeInHierarchy: true)
        expect(viewController: viewController2, toBeInHierarchy: false)

        appDelegate.window?.rootViewController?.dismiss(animated: true)
        expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: false)
    }

    public func testNavigationControllerPresentAndPush() {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        let viewController3 = TestViewController()
        
        expect(viewControllers: [
            viewController1,
            viewController2,
            viewController3
        ], toBeInHierarchy: false)

        // Use autoreleasepool to be sure, navigationController will be properly deleted after dismissal
        autoreleasepool {
            let navigationController = UINavigationController(rootViewController: viewController1)

            appDelegate.window?.rootViewController?.present(navigationController, animated: false)

            expect(viewController: viewController1, toBeInHierarchy: true)
            expect(viewControllers: [viewController2, viewController3], toBeInHierarchy: false)

            navigationController.pushViewController(viewController2, animated: false)
            expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)
            expect(viewController: viewController3, toBeInHierarchy: false)

            navigationController.present(viewController3, animated: false)
            expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: true)

            viewController3.dismiss(animated: false)
            expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)
            expect(viewController: viewController3, toBeInHierarchy: false)

            navigationController.dismiss(animated: false)
        }
        
        expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: false)
    }
    
    public func testNavigationControllerPresentAndPush2() {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        let viewController3 = TestViewController()

        // Use autoreleasepool to be sure, navigationController will be properly deleted after dismissal
        autoreleasepool {
            let rootViewController = appDelegate!.window!.rootViewController!
            
            let navigationController = UINavigationController(rootViewController: viewController1)

            rootViewController.present(navigationController, animated: false)
            navigationController.pushViewController(viewController2, animated: false)
            navigationController.pushViewController(viewController3, animated: false)

            navigationController.dismiss(animated: false)
        }
        
        expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: false)
    }

    public func testTabBarControllerPresentAndPush() {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        let viewController3 = TestViewController()

        // Use autoreleasepool to be sure, tabBarController will be properly deleted after dismissal
        autoreleasepool {
            let tabBarController = UITabBarController()
            tabBarController.viewControllers = [viewController1, viewController2]

            appDelegate.window?.rootViewController?.present(tabBarController, animated: true)

            expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)
            expect(viewController: viewController3, toBeInHierarchy: false)

            tabBarController.present(viewController3, animated: true)
            expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: true)

            viewController3.dismiss(animated: true)
            expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)
            expect(viewController: viewController3, toBeInHierarchy: false)

            tabBarController.dismiss(animated: true)
        }

        expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: false)
    }
}

private class TestViewController: CMPViewController {
    public static var counter: Int = 1
    
    private let id: Int
    
    public var viewIsInWindowHierarchy: Bool = false
    
    init() {
        id = TestViewController.counter
        TestViewController.counter += 1
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        nil
    }
    
    override func viewControllerDidEnterWindowHierarchy() {
        print("\(id) entered")
        viewIsInWindowHierarchy = true
    }

    override func viewControllerDidLeaveWindowHierarchy() {
        print("\(id) left")
        viewIsInWindowHierarchy = false
    }
}
