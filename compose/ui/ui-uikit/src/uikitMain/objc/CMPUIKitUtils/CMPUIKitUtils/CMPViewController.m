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

#import "CMPViewController.h"
#import "CMPView.h"
#import <objc/runtime.h>

#pragma mark - CMPDeinitNotifier

@interface CMPDeinitNotifier: NSObject

@property (nonatomic, copy) void (^deinitHandlerBlock)(void);

+ (instancetype)notifierWithDeinitHandler:(void (^)(void))deinitHandlerBlock;

@end

@implementation CMPDeinitNotifier

+ (instancetype)notifierWithDeinitHandler:(void (^)(void))deinitHandlerBlock {
    CMPDeinitNotifier *notifier = [CMPDeinitNotifier new];
    notifier.deinitHandlerBlock = deinitHandlerBlock;
    return notifier;
}

- (void)dealloc {
    if (self.deinitHandlerBlock) {
        self.deinitHandlerBlock();
    }
}

@end

#pragma mark - UIViewController + CMPUIKitUtilsPrivate

@interface UIViewController(CMPUIKitUtilsPrivate)

@end

@implementation UIViewController(CMPUIKitUtilsPrivate)

static const void *const kCMPUIKitUtilsDeinitMonitorKey = @"cmp_uikit_utils_deinit_monitor_key";

- (NSMutableArray *)cmp_uikit_utils_deinitMonitor {
    NSMutableArray *observers = objc_getAssociatedObject(self, kCMPUIKitUtilsDeinitMonitorKey);
    if (observers) {
        return observers;
    }
    observers = [NSMutableArray new];
    objc_setAssociatedObject(self, kCMPUIKitUtilsDeinitMonitorKey, observers, OBJC_ASSOCIATION_RETAIN);
    return observers;
}

@end


#pragma mark - CMPViewController

@interface CMPViewController()

@property (nonatomic, assign) BOOL cmp_needsVerifyHierarchy;
@property (nonatomic, assign) BOOL cmp_viewControllerInWindowHierarchy;
@property (nonatomic, copy) void (^cmp_terminateComposeSceneTask)(void);
@property (nonatomic, weak) CMPDeinitNotifier *cmp_parentDeinitNotifier;

@end

@implementation CMPViewController

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];

    self.cmp_viewControllerInWindowHierarchy = true;
}

- (void)viewControllerDidLeaveWindowHierarchy {}

- (void)viewControllerDidEnterWindowHierarchy {}

- (void)cmp_deinitScene {
    self.cmp_terminateComposeSceneTask = false;
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];

    [self cmp_setNeedsVerifyHierarchy];
}

- (void)didMoveToParentViewController:(UIViewController *)parent {
    self.cmp_parentDeinitNotifier.deinitHandlerBlock = nil;

    if (parent) {
        __weak typeof(self) weakSelf = self;
        CMPDeinitNotifier *notifier = [CMPDeinitNotifier notifierWithDeinitHandler:^{
            [weakSelf cmp_setNeedsVerifyHierarchy];
        }];
        [parent.cmp_uikit_utils_deinitMonitor addObject:notifier];

        self.cmp_parentDeinitNotifier = notifier;
    } else if (self.parentViewController != nil && self.cmp_parentDeinitNotifier != nil) {
        [self.parentViewController.cmp_uikit_utils_deinitMonitor filterUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(CMPDeinitNotifier *notifier, NSDictionary<NSString *,id> * _Nullable bindings) {
            return notifier != self.cmp_parentDeinitNotifier;
        }]];
    }

    [super didMoveToParentViewController:parent];

    if (parent != nil && !self.cmp_viewControllerInWindowHierarchy) {
        [self cmp_verifyHierarchyAndScheduleNotificationIfNeeded];
    }

    [self cmp_setNeedsVerifyHierarchy];
}

- (void)removeFromParentViewController {
    [super removeFromParentViewController];

    [self cmp_setNeedsVerifyHierarchy];
}

- (void)viewDidLoad {
    if ([self.view isKindOfClass:[CMPView class]]) {
        __weak typeof(self) weakSelf = self;
        ((CMPView *)self.view).windowDidChangeBlock = ^{
            [weakSelf cmp_setNeedsVerifyHierarchy];
        };
    } else {
        NSAssert(false, @"The view of CMPViewController should be either CMPView or subclassed from CMPView");
    }

    [super viewDidLoad];
}

- (void)loadView {
    self.view = [CMPView new];
}

- (void)cmp_setNeedsVerifyHierarchy {
    if (self.cmp_needsVerifyHierarchy) {
        return;
    }
    self.cmp_needsVerifyHierarchy = true;

    __weak typeof(self) weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        weakSelf.cmp_needsVerifyHierarchy = false;
        [weakSelf cmp_verifyHierarchyAndScheduleNotificationIfNeeded];
    });
}

- (void)cmp_verifyHierarchyAndScheduleNotificationIfNeeded {
    if ([self cmp_isInWindowHierarchy]) {
        self.cmp_viewControllerInWindowHierarchy = true;
        [self cmp_cancelFinalizeAndNotifyViewControllerIsInHierarchy];
    } else {
        [self cmp_setNeedsFinalizeAndNotifyViewControllerIsInHierarchy];
    }
}

- (void)cmp_setNeedsFinalizeAndNotifyViewControllerIsInHierarchy {
    if (self.cmp_terminateComposeSceneTask == nil && self.cmp_viewControllerInWindowHierarchy) {
        __weak typeof(self) weakSelf = self;
        self.cmp_terminateComposeSceneTask = ^{
            [weakSelf cmp_finalizeAndNotifyViewControllerIsInHierarchy];
            weakSelf.cmp_terminateComposeSceneTask = nil;
        };
        dispatch_async(dispatch_get_main_queue(), self.cmp_terminateComposeSceneTask);
    }
}

- (void)cmp_finalizeAndNotifyViewControllerIsInHierarchy {
    if ([self cmp_isInWindowHierarchy]) {
        self.cmp_viewControllerInWindowHierarchy = true;
    } else {
        self.cmp_viewControllerInWindowHierarchy = false;
    }
}

- (void)cmp_cancelFinalizeAndNotifyViewControllerIsInHierarchy {
    if (self.cmp_terminateComposeSceneTask) {
        dispatch_block_cancel(self.cmp_terminateComposeSceneTask);
    }
    self.cmp_terminateComposeSceneTask = nil;
}

- (BOOL)cmp_isInWindowHierarchy {
    return [self cmp_isViewControllerInWindowHierarchy:self];
}

- (BOOL)cmp_isViewControllerInWindowHierarchy:(UIViewController *)viewController {
    if (viewController.view.window != nil) {
        return YES;
    } else if (viewController.parentViewController != nil) {
        return [self cmp_isViewControllerInWindowHierarchy:viewController.parentViewController];
    } else {
        return NO;
    }
}

- (void)setCmp_viewControllerInWindowHierarchy:(BOOL)cmp_viewControllerInWindowHierarchy {
    if (_cmp_viewControllerInWindowHierarchy == cmp_viewControllerInWindowHierarchy) {
        return;
    }
    _cmp_viewControllerInWindowHierarchy = cmp_viewControllerInWindowHierarchy;

    if (cmp_viewControllerInWindowHierarchy) {
        [self viewControllerDidEnterWindowHierarchy];
    } else {
        [self viewControllerDidLeaveWindowHierarchy];
    }
}

@end
