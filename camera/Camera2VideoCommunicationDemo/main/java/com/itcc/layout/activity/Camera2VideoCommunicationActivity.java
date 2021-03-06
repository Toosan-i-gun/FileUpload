package com.itcc.layout.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.itcc.layout.utils.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2VideoCommunicationActivity extends AppCompatActivity {

    private ImageView large_ttv;
    private TextureView small_ttv;
    private CameraManager cameraManager;
    private Handler childHandler;
    private HandlerThread handlerThread;
    private ImageReader imageReader;
    private String[] permission = {Manifest.permission.CAMERA};

    private CaptureRequest.Builder captureRequestBuilder;
    private Surface surface;
    private SurfaceTexture surfaceTexture;
    private String frontCameraId;
    private Size frontSize;
    private String backCameraId;
    private Size backSize;
    private CameraCaptureSession.CaptureCallback cameraCaptureSessionCaptureCallback;
    private CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice.StateCallback cameraDeviceStateCallback;
    private CameraDevice cameraDevice;
    private Image image;
    private boolean hasImage = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_communication);
        initView();
        initBind();
        initData();
    }

    private void initView() {
        large_ttv = findViewById(R.id.large_ttv);
        small_ttv = findViewById(R.id.small_ttv);
    }

    private void initBind() {
        small_ttv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                initCamera2();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera2() {
        initPermission();
        initCameraCaptureSessionCaptureCallback();
        initCameraCaptureSessionStateCallback();
        initCameraDeviceStateCallback();
        openCamera();
    }

    private void initPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permission, 1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCameraCaptureSessionCaptureCallback() {
        cameraCaptureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
            }

            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
//                Log.e(TAG, "onCaptureCompleted: ??????????????????");
//                Size size = request.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
            }

            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
            }

            @Override
            public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            }

            @Override
            public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
                super.onCaptureSequenceAborted(session, sequenceId);
            }

            @Override
            public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber) {
                super.onCaptureBufferLost(session, request, target, frameNumber);
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCameraCaptureSessionStateCallback() {
        cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
            //????????????????????????????????????Capture????????????
            @Override
            public void onConfigured(CameraCaptureSession session) {
                try {
                    cameraCaptureSession = session;
                    //???????????????????????? setRepeatingRequest() ?????????????????????????????????????????????????????????????????????????????????????????????
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), cameraCaptureSessionCaptureCallback, childHandler);

//                    mCameraCaptureSession.stopRepeating();//????????????   ??????????????????????????????????????????
//                    mCameraCaptureSession.abortCaptures();//????????????   ?????????????????????????????????????????????????????????????????????????????????activity??????????????????
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            //?????????????????????
            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        };
    }


    /**
     * ??????????????????????????????
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCameraDeviceStateCallback() {
        cameraDeviceStateCallback = new CameraDevice.StateCallback() {
            /**
             * ??????????????????
             * @param camera
             */
            @Override
            public void onOpened(CameraDevice camera) {
                cameraDevice = camera;
                try {
                    //surfaceTexture    ??????????????????
                    surfaceTexture = small_ttv.getSurfaceTexture();
                    Size matchingSize = frontSize;
                    surfaceTexture.setDefaultBufferSize(matchingSize.getWidth(), matchingSize.getHeight());//???????????????????????????
                    //surface?????????????????????????????????,surface.release();
                    surface = new Surface(surfaceTexture);
//                        CaptureRequest?????????????????????????????????,????????????????????????????????????,??????Camera2????????????????????????????????????,??????:
// ???????????????????????????      TEMPLATE_PREVIEW ?????????
//                        TEMPLATE_RECORD???????????????
//                        TEMPLATE_STILL_CAPTURE?????????
//                        TEMPLATE_VIDEO_SNAPSHOT??????????????????????????????????????????
//                        TEMPLATE_ZERO_SHUTTER_LAG???????????????????????????????????????????????????????????????????????????????????????????????????????????????
//                        TEMPLATE_MANUAL???????????????????????????????????????????????????????????????????????????????????????(?????????????????????????????????????????????)???
                    //??????????????????
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.addTarget(surface); //??????surface   ?????????????????????surface????????????????????? ???onDestroy?????????mCaptureRequest.removeTarget(mSurface);??????,?????????????????????

//                    ????????????????????????????????????????????????????????????YUV???????????????H264?????????????????????
                    captureRequestBuilder.addTarget(imageReader.getSurface());
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);//????????????
                    /**
                     * ??????????????????
                     * ????????????????????????????????????,?????????Arrays.asList(surface, mImageReader.getSurface())????????????
                     * ????????????????????????????????????????????????????????????surface,????????????/???????????????2??????????????????????????????2???
                     * ??????????????????????????????????????????????????????surface???????????????,?????????????????????????????????ImageReader?????????,??????????????????????????????ImageReader???surface???
                     */
                    cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), cameraCaptureSessionStateCallback, childHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            /**
             *??????????????????
             * @param camera
             */
            @Override
            public void onDisconnected(CameraDevice camera) {

            }

            /**
             * ?????????????????????
             * @param camera
             * @param error
             */
            @Override
            public void onError(CameraDevice camera, int error) {

            }

            /**
             * ??????????????????
             * @param camera
             */
            @Override
            public void onClosed(CameraDevice camera) {
                super.onClosed(camera);
            }
        };
    }

    /**
     * ???????????????
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(frontCameraId, cameraDeviceStateCallback, childHandler);
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initData() {
        initCameraManager();
        initChildHander();
        initImageReader();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCameraManager() {
        cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        float viewProportion = (float) small_ttv.getWidth() / (float) small_ttv.getHeight();
        String[] cameraIds = null;
        float selectProportion = 0;
        try {
            cameraIds = cameraManager.getCameraIdList();
            if (null == cameraIds && 0 == cameraIds.length) {
                return;
            }
            for (String id : cameraIds) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
                int facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (CameraCharacteristics.LENS_FACING_FRONT == facing) {
                    frontCameraId = id;
//                    frontCameraId = CameraCharacteristics.LENS_FACING_FRONT????????????
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                    for (int i = 0; i < sizes.length; i++) {
                        Size itemSize = sizes[i];
                        float itemSizeProportion = (float) itemSize.getHeight() / (float) itemSize.getWidth();
                        float differenceProportion = Math.abs(viewProportion - itemSizeProportion);
                        if (i == 0) {
                            frontSize = itemSize;
                            selectProportion = differenceProportion;
                            continue;
                        }
                        if (differenceProportion <= selectProportion) {
                            if (differenceProportion == selectProportion) {
                                if (frontSize.getWidth() + frontSize.getHeight() < itemSize.getWidth() + itemSize.getHeight()) {
                                    frontSize = itemSize;
                                    selectProportion = differenceProportion;
                                }
                            } else {
                                frontSize = itemSize;
                                selectProportion = differenceProportion;
                            }
                        }
                    }
                } else if (CameraCharacteristics.LENS_FACING_BACK == facing) {
                    backCameraId = id;
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                    for (int i = 0; i < sizes.length; i++) {
                        Size itemSize = sizes[i];
                        float itemSizeProportion = (float) itemSize.getHeight() / (float) itemSize.getWidth();
                        float differenceProportion = Math.abs(viewProportion - itemSizeProportion);
                        if (i == 0) {
                            backSize = itemSize;
                            selectProportion = differenceProportion;
                            continue;
                        }
                        if (differenceProportion <= selectProportion) {
                            if (differenceProportion == selectProportion) {
                                if (backSize.getWidth() + backSize.getHeight() < itemSize.getWidth() + itemSize.getHeight()) {
                                    backSize = itemSize;
                                    selectProportion = differenceProportion;
                                }
                            } else {
                                backSize = itemSize;
                                selectProportion = differenceProportion;
                            }
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initChildHander() {
        handlerThread = new HandlerThread("camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
    }

    private void initImageReader() {
//        ?????????????????????????????????????????????????????????ImageFormat.JPEG???image.getPlanes()[0]??????JPEG???????????????

//        ????????????????????????????????????????????????ImageFormat.YV12??????ImageFormat.YUV_420_888
//        image.getPlanes()[0]??????Y???image.getPlanes()[1]??????U???image.getPlanes()[2]??????V

        //ImageReader?????????NV21,YUY2?????????YUV_420_888,YV12,JPEG
        imageReader = ImageReader.newInstance(frontSize.getWidth(),
                frontSize.getHeight(), ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
//                reader.acquireNextImage()
                image = reader.acquireLatestImage();
                int width = image.getWidth();
                int height = image.getHeight();
                byte[] yuvBytes = ImageUtils.getYUVBytesFromImageWithType(image, ImageUtils.NV21);
//----------------------------------
                YuvImage localYuvImage = new YuvImage(yuvBytes, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
                //??????????????????????????????YUV??????????????????????????????JPEG??????????????????????????????
                localYuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, localByteArrayOutputStream);
                byte[] mParamArrayOfByte = localByteArrayOutputStream.toByteArray();
                //??????Bitmap
                BitmapFactory.Options localOptions = new BitmapFactory.Options();
                localOptions.inPreferredConfig = Bitmap.Config.RGB_565;  //???????????????????????????????????????565?????????+enum
                final Bitmap mCurrentBitmap = BitmapFactory.decodeByteArray(mParamArrayOfByte, 0, mParamArrayOfByte.length, localOptions);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        large_ttv.setImageBitmap(mCurrentBitmap);
                    }
                });
            }
        }, childHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (captureRequestBuilder != null) {
            captureRequestBuilder.removeTarget(surface);
            captureRequestBuilder = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        if (cameraCaptureSession != null) {
            try {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (childHandler != null) {
            childHandler.removeCallbacksAndMessages(null);
            childHandler = null;
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        cameraManager = null;
        cameraCaptureSessionStateCallback = null;
        cameraCaptureSessionCaptureCallback = null;
        cameraDeviceStateCallback = null;

    }
}
