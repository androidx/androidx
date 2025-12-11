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

#import "CMPHoverGestureHandler.h"

API_AVAILABLE(ios(13.0))
@interface CMPHoverGestureRecognizer : UIHoverGestureRecognizer

@property (strong, nonatomic, nullable) UIEvent *lastReceivedEvent;

@end

@implementation CMPHoverGestureRecognizer

- (BOOL)shouldReceiveEvent:(UIEvent *)event {
    BOOL shouldReceive = [super shouldReceiveEvent:event];
    if (shouldReceive) {
        self.lastReceivedEvent = event;
    }
    return shouldReceive;
}

@end


@interface CMPHoverGestureHandler()

@property (strong, nonatomic) id gestureRecognizer;

@end

@implementation CMPHoverGestureHandler

- (instancetype)initWithTarget:(id)target action:(SEL)action {
    self = [super init];
    if (self) {
        if (@available(iOS 13.0, *)) {
            CMPHoverGestureRecognizer *gestureRecognizer = [[CMPHoverGestureRecognizer alloc] initWithTarget:target action:action];
            gestureRecognizer.delaysTouchesBegan = NO;
            gestureRecognizer.delaysTouchesEnded = NO;
            gestureRecognizer.cancelsTouchesInView = NO;
            
            _gestureRecognizer = gestureRecognizer;
        }
    }
    return self;
}

- (void)attachToView:(nonnull UIView *)view {
    if (@available(iOS 13.0, *)) {
        [view addGestureRecognizer:(CMPHoverGestureRecognizer *)self.gestureRecognizer];
    }
}

- (void)detachFromViewAndDispose:(nonnull UIView *)view {
    if (@available(iOS 13.0, *)) {
        [view removeGestureRecognizer:(CMPHoverGestureRecognizer *)self.gestureRecognizer];
        self.gestureRecognizer = nil;
    }
}

- (UIEvent *)lastHandledEvent {
    if (@available(iOS 13.0, *)) {
        return ((CMPHoverGestureRecognizer *)_gestureRecognizer).lastReceivedEvent;
    } else {
        return nil;
    }
}

@end
