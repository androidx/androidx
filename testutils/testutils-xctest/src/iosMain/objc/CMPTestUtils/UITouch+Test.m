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

#import "UITouch+Test.h"
#import <objc/runtime.h>
#import "HIDEvent.h"

#pragma mark - UIEvent private methods
@interface UIEvent (CMPTestPrivate)

- (void)_addTouch:(UITouch *)touch forDelayedDelivery:(BOOL)arg2;
- (void)_clearTouches;
- (void)_setHIDEvent:(IOHIDEventPtr)event;

@end

#pragma mark - UIApplication private methods
@interface UIApplication (CMPTestPrivate)

- (UIEvent *)_touchesEvent;

@end

typedef struct {
    unsigned int _firstTouchForView:1;
    unsigned int _isTap:1;
    unsigned int _isDelayed:1;
    unsigned int _sentTouchesEnded:1;
    unsigned int _abandonForwardingRecord:1;
} UITouchFlags;

#pragma mark - ActiveTouchesHolder

@interface CMPActiveTouchesHolder : NSObject

@property (strong, nonatomic) NSMutableArray<UITouch *> *touches;

+ (instancetype)shared;

@end

@implementation CMPActiveTouchesHolder

+ (instancetype)shared {
    static CMPActiveTouchesHolder *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _touches = [NSMutableArray new];
    }
    return self;
}

@end

#pragma mark - UITouch extension

@interface UITouch (CMPTestPrivate)

- (void)setWindow:(UIWindow *)window;
- (void)setView:(UIView *)view;
- (void)setTapCount:(NSInteger)tapCount;
- (void)setIsTap:(BOOL)isTap;
- (void)setTimestamp:(NSTimeInterval)timestamp;
- (void)setGestureView:(UIView *)view;
- (void)_setLocationInWindow:(CGPoint)location resetPrevious:(BOOL)resetPrevious;
- (void)_setIsFirstTouchForView:(BOOL)firstTouchForView;
- (void)_setIsTapToClick:(BOOL)tapToClick;

- (void)_setHidEvent:(IOHIDEventPtr)event;
- (void)_setEdgeType:(NSInteger)edgeType;

- (void)setPhase:(UITouchPhase)touchPhase;
- (UITouchPhase)phase;

@end

@implementation UITouch (CMPTest)

+ (instancetype)touchAtPoint:(CGPoint)point
                    inWindow:(UIWindow *)window
                    tapCount:(NSInteger)tapCount
                    fromEdge:(BOOL)fromEdge {
    return [[UITouch alloc] initAtPoint:point inWindow:window tapCount:tapCount fromEdge:fromEdge];
}

+ (UIEvent *)getTouchesEvent {
    return [UIApplication.sharedApplication _touchesEvent];
}

+ (void)endAllTouches {
    [CMPActiveTouchesHolder.shared.touches removeAllObjects];
}

- (id)initAtPoint:(CGPoint)point inWindow:(UIWindow *)window tapCount:(NSInteger)tapCount fromEdge:(BOOL)fromEdge {
	self = [super init];
    if (self) {
        UIView *hitTestView = [window hitTest:point withEvent:[[UIApplication sharedApplication] _touchesEvent]];

        [self setWindow:window];
        [self setView:hitTestView];
        [self setTapCount:tapCount];
        [self _setLocationInWindow:point resetPrevious:YES];
        [self setPhase:UITouchPhaseBegan];
        [self _setEdgeType:fromEdge ? 4 : 0];
        [self _setIsFirstTouchForView:YES];
        
        [self updateTimestamp];
        
        if ([self respondsToSelector:@selector(setGestureView:)]) {
            [self setGestureView:hitTestView];
        }
        
        IOHIDEventPtr event = HIDEventWithTouches(@[self]);
        [self _setHidEvent:event];
        CFRelease(event);

        [CMPActiveTouchesHolder.shared.touches addObject:self];
    }
    
	return self;
}

- (void)setLocationInWindow:(CGPoint)locationInWIndow {
    [self _setLocationInWindow:locationInWIndow resetPrevious:NO];
}

- (CGPoint)locationInWindow {
    return [self locationInView:self.view.window];
}

- (void)updateTimestamp {
    [self setTimestamp:[[NSProcessInfo processInfo] systemUptime]];
}

- (void)send {
    if (![CMPActiveTouchesHolder.shared.touches containsObject:self]) {
        [CMPActiveTouchesHolder.shared.touches addObject:self];
    }

    UIEvent *event = [[UIApplication sharedApplication] _touchesEvent];
    [event _clearTouches];
    IOHIDEventPtr hidEvent = HIDEventWithTouches(CMPActiveTouchesHolder.shared.touches);
    [event _setHIDEvent:hidEvent];

    [self updateTimestamp];
    [event _addTouch:self forDelayedDelivery:NO];
    
    [[UIApplication sharedApplication] sendEvent:event];
}

- (void)endTouch {
    [CMPActiveTouchesHolder.shared.touches removeObject:self];
}

@end
