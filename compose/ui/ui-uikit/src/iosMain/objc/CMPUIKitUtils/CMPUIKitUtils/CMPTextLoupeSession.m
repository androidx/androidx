/*
 * Copyright 2024 The Android Open Source Project
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

#import "CMPTextLoupeSession.h"
#import <UIKit/UIKit.h>

@implementation CMPTextLoupeSession {
    id _session;
}

+ (nullable instancetype)beginLoupeSessionAtPoint:(CGPoint)point
                          fromSelectionWidgetView:(nullable UIView *)selectionWidget
                                           inView:(UIView *)interactionView {
    CMPTextLoupeSession *session = [CMPTextLoupeSession new];
    if (@available(iOS 17, *)) {
        session->_session = [UITextLoupeSession beginLoupeSessionAtPoint:point
                                                 fromSelectionWidgetView:selectionWidget
                                                                  inView:interactionView];
    } else {
        [NSException raise:@"UITextLoupeSession is not available" format:@"The method must be called from iOS 17+"];
    }
    return session;
}

- (UITextLoupeSession *)session API_AVAILABLE(ios(17.0)) {
    return (UITextLoupeSession *)_session;
}

- (void)moveToPoint:(CGPoint)point
      withCaretRect:(CGRect)caretRect
      trackingCaret:(BOOL)tracksCaret API_AVAILABLE(ios(17.0)) {
    [self.session moveToPoint:point withCaretRect:caretRect trackingCaret:tracksCaret];
}

- (void)invalidate API_AVAILABLE(ios(17.0)) {
    [self.session invalidate];
}

@end
