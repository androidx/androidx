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

#import "CMPAccessibilityElement.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPAccessibilityElement

+ (__nullable id)accessibilityContainerOfObject:(id)object {
    // Duck-typing selector dispatch
    return [object accessibilityContainer];
}

- (NSArray<UIAccessibilityCustomAction *> *)accessibilityCustomActions {
    return [super accessibilityCustomActions];
}

- (UIAccessibilityTraits)accessibilityTraits {
    return [super accessibilityTraits];
}

- (UIAccessibilityContainerType)accessibilityContainerType {
    return [super accessibilityContainerType];
}

- (NSString *__nullable)accessibilityIdentifier {
    return [super accessibilityIdentifier];
}

- (NSString *__nullable)accessibilityHint {
    return [super accessibilityHint];
}

- (NSString *__nullable)accessibilityLabel {
    return [super accessibilityLabel];
}

- (NSString *__nullable)accessibilityValue {
    return [super accessibilityValue];
}

- (CGRect)accessibilityFrame {
    return [super accessibilityFrame];
}

- (BOOL)isAccessibilityElement {
    return [super isAccessibilityElement];
}

- (BOOL)accessibilityActivate {
    return [super accessibilityActivate];
}

- (void)accessibilityIncrement {
    [super accessibilityIncrement];
}

- (void)accessibilityDecrement {
    [super accessibilityDecrement];
}

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction {
    return [super accessibilityScroll:direction];
}

- (BOOL)accessibilityPerformEscape {
    return [super accessibilityPerformEscape];
}

- (void)accessibilityElementDidBecomeFocused {
    [super accessibilityElementDidBecomeFocused];
}

- (void)accessibilityElementDidLoseFocus {
    [super accessibilityElementDidLoseFocus];
}

- (NSArray *)accessibilityElements {
    return [super accessibilityElements];
}

- (void)setAccessibilityElements:(nullable NSArray *)accessibilityElements {
    [super setAccessibilityElements:accessibilityElements];
}

- (BOOL)_drawsFocusRingWhenChildrenFocused {
    return [self drawsFocusRingWhenChildrenFocused];
}

- (BOOL)drawsFocusRingWhenChildrenFocused {
    return NO;
}

- (CGRect)focusEffectRect {
    return CGRectZero;
}

- (UIFocusEffect *)focusEffect {
    return [UIFocusHaloEffect effectWithRect:[self focusEffectRect]];
}

@end

NS_ASSUME_NONNULL_END
