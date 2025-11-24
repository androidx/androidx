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

#import "CMPView.h"
#import "CMPComposeContainerLifecycleState.h"

#pragma mark - CMPViewController

@implementation CMPView {
    CMPComposeContainerLifecycleState _lifecycleState;
    id<CMPComposeContainerLifecycleDelegate> _lifecycleDelegate;
    BOOL _isViewInWindowHierarchy;
}

- (id)initWithLifecycleDelegate:(id<CMPComposeContainerLifecycleDelegate>)delegate {
    self = [super initWithFrame:CGRectZero];
    
    if (self) {
        _lifecycleDelegate = delegate;
        _lifecycleState = CMPComposeContainerLifecycleStateInitialized;
        _isViewInWindowHierarchy = NO;
        
        __weak typeof(self) weakSelf = self;
        if (@available(iOS 17, *)) {
            [self registerForTraitChanges:@[[UITraitUserInterfaceStyle class]]
                              withHandler:^(__kindof id<UITraitEnvironment>  _Nonnull traitEnvironment,
                                            UITraitCollection * _Nonnull previousCollection) {
                [weakSelf userInterfaceStyleDidChange];
            }];
        }
    }
    
    return self;
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    
    [self updateViewAppearanceState];
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    
    [self updateViewAppearanceState];
}

- (void)updateViewAppearanceState {
    BOOL isViewInWindowHierarchy = self.superview != nil && self.window != nil;
    if (_isViewInWindowHierarchy != isViewInWindowHierarchy) {
        _isViewInWindowHierarchy = isViewInWindowHierarchy;
        if (isViewInWindowHierarchy) {
            [_lifecycleDelegate composeContainerWillAppear];
            [self transitViewLifecycleToStarted];
            [self viewDidAppear];
        } else {
            [self viewDidDisappear];
            [self scheduleViewHierarchyContainmentCheck];
            [_lifecycleDelegate composeContainerDidDisappear];
        }
    }
}

- (void)transitViewLifecycleToStarted {
    switch (_lifecycleState) {
        case CMPComposeContainerLifecycleStateInitialized:
        case CMPComposeContainerLifecycleStateStopped:
            _lifecycleState = CMPComposeContainerLifecycleStateStarted;
            [self viewDidEnterWindowHierarchy];
            break;
        case CMPComposeContainerLifecycleStateStarted:
            break;
    }
}

- (void)scheduleViewHierarchyContainmentCheck {
    double delayInSeconds = 0.5;
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        switch (self->_lifecycleState) {
            case CMPComposeContainerLifecycleStateInitialized:
                NSAssert(false, @"Attempt to schedule hierarchy check without starting the container");
                break;
            case CMPComposeContainerLifecycleStateStopped:
                break;
            case CMPComposeContainerLifecycleStateStarted:
                // perform check
                if (!self->_isViewInWindowHierarchy) {
                    self->_lifecycleState = CMPComposeContainerLifecycleStateStopped;
                    [self viewDidLeaveWindowHierarchy];
                }
                break;
        }
    });
}

- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];

    if (@available(iOS 17, *)) {
        // Do nothing
    } else if (self.traitCollection.userInterfaceStyle != previousTraitCollection.userInterfaceStyle) {
        [self userInterfaceStyleDidChange];
    }
}

- (void)viewDidAppear {
}

- (void)viewDidDisappear {
}

- (void)viewDidEnterWindowHierarchy {
}

- (void)viewDidLeaveWindowHierarchy {
}

- (void)userInterfaceStyleDidChange {
}

- (void)dealloc {
    if (_lifecycleState == CMPComposeContainerLifecycleStateStarted) {
        [self viewDidLeaveWindowHierarchy];
    }
    
    [_lifecycleDelegate composeContainerWillDealloc];
}

@end
