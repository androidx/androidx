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

#import <UIKit/UIKit.h>
#import "CMPComposeContainerLifecycleDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPView : UIView

- (id)initWithLifecycleDelegate:(id<CMPComposeContainerLifecycleDelegate> _Nullable)delegate;

/// Notifies the view is added to a view hierarchy
- (void)viewDidAppear;

/// Notifies the view was removed from a view hierarchy
- (void)viewDidDisappear;

/// Indicates that view is considered alive in terms of structural containment
- (void)viewDidEnterWindowHierarchy;

/// Indicates that view is considered as closed in terms of structural containment
- (void)viewDidLeaveWindowHierarchy;

/// Indicates that trait interface style trait changed
- (void)userInterfaceStyleDidChange;

@end

NS_ASSUME_NONNULL_END
