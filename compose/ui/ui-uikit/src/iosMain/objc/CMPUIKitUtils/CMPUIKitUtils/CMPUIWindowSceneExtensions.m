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

#import "CMPUIWindowSceneExtensions.h"

static NSString * const CMPUIWindowSceneUtilsErrorDomain = @"CMPUIWindowSceneUtilsErrorDomain";

typedef NS_ENUM(NSInteger, CMPUIWindowSceneUtilsErrorCode) {
    CMPUIWindowSceneUtilsErrorCodeWindowNil = 1000,
    CMPUIWindowSceneUtilsErrorCodeWindowSceneNil = 1001,
    CMPUIWindowSceneUtilsErrorCodeInvalidInterfaceOrientation = 1002
};

@implementation CMPUIWindowSceneUtils

+ (UIInterfaceOrientation)interfaceOrientationForWindowScene:(UIWindowScene * _Nullable)windowScene {
    if (windowScene != nil) {
        if (@available(iOS 16.0, *)) {
            return windowScene.effectiveGeometry.interfaceOrientation;
        } else {
            return windowScene.interfaceOrientation;
        }
    }
    return UIApplication.sharedApplication.statusBarOrientation;
}

+ (void)requestOrientationChangeForWindow:(UIWindow * _Nullable)window
                     interfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
                             errorHandler:(void (^ _Nullable)(NSError * _Nonnull error))errorHandler {
    if (@available(iOS 16.0, *)) {
        
        if (window == nil) {
            if (errorHandler != nil) {
                NSError *error = [NSError errorWithDomain:CMPUIWindowSceneUtilsErrorDomain
                                                     code:CMPUIWindowSceneUtilsErrorCodeWindowNil
                                                 userInfo:@{
                    NSLocalizedDescriptionKey: @"Cannot request an orientation change because the window is nil."
                }];

                errorHandler(error);
            }

            return;
        }
        
        UIViewController *rootViewController = window.rootViewController;
        if (rootViewController != nil) {
            [rootViewController setNeedsUpdateOfSupportedInterfaceOrientations];
        }
        
        UIWindowScene *windowScene = window.windowScene;

        if (windowScene == nil) {
            if (errorHandler != nil) {
                NSError *error = [NSError errorWithDomain:CMPUIWindowSceneUtilsErrorDomain
                                                     code:CMPUIWindowSceneUtilsErrorCodeWindowSceneNil
                                                 userInfo:@{
                    NSLocalizedDescriptionKey: @"Cannot request an orientation change because the window scene is nil."
                }];

                errorHandler(error);
            }

            return;
        }
        
        UIInterfaceOrientationMask mask =
            [self maskForInterfaceOrientation:interfaceOrientation];
        UIWindowSceneGeometryPreferencesIOS *preferences =
            [[UIWindowSceneGeometryPreferencesIOS alloc] initWithInterfaceOrientations:mask];

        [windowScene requestGeometryUpdateWithPreferences:preferences
                                             errorHandler:^(NSError * _Nonnull error) {
            if (errorHandler != nil) {
                errorHandler(error);
            }
        }];
        
    } else {
        UIDevice *device = UIDevice.currentDevice;

        [device setValue:@(interfaceOrientation) forKey:@"orientation"];
        [UIViewController attemptRotationToDeviceOrientation];
    }
}

+ (UIInterfaceOrientationMask)maskForInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    switch (interfaceOrientation) {
        case UIInterfaceOrientationPortrait:
            return UIInterfaceOrientationMaskPortrait;

        case UIInterfaceOrientationPortraitUpsideDown:
            return UIInterfaceOrientationMaskPortraitUpsideDown;

        case UIInterfaceOrientationLandscapeLeft:
            return UIInterfaceOrientationMaskLandscapeLeft;

        case UIInterfaceOrientationLandscapeRight:
            return UIInterfaceOrientationMaskLandscapeRight;

        case UIInterfaceOrientationUnknown:
        default:
            return UIInterfaceOrientationMaskAll;
    }
}

@end
