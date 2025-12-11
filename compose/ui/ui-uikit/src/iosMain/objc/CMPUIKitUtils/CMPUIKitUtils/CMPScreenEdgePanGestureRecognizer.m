//
//  CMPScreenEdgePanGestureRecognizer.m
//  CMPUIKitUtils
//
//  Created by Andrei Salavei on 05.03.25.
//

#import "CMPScreenEdgePanGestureRecognizer.h"

@implementation CMPScreenEdgePanGestureRecognizer

- (BOOL)canPreventGestureRecognizer:(UIGestureRecognizer *)preventedGestureRecognizer {
    return [super canPreventGestureRecognizer:preventedGestureRecognizer];
}

- (BOOL)canBePreventedByGestureRecognizer:(UIGestureRecognizer *)preventingGestureRecognizer {
    return [super canBePreventedByGestureRecognizer:preventingGestureRecognizer];
}

@end
