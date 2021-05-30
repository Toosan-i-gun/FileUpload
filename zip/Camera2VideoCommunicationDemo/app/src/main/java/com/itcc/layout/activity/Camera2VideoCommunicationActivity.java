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
//                Log.e(TAG, "onCaptureCompleted: 触发接收数据");
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
            //摄像头完成配置，可以处理Capture请求了。
            @Override
            public void onConfigured(CameraCaptureSession session) {
                try {
                    cameraCaptureSession = session;
                    //注意这里使用的是 setRepeatingRequest() 请求通过此捕获会话无休止地重复捕获图像。用它来一直请求预览图像
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), cameraCaptureSessionCaptureCallback, childHandler);

//                    mCameraCaptureSession.stopRepeating();//停止重复   取消任何正在进行的重复捕获集
//                    mCameraCaptureSession.abortCaptures();//终止获取   尽可能快地放弃当前挂起和正在进行的所有捕获。请只在销毁activity的时候调用它
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            //摄像头配置失败
            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        };
    }


    /**
     * 初始化摄像头状态回调
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCameraDeviceStateCallback() {
        cameraDeviceStateCallback = new CameraDevice.StateCallback() {
            /**
             * 摄像头打开时
             * @param camera
             */
            @Override
            public void onOpened(CameraDevice camera) {
                cameraDevice = camera;
                try {
                    //surfaceTexture    需要手动释放
                    surfaceTexture = small_ttv.getSurfaceTexture();
                    Size matchingSize = frontSize;
                    surfaceTexture.setDefaultBufferSize(matchingSize.getWidth(), matchingSize.getHeight());//设置预览的图像尺寸
                    //surface最好在销毁的时候要释放,surface.release();
                    surface = new Surface(surfaceTexture);
//                        CaptureRequest可以完全自定义拍摄参数,但是需要配置的参数太多了,所以Camera2提供了一些快速配置的参数,如下:
// 　　　　　　　　　      TEMPLATE_PREVIEW ：预览
//                        TEMPLATE_RECORD：拍摄视频
//                        TEMPLATE_STILL_CAPTURE：拍照
//                        TEMPLATE_VIDEO_SNAPSHOT：创建视视频录制时截屏的请求
//                        TEMPLATE_ZERO_SHUTTER_LAG：创建一个适用于零快门延迟的请求。在不影响预览帧率的情况下最大化图像质量。
//                        TEMPLATE_MANUAL：创建一个基本捕获请求，这种请求中所有的自动控制都是禁用的(自动曝光，自动白平衡、自动焦点)。
                    //创建预览请求
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.addTarget(surface); //添加surface   实际使用中这个surface最好是全局变量 在onDestroy的时候mCaptureRequest.removeTarget(mSurface);清除,否则会内存泄露

//                    这一步的目的是过去预览的每一帧，然后获取YUV数据并压缩H264格式发往服务器
                    captureRequestBuilder.addTarget(imageReader.getSurface());
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);//自动对焦
                    /**
                     * 创建获取会话
                     * 这里会有一个容易忘记的坑,那就是Arrays.asList(surface, mImageReader.getSurface())这个方法
                     * 这个方法需要你导入后面需要操作功能的所有surface,比如预览/拍照如果你2个都要操作那就要导入2个
                     * 否则后续操作没有添加的那个功能就报错surface没有准备好,这也是我为什么先初始化ImageReader的原因,因为在这里就可以拿到ImageReader的surface了
                     */
                    cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), cameraCaptureSessionStateCallback, childHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            /**
             *摄像头断开时
             * @param camera
             */
            @Override
            public void onDisconnected(CameraDevice camera) {

            }

            /**
             * 出现异常情况时
             * @param camera
             * @param error
             */
            @Override
            public void onError(CameraDevice camera, int error) {

            }

            /**
             * 摄像头关闭时
             * @param camera
             */
            @Override
            public void onClosed(CameraDevice camera) {
                super.onClosed(camera);
            }
        };
    }

    /**
     * 打开摄像头
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
//                    frontCameraId = CameraCharacteristics.LENS_FACING_FRONT；方式二
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
//        如果这里使用的是拍照功能的话，必须使用ImageFormat.JPEG，image.getPlanes()[0]就是JPEG格式的数据

//        如果是实时视频，这里我们就要使用ImageFormat.YV12或者ImageFormat.YUV_420_888
//        image.getPlanes()[0]代表Y，image.getPlanes()[1]代表U，image.getPlanes()[2]代表V

        //ImageReader不支持NV21,YUY2，支持YUV_420_888,YV12,JPEG
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
                //把摄像头回调数据转成YUV，再按图像尺寸压缩成JPEG，从输出流中转成数组
                localYuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, localByteArrayOutputStream);
                byte[] mParamArrayOfByte = localByteArrayOutputStream.toByteArray();
                //生成Bitmap
                BitmapFactory.Options localOptions = new BitmapFactory.Options();
                localOptions.inPreferredConfig = Bitmap.Config.RGB_565;  //构造位图生成的参数，必须为565。类名+enum
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
