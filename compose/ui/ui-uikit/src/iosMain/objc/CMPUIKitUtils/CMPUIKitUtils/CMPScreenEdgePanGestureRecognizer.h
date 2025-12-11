//
//  CMPScreenEdgePanGestureRecognizer.h
//  CMPUIKitUtils
//
//  Created by Andrei Salavei on 05.03.25.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface CMPScreenEdgePanGestureRecognizer : UIScreenEdgePanGestureRecognizer

- (BOOL)canPreventGestureRecognizer:(UIGestureRecognizer *)preventedGestureRecognizer;

- (BOOL)canBePreventedByGestureRecognizer:(UIGestureRecognizer *)preventingGestureRecognizer;

@end

NS_ASSUME_NONNULL_END
