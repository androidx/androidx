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

#import <UIKit/UIKit.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPAccessibilityElement : UIAccessibilityElement <UIFocusItem>

- (NSArray<UIAccessibilityCustomAction *> *)accessibilityCustomActions;

- (UIAccessibilityTraits)accessibilityTraits;

- (UIAccessibilityContainerType)accessibilityContainerType;

- (NSString *__nullable)accessibilityIdentifier;

- (NSString *__nullable)accessibilityHint;

- (NSString *__nullable)accessibilityLabel;

- (NSString *__nullable)accessibilityValue;

- (CGRect)accessibilityFrame;

- (BOOL)isAccessibilityElement;

- (BOOL)accessibilityActivate;

- (void)accessibilityIncrement;

- (void)accessibilityDecrement;

- (void)accessibilityElementDidBecomeFocused;

- (void)accessibilityElementDidLoseFocus;

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction;

- (BOOL)accessibilityPerformEscape;

- (NSArray *)accessibilityElements;

- (void)setAccessibilityElements:(nullable NSArray *)accessibilityElements;

- (BOOL)drawsFocusRingWhenChildrenFocused;

- (CGRect)focusEffectRect;

- (UIFocusEffect *)focusEffect API_AVAILABLE(ios(15.0));

@end

NS_ASSUME_NONNULL_END
