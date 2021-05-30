package com.itcc.camera.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CommicationSurfaceView extends SurfaceView implements Camera.PreviewCallback, SurfaceHolder.Callback {

    private Camera camera;
    private byte[] bytes;
    private BitmapCallback callback;

    public CommicationSurfaceView(Context context) {
        super(context);
    }

    public CommicationSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CommicationSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(int width, int height) {
        bytes = new byte[width * height * 3 / 2];
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

//        Camera.CameraInfo.CAMERA_FACING_FRONT
//        Camera.CameraInfo.CAMERA_FACING_BACK
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        //TODO
//        try {
////            camera.setPreviewTexture(new SurfaceTexture(10));
//        } catch (IOException e) {
//            e.printStackTrace();
//            camera.release();
//            camera = null;
//        }
        camera.setDisplayOrientation(90);
    }

    public void release() {
//        关闭摄像头资源
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camer) {
//        Camera1返回的YUV数据格式默认是NV21
//        YUV_420_888格式很多，比如YUV420P（YU12  YV12  I420）、YUV420SP(NV12  NV21)
//        data就是Preview的图像YUV数据，Y代表亮度，U、V代表色度，各占1byte，大小是宽高积的3/2，
//        可以用YuvImage封装，解析的图像而这个天生是横着的
//        YuvImage只支持ImageFormat.NV21和ImageFormat.YUY2


//        据说将YUV数据（data）压缩成H264的包发送给服务器，服务端收到并解包读取H264数据，并以RTP/RTCP格式打包发给客户端
//        并且接收客户端的反馈，对传输速度等做相应的控制。android客户端主要完成从服务器接收实时码流数据，经过缓冲，进行视频数据解析
//        然后送去解码，最后在手机上播放
        if (data == null && camer == null) {
            return;
        }
        Camera.Size localSize = camer.getParameters().getPreviewSize();  //获得预览分辨率
        //YuvImage可以记录宽高，用YuvImage.getwidth就可以拿到
        YuvImage localYuvImage = new YuvImage(data, ImageFormat.NV21, localSize.width, localSize.height, null);
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        //把摄像头回调数据转成YUV，再按图像尺寸压缩成JPEG，从输出流中转成数组
        localYuvImage.compressToJpeg(new Rect(0, 0, localSize.width, localSize.height), 80, localByteArrayOutputStream);
        byte[] mParamArrayOfByte = localByteArrayOutputStream.toByteArray();
        //生成Bitmap
        BitmapFactory.Options localOptions = new BitmapFactory.Options();
        localOptions.inPreferredConfig = Bitmap.Config.RGB_565;  //构造位图生成的参数，必须为565。类名+enum
        Bitmap mCurrentBitmap = BitmapFactory.decodeByteArray(mParamArrayOfByte, 0, mParamArrayOfByte.length, localOptions);
        if (callback == null) {
            return;
        }
        callback.getBitmap(mCurrentBitmap, camer);
        //循环的去申请
        camera.addCallbackBuffer(bytes);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPictureSize(getHeight(), getWidth());
        parameters.setPreviewSize(getHeight(), getWidth());
        parameters.setPictureFormat(PixelFormat.JPEG);//ImageFormat.JPEG一样
        parameters.setPreviewFpsRange(4, 10);//.setPreviewFrameRate(25);//每秒25
        parameters.set("jpeg-quantity", 100);
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(parameters);
        camera.addCallbackBuffer(bytes);
        camera.setPreviewCallbackWithBuffer(this);//不知道为什么我的手机调用这个方法不会回调onPreviewFrame方法
        camera.setPreviewCallback(this);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
            camera.release();
            camera = null;
        }
        camera.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //终止回调，避免报错
        camera.setPreviewCallbackWithBuffer(null);
        camera.setPreviewCallback(null);
        release();
    }

    public interface BitmapCallback {
        public void getBitmap(Bitmap bitmap, Camera camera);
    }

    public void setBitmapCallback(BitmapCallback callback) {
        this.callback = callback;
    }
}
