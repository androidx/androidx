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
    }

    override func tearDownWithError() throws {
        super.tearDown()

        appDelegate?.cleanUp()
        appDelegate = nil
    }

    public func testControllerPresent() {
        let viewController = TestViewController()
        XCTAssertFalse(viewController.viewIsInWindowHierarchy)

        appDelegate.window?.rootViewController?.present(viewController, animated: true)
        wait { viewController.viewIsInWindowHierarchy == true }

        appDelegate.window?.rootViewController?.dismiss(animated: true)
        wait { viewController.viewIsInWindowHierarchy == false }
    }

    public func testChildController() {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()

        appDelegate.window?.rootViewController?.present(viewController1, animated: true)
        wait { viewController1.viewIsInWindowHierarchy == true }
        wait { viewController2.viewIsInWindowHierarchy == false }

        viewController1.addChild(viewController2)
        viewController2.didMove(toParent: viewController1)
        viewController1.view.addSubview(viewController2.view)
        wait { viewController1.viewIsInWindowHierarchy == true }
        wait { viewController2.viewIsInWindowHierarchy == true }

        viewController2.removeFromParent()
        viewController2.view.removeFromSuperview()
        wait { viewController1.viewIsInWindowHierarchy == true }
        wait { viewController2.viewIsInWindowHierarchy == false }

        viewController1.addChild(viewController2)
        viewController2.didMove(toParent: viewController1)
        viewController1.view.addSubview(viewController2.view)
        wait { viewController1.viewIsInWindowHierarchy == true }
        wait { viewController2.viewIsInWindowHierarchy == true }

        appDelegate.window?.rootViewController?.dismiss(animated: true)
        wait { viewController1.viewIsInWindowHierarchy == false }
        wait { viewController2.viewIsInWindowHierarchy == false }
    }

    public func testNavigationControllerPresentAndPush() {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        let viewController3 = TestViewController()

        // Use autoreleasepool to be sure, navigationController will be properly deleted after dismissal
        autoreleasepool {
            let navigationController = UINavigationController(rootViewController: viewController1)

            appDelegate.window?.rootViewController?.present(navigationController, animated: true)

            wait { viewController1.viewIsInWindowHierarchy == true }
            wait { viewController2.viewIsInWindowHierarchy == false }
            wait { viewController3.viewIsInWindowHierarchy == false }

            navigationController.pushViewController(viewController2, animated: true)
            wait { viewController1.viewIsInWindowHierarchy == true }
            wait { viewController2.viewIsInWindowHierarchy == true }
            wait { viewController3.viewIsInWindowHierarchy == false }

            navigationController.present(viewController3, animated: true)
            wait { viewController1.viewIsInWindowHierarchy == true }
            wait { viewController2.viewIsInWindowHierarchy == true }
            wait { viewController3.viewIsInWindowHierarchy == true }

            viewController3.dismiss(animated: true)
            wait { viewController1.viewIsInWindowHierarchy == true }
            wait { viewController2.viewIsInWindowHierarchy == true }
            wait { viewController3.viewIsInWindowHierarchy == false }

            navigationController.dismiss(animated: true)
        }
        wait { viewController1.viewIsInWindowHierarchy == false }
        wait { viewController2.viewIsInWindowHierarchy == false }
        wait { viewController3.viewIsInWindowHierarchy == false }
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

            wait { viewController1.viewIsInWindowHierarchy == true }
            wait { viewController2.viewIsInWindowHierarchy == true }
            wait { viewController3.viewIsInWindowHierarchy == false }

            tabBarController.present(viewController3, animated: true)
            wait { viewController1.viewIsInWindowHierarchy == true }
            wait { viewController2.viewIsInWindowHierarchy == true }
            wait { viewController3.viewIsInWindowHierarchy == true }

            viewController3.dismiss(animated: true)
            wait { viewController1.viewIsInWindowHierarchy == true }
            wait { viewController2.viewIsInWindowHierarchy == true }
            wait { viewController3.viewIsInWindowHierarchy == false }

            tabBarController.dismiss(animated: true)
        }

        wait { viewController1.viewIsInWindowHierarchy == false }
        wait { viewController2.viewIsInWindowHierarchy == false }
        wait { viewController3.viewIsInWindowHierarchy == false }
    }
}

private class TestViewController: CMPViewController {
    public var viewIsInWindowHierarchy: Bool = false

    override func viewControllerDidEnterWindowHierarchy() {
        viewIsInWindowHierarchy = true
    }

    override func viewControllerDidLeaveWindowHierarchy() {
        viewIsInWindowHierarchy = false
    }
}
