package com.reactnativecomponent.barcode;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;

import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.reactnativecomponent.barcode.decoding.DecodeUtil;


public class RCTBarCodeUtilModule extends ReactContextBaseJavaModule {

    private static final int REQUEST_CODE_GET_PIC_URI = 11;
    private Boolean isTorchOn = false;
    private Camera camera;

    public RCTBarCodeUtilModule(ReactApplicationContext reactContext) {
        super(reactContext);

    }

    @Override
    public String getName() {
        return "BarcodeUtil";
    }


    @ReactMethod
    public void decodePictureFromPhotos(final Promise promise) {
        int checked = ContextCompat.checkSelfPermission(getReactApplicationContext()
                , Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (checked == PackageManager.PERMISSION_GRANTED) {
            goPicture();
            getReactApplicationContext().addActivityEventListener(new BaseActivityEventListener() {
                @Override
                public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                    try {
                        super.onActivityResult(activity, requestCode, resultCode, data);
                        if (data == null) promise.reject("error", "do not have data");
                        if (resultCode == -1) {
                            switch (requestCode) {
                                case REQUEST_CODE_GET_PIC_URI:
                                    Uri uri = data.getData();
                                    String imagePath = getPicturePathFromUri(uri);
                                    //对获取到的二维码照片进行压缩
                                    String res = DecodeUtil.getStringFromQRCode(imagePath);
                                    promise.resolve(res == null ? "" : res);
                                    break;
                            }
                        }
                    }catch (Exception e){
                        promise.reject("error", e.getMessage());
                    }

                }
            });
        } else {
            promise.reject("error", "do not have permission");
        }

    }

    @ReactMethod
    public void switchFlashLightState(Boolean newState, Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager cameraManager =
                    (CameraManager) this.getReactApplicationContext().getSystemService(Context.CAMERA_SERVICE);

            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, newState);
                promise.resolve(true);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                promise.reject("error", errorMessage);
            }
        } else {
            Camera.Parameters params;

            if (!isTorchOn) {
                camera = Camera.open();
                params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
                camera.startPreview();
                isTorchOn = true;
            } else {
                params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

                camera.setParameters(params);
                camera.stopPreview();
                camera.release();
                isTorchOn = false;
            }
        }
    }

    private void goPicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        getCurrentActivity().startActivityForResult(intent, REQUEST_CODE_GET_PIC_URI);
    }


    private String getPicturePathFromUri(Uri uri) {
        int sdkVersion = Build.VERSION.SDK_INT;
        if (sdkVersion >= 19) {
            return getPicturePathFromUriAboveApi19(uri);
        } else {
            return getPicturePathFromUriBelowAPI19(uri);
        }
    }

    private String getPicturePathFromUriBelowAPI19(Uri uri) {
        String path = null;

        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = getReactApplicationContext().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    private String getPicturePathFromUriAboveApi19(Uri uri) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(getCurrentActivity(), uri)) {
            // 如果是document类型的 uri, 则通过document id来进行处理
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isMediaDocument(uri)) { // MediaProvider
                // 使用':'分割
                String id = documentId.split(":")[1];

                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = {id};
                filePath = getDataColumn(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs);
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                filePath = getDataColumn(contentUri, null, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是 content 类型的 Uri
            filePath = getDataColumn(uri, null, null);
        } else if ("file".equals(uri.getScheme())) {
            // 如果是 file 类型的 Uri,直接获取图片对应的路径
            filePath = uri.getPath();
        }
        return filePath;
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private String getDataColumn(Uri uri, String selection, String[] selectionArgs) {
        String path = null;

        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = getReactApplicationContext().getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

}
