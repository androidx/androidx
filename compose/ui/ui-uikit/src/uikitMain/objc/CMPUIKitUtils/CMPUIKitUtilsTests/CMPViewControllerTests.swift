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

import AVKit
import XCTest

final class CMPViewControllerTests: XCTestCase {
    var appDelegate: MockAppDelegate!
    var rootViewController: UIViewController {
        get {
            appDelegate.window!.rootViewController!
        }
        set {
            appDelegate.window!.rootViewController = newValue
        }
    }

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
        
    @MainActor
    private func expect(
        viewController: TestViewController,
        toBeInHierarchy inHierarchy: Bool,
        line: Int = #line
    ) async {
        await expect(timeout: 5.0, line: line) {
            viewController.viewIsInWindowHierarchy == inHierarchy
        }
    }
    
    @MainActor
    private func expect(
        viewControllersToBeInHierarchy: [(TestViewController, Bool)],
        line: Int = #line
    ) async {
        await expect(timeout: 5.0, line: line) {
            viewControllersToBeInHierarchy.reduce(true) { partialResult, pair in
                let (viewController, inHierarchy) = pair
                
                return partialResult && viewController.viewIsInWindowHierarchy == inHierarchy
            }
        }
    }
    
    @MainActor
    private func expect(
        viewControllers: [TestViewController],
        toBeInHierarchy inHierarchy: Bool,
        line: Int = #line
    ) async  {
        await expect(viewControllersToBeInHierarchy: viewControllers.map {
            ($0, inHierarchy)
        }, line: line)
    }
    
    @MainActor
    public func testNotAttached() async {
        let viewController = TestViewController()
        await expect(viewController: viewController, toBeInHierarchy: false)
    }
    
    @MainActor
    public func testRootViewController() async {
        let viewController = TestViewController()
        rootViewController = viewController
        await expect(viewController: viewController, toBeInHierarchy: true)
        
        rootViewController = UIViewController()
        await expect(viewController: viewController, toBeInHierarchy: false)
    }

    @MainActor
    public func testPresentAndDismiss() async {
        let viewController = TestViewController()

        rootViewController.present(viewController, animated: true)
        await expect(viewController: viewController, toBeInHierarchy: true)

        rootViewController.dismiss(animated: true)
        
        await expect(viewController: viewController, toBeInHierarchy: false)
    }

    @MainActor
    public func testChildController() async {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        await expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: false)

        rootViewController.present(viewController1, animated: true)
        await expect(viewControllersToBeInHierarchy: [
            (viewController1, true),
            (viewController2, false)
        ])

        viewController1.addChild(viewController2)
        viewController2.didMove(toParent: viewController1)
        viewController1.view.addSubview(viewController2.view)
        await expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)

        viewController2.willMove(toParent: nil)
        viewController2.removeFromParent()
        viewController2.view.removeFromSuperview()
        await expect(viewControllersToBeInHierarchy: [
            (viewController1, true),
            (viewController2, false)
        ])

        rootViewController.dismiss(animated: true)
        await expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: false)
    }

    @MainActor
    public func testNavigationControllerPresentAndPush() async {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        let viewController3 = TestViewController()
        
        await expect(viewControllers: [
            viewController1,
            viewController2,
            viewController3
        ], toBeInHierarchy: false)
        
        let navigationController = UINavigationController(rootViewController: viewController1)

        rootViewController.present(navigationController, animated: false)

        await expect(viewController: viewController1, toBeInHierarchy: true)
        await expect(viewControllers: [viewController2, viewController3], toBeInHierarchy: false)

        navigationController.pushViewController(viewController2, animated: false)
        await expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)
        await expect(viewController: viewController3, toBeInHierarchy: false)

        navigationController.present(viewController3, animated: false)
        await expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: true)

        viewController3.dismiss(animated: false)
        await expect(viewControllers: [viewController1, viewController2], toBeInHierarchy: true)
        await expect(viewController: viewController3, toBeInHierarchy: false)

        navigationController.dismiss(animated: false)
        
        await expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: false)
    }
    
    @MainActor
    public func testNavigationControllerPresentAndPush2() async {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        let viewController3 = TestViewController()
        
        let navigationController = UINavigationController(rootViewController: viewController1)

        rootViewController.present(navigationController, animated: false)
        navigationController.pushViewController(viewController2, animated: false)
        navigationController.pushViewController(viewController3, animated: false)

        navigationController.dismiss(animated: false)
        
        await expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: false)
    }

    @MainActor
    public func testTabBarControllerPresentAndPush() async {
        let viewController1 = TestViewController()
        let viewController2 = TestViewController()
        let viewController3 = TestViewController()
        
        let tabBarController = UITabBarController()
        tabBarController.viewControllers = [viewController1, viewController2]

        rootViewController.present(tabBarController, animated: true)

        await expect(viewControllers: [viewController2, viewController3], toBeInHierarchy: false)
        await expect(viewController: viewController1, toBeInHierarchy: true)

        tabBarController.present(viewController3, animated: true)
        await expect(viewControllers: [viewController1, viewController3], toBeInHierarchy: true)
        
        viewController3.dismiss(animated: true)
        await expect(viewController: viewController1, toBeInHierarchy: true)
        await expect(viewControllers: [viewController2, viewController3], toBeInHierarchy: false)

        tabBarController.dismiss(animated: true)

        await expect(viewControllers: [viewController1, viewController2, viewController3], toBeInHierarchy: false)
    }
    
    @MainActor
    public func testFullscreenPresentationOnTop() async {
        let viewController = TestViewController()
        rootViewController = viewController
        
        await expect(viewController: viewController, toBeInHierarchy: true)
        
        let urlStr = "https://nonexisting"
        let url = URL(string: urlStr)!
        let player = AVPlayer(url: url)
        let playerController = AVPlayerViewController()
        playerController.player = player
        
        viewController.present(playerController, animated: false)
        await expect(viewController: viewController, toBeInHierarchy: true)
        playerController.dismiss(animated: false)
        
        rootViewController = UIViewController()
        await expect(viewController: viewController, toBeInHierarchy: false)
    }
    
    @MainActor
    public func testFullScreenPresentationSandwich() async {
        let viewController0 = TestViewController()
        
        rootViewController = viewController0
        
        let viewController1 = TestViewController()
        viewController1.modalPresentationStyle = .fullScreen
        
        let viewController2 = TestViewController()
        viewController1.addChild(viewController2)
        viewController1.view.addSubview(viewController2.view)
        viewController2.didMove(toParent: viewController1)
        
        let viewController3 = TestViewController()
        viewController3.modalPresentationStyle = .fullScreen
        
        await expect(viewControllersToBeInHierarchy: [
            (viewController0, true),
            (viewController1, false),
            (viewController2, false),
            (viewController3, false),
        ])
        
        viewController0.present(viewController1, animated: false)
        await expect(viewControllersToBeInHierarchy: [
            (viewController0, true),
            (viewController1, true),
            (viewController2, true),
            (viewController3, false),
        ])
        
        viewController1.present(viewController3, animated: false)
        await expect(viewControllers: [viewController0, viewController1, viewController2, viewController3], toBeInHierarchy: true)
                        
        viewController0.dismiss(animated: false)
        await expect(viewControllersToBeInHierarchy: [
            (viewController0, true),
            (viewController1, false),
            (viewController2, false),
            (viewController3, false),
        ])
        rootViewController = UIViewController()
        await expect(viewControllers: [viewController0, viewController1, viewController2, viewController3], toBeInHierarchy: false)
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
        super.viewControllerDidEnterWindowHierarchy()
        print("TestViewController_\(id) didEnterWindowHierarchy")
        viewIsInWindowHierarchy = true
    }

    override func viewControllerDidLeaveWindowHierarchy() {
        super.viewControllerDidLeaveWindowHierarchy()
        print("TestViewController_\(id) didLeaveWindowHierarchy")
        viewIsInWindowHierarchy = false
    }
}
