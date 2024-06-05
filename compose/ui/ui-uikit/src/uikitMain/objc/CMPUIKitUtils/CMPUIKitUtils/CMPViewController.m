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

#import "CMPViewController.h"
#import <objc/runtime.h>

#pragma mark - UIViewController + CMPUIKitUtilsPrivate

@interface UIViewController(CMPUIKitUtilsPrivate)

@end

@implementation UIViewController(CMPUIKitUtilsPrivate)

- (BOOL)cmp_isRootViewController {
    // Check that it's not rootViewController of one of windows of one of the connected scenes.
    // In most apps it will be a single scene with a single connected window.
    if (@available(iOS 13.0, *)) {
        for (UIScene *scene in [UIApplication.sharedApplication connectedScenes]) {
            if ([scene isKindOfClass:[UIWindowScene class]]) {
                UIWindowScene *windowScene = (UIWindowScene *)scene;
                
                for (UIWindow *window in windowScene.windows) {
                    if (window.rootViewController == self) {
                        return YES;
                    }
                }
            }
        }
    } else {
        for (UIWindow* window in UIApplication.sharedApplication.windows) {
            if (window.rootViewController == self) {
                return YES;
            }
        }
    }
    
    return NO;
}

- (BOOL)cmp_isInWindowHierarchy {
    if (self.view.window != nil) {
        return YES;
    } else if (self.parentViewController != nil) {
        return [self.parentViewController cmp_isInWindowHierarchy];
    } else if (self.presentingViewController != nil) {
        return [self.presentingViewController cmp_isInWindowHierarchy];
    } else {
        return [self cmp_isRootViewController];
    }
}

@end

#pragma mark - CMPViewControllerLifecycleState

typedef NS_ENUM(NSInteger, CMPViewControllerLifecycleState) {
    CMPViewControllerLifecycleStateInitalized,
    CMPViewControllerLifecycleStateStarted,
    CMPViewControllerLifecycleStateDestroyed
};

#pragma mark - CMPViewController

@implementation CMPViewController {
    CMPViewControllerLifecycleState _lifecycleState;
}

- (instancetype)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    
    if (self) {
        _lifecycleState = CMPViewControllerLifecycleStateInitalized;
    }
    
    return self;
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
        
    [self viewControllerDidEnterWindowHierarchy];
}

- (void)viewSafeAreaInsetsDidChange {
    [super viewSafeAreaInsetsDidChange];
}

- (void)transitLifecycleToStarted {
    switch (_lifecycleState) {
        case CMPViewControllerLifecycleStateDestroyed:
            @throw [NSException exceptionWithName:@"CMPViewControllerMisuse"
                                           reason:@"CMPViewController shouldn't be reused after completely removed from hierarchy, because it's logically marked as Destroyed. You must create a new CMPViewController and use it instead."
                                         userInfo:nil];
        case CMPViewControllerLifecycleStateInitalized:
            _lifecycleState = CMPViewControllerLifecycleStateStarted;
            [self viewControllerDidEnterWindowHierarchy];
            [self scheduleHierarchyContainmentCheck];
            break;
        case CMPViewControllerLifecycleStateStarted:
            break;
    }
}

- (void)scheduleHierarchyContainmentCheck {
    double delayInSeconds = 0.5;
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        switch (self->_lifecycleState) {
            case CMPViewControllerLifecycleStateInitalized:
            case CMPViewControllerLifecycleStateDestroyed:
                assert(false);
                break;
            case CMPViewControllerLifecycleStateStarted:
                // perform check
                if ([self cmp_isInWindowHierarchy]) {
                    // everything is fine, schedule next one
                    [self scheduleHierarchyContainmentCheck];
                } else {
                    self->_lifecycleState = CMPViewControllerLifecycleStateDestroyed;
                    [self viewControllerDidLeaveWindowHierarchy];
                    
                }
                break;
        }
    });
}

- (void)viewControllerDidEnterWindowHierarchy {
    [self transitLifecycleToStarted];
}

- (void)viewControllerDidLeaveWindowHierarchy {
}

@end
