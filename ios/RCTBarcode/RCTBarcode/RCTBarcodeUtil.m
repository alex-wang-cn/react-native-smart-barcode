//
//  nativeConstant.m
//  RNExample
//
//  Created by puti on 2018/4/28.
//  Copyright © 2018年 Facebook. All rights reserved.
//

#import "RCTBarcodeUtil.h"
#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>

@implementation RCTBarcodeUtil {
    RCTPromiseResolveBlock _resolve;
}
RCT_EXPORT_MODULE();
+ (BOOL)requiresMainQueueSetup
{
    return YES;
}
RCT_EXPORT_METHOD(decodePictureFromPhotos:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    _resolve = resolve;
    // 1.判断相册是否可以打开
    if (![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypePhotoLibrary]) {
        reject(@"error",@"do not have permission",nil);
        return;
    }
    
    UIImagePickerController *pickerController = [[UIImagePickerController alloc] init];
    pickerController.sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
    pickerController.allowsEditing = false;//启动编辑功能
    // 4.设置代理
    pickerController.delegate = self;
    // 5.modal出这个控制器
    dispatch_async(dispatch_get_main_queue(), ^{
        [[self getRootVC] presentViewController:pickerController animated:YES completion:nil];
    });
}

RCT_EXPORT_METHOD(switchFlashLightState:(BOOL *)newState)
{
    if ([AVCaptureDevice class]) {
        AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
        if ([device hasTorch]){
            [device lockForConfiguration:nil];
            
            if (newState) {
                [device setTorchMode:AVCaptureTorchModeOn];
            } else {
                [device setTorchMode:AVCaptureTorchModeOff];
            }
            
            [device unlockForConfiguration];
        }
    }
}


- (UIViewController*) getRootVC {
    UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
    while (root.presentedViewController != nil) {
        root = root.presentedViewController;
    }
    
    return root;
}

// 获取图片后的操作
- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    // 销毁控制器
    BOOL allowsEditing = picker.allowsEditing;
    [picker dismissViewControllerAnimated:YES completion:nil];
    NSString *scannedResult = @"";
    // 设置图片
    UIImage *image = allowsEditing ? info[UIImagePickerControllerEditedImage]
    :info[UIImagePickerControllerOriginalImage]
    ;
    if(image){
        //1. 初始化扫描仪，设置设别类型和识别质量
        CIDetector*detector = [CIDetector detectorOfType:CIDetectorTypeQRCode context:nil options:@{ CIDetectorAccuracy : CIDetectorAccuracyHigh }];
        //2. 扫描获取的特征组
        NSArray *features = [detector featuresInImage:[CIImage imageWithCGImage:image.CGImage]];
        if([features count] > 0){
            //3. 获取扫描结果
            CIQRCodeFeature *feature = [features objectAtIndex:0];
            scannedResult = feature.messageString;
        }
    }
    _resolve(scannedResult);
    
}
@end
