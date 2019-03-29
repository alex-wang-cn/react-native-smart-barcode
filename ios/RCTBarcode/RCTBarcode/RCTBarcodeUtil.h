//
//  nativeConstant.h
//  RNExample
//
//  Created by puti on 2018/4/28.
//  Copyright © 2018年 Facebook. All rights reserved.
//
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif
#import <UIKit/UIKit.h>
@interface RCTBarcodeUtil : NSObject<RCTBridgeModule,UIImagePickerControllerDelegate, UINavigationControllerDelegate>

@end
