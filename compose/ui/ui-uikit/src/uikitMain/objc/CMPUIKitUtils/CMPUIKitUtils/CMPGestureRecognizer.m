//
//  CMPGestureRecognizer.m
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 28/06/2024.
//

#import "CMPGestureRecognizer.h"

@implementation CMPGestureRecognizer

- (instancetype)init {
    self = [super init];
    
    if (self) {
        self.cancelsTouchesInView = false;
        self.delegate = self;
    }
    
    return self;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    id <CMPGestureRecognizerHandler> handler = self.handler;
    
    if (handler) {
        return [handler shouldRecognizeSimultaneously:gestureRecognizer withOther:otherGestureRecognizer];
    } else {
        return NO;
    }
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesBegan:touches withEvent:event];
    
    if (self.state == UIGestureRecognizerStatePossible) {
        self.state = UIGestureRecognizerStateBegan;
    }
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesMoved:touches withEvent:event];
    
    switch (self.state) {
        case UIGestureRecognizerStateBegan:
        case UIGestureRecognizerStateChanged:
            self.state = UIGestureRecognizerStateChanged;
            break;
        default:
            break;
    }
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesEnded:touches withEvent:event];
    
    switch (self.state) {
        case UIGestureRecognizerStateBegan:
        case UIGestureRecognizerStateChanged:
            if (self.numberOfTouches == 0) {
                self.state = UIGestureRecognizerStateEnded;
            } else {
                self.state = UIGestureRecognizerStateChanged;
            }
            break;
        default:
            break;
    }
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.handler touchesCancelled:touches withEvent:event];
    
    switch (self.state) {
        case UIGestureRecognizerStateBegan:
        case UIGestureRecognizerStateChanged:
            if (self.numberOfTouches == 0) {
                self.state = UIGestureRecognizerStateCancelled;
            } else {
                self.state = UIGestureRecognizerStateChanged;
            }
            break;
        default:
            break;
    }
}

@end
