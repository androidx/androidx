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

@implementation CMPAccessibilityElement {
    BOOL _inDealloc;
}

- (instancetype)initWithAccessibilityContainer:(id)container {
    self = [super initWithAccessibilityContainer:container];
    
    if (self) {
        _inDealloc = NO;
    }
    
    return self;
}

- (void)dealloc {
    _inDealloc = YES;
}

- (void)setAccessibilityContainer:(__nullable id)accessibilityContainer {
    // NoOp
}

// Overrides default accessibilityContainer implementation.
- (__nullable id)accessibilityContainer {
    // see https://github.com/flutter/flutter/issues/87247
    // TODO: investigate if this bug is still present on iOS versions supported by Compose, if it's not, fuse `accessibilityContainer` and `resolveAccessibilityContainer` implementations into a single one (like in `CMPAccessibilityContainer`)
    if (_inDealloc) {
        return nil;
    }
    
    return [self resolveAccessibilityContainer];
}

- (__nullable id)resolveAccessibilityContainer {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

+ (__nullable id)accessibilityContainerOfObject:(id)object {
    // Duck-typing selector dispatch
    return [object accessibilityContainer];
}

- (NSArray<UIAccessibilityCustomAction *> *)accessibilityCustomActions {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (UIAccessibilityTraits)accessibilityTraits {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSString *__nullable)accessibilityIdentifier {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSString *__nullable)accessibilityHint {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSString *__nullable)accessibilityLabel {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSString *__nullable)accessibilityValue {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (CGRect)accessibilityFrame {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)isAccessibilityElement {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)accessibilityActivate {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)accessibilityPerformEscape {
    return [super accessibilityPerformEscape];
}

- (BOOL)accessibilityScrollToVisible {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)accessibilityScrollToVisibleWithChild:(id)child {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)accessibilityElementDidBecomeFocused {
    [super accessibilityElementDidBecomeFocused];
}

- (void)accessibilityElementDidLoseFocus {
    [super accessibilityElementDidLoseFocus];
}

@end

NS_ASSUME_NONNULL_END
