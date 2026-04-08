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

NS_ASSUME_NONNULL_BEGIN

@interface CMPUIWindowSceneUtils : NSObject

- (instancetype)init NS_UNAVAILABLE;

/// Returns the interface orientation for the given window scene.
/// Uses effectiveGeometry on iOS 16+ and falls back to the legacy interfaceOrientation
/// property on older versions. Falls back to UIApplication.statusBarOrientation when
/// windowScene is nil.
+ (UIInterfaceOrientation)interfaceOrientationForWindowScene:(UIWindowScene * _Nullable)windowScene;

+ (void)requestOrientationChangeForWindow:(UIWindow * _Nullable)window
                     interfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
                             errorHandler:(void (^ _Nullable)(NSError * _Nonnull error))errorHandler;

@end

NS_ASSUME_NONNULL_END
