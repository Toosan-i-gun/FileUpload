package com.itcc.camera.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.itcc.camera.utils.CheckPermissinUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class CameraPictureActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {

    private ImageView pic_image;
    private SurfaceView surface_view;
    private SurfaceHolder surfaceHolder;

    private Camera camera;
    private WindowManager manager;
    private Display display;
    private int width;
    private int height;
    private DisplayMetrics metrics;
    private View view;
    private RelativeLayout nev_pos_rl;
    private Button neg_btn;
    private Button pos_btn;
    private Button take_btn;
    private Bitmap bitmap;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        initView();
        initBind();
        initData();
    }

    private void initView() {
        nev_pos_rl = findViewById(R.id.nev_pos_rl);
        neg_btn = findViewById(R.id.neg_btn);
        pos_btn = findViewById(R.id.pos_btn);
        take_btn = findViewById(R.id.take_btn);
        surface_view = findViewById(R.id.surface_view);
        surfaceHolder = surface_view.getHolder();
//        设置surfaceHolder不需要维护自己的缓存区
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
//              初始化
                initCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
//                关闭摄像头
                releaseCamera();
            }
        });

        //        加载拍照后承载的布局
        view = getLayoutInflater().inflate(R.layout.pic_main, null);
        pic_image = view.findViewById(R.id.pic_image);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void initBind() {
        neg_btn.setOnClickListener(this);
        pos_btn.setOnClickListener(this);
        take_btn.setOnClickListener(this);
    }

    private void initData() {
        String[] permissions = CheckPermissinUtils.checkPermission(getApplication());
        if (permissions.length == 0) {
            //权限都申请了
        } else {
            //申请权限 这种写法兼容低版本  下面不能兼容
            ActivityCompat.requestPermissions(this, permissions, 100);
//            requestPermissions(this, permissions, 100);
        }
//        获取屏幕的宽高
        manager = getWindowManager();
        display = manager.getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
//        metrics = new DisplayMetrics();
//        display.getMetrics(metrics);
//        width = metrics.widthPixels;
//        height = metrics.heightPixels;
    }

    private void initCamera() {
//        如果没有摄像头就返回-----------------
        if (!hasCameraHardware()) {
            Log.d("CameraPictureActivity", "initCamera hasCameraHardware: " + hasCameraHardware());
            return;
        }

//        此处默认打开后置摄像头------------------------
//        通过传参可以打开前置摄像头
        camera = Camera.open();
        if (null == camera) {
            Log.d("CameraPictureActivity", "initCamera camera: " + camera);
            Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        Log.d("CameraPictureActivity", "initCamera: " + info.orientation);//这里的info.orientation为90

//        int orientation = getWindowManager().getDefaultDisplay().getOrientation();//这里的orientation为0
//        Log.d("CameraPictureActivity", "initCamera orientation:  " + orientation);
//        int degree = 0;
//        switch (orientation) {
//            case Surface.ROTATION_0:
//                degree = 0;
//                break;
//            case Surface.ROTATION_90:
//                degree = 90;
//                break;
//            case Surface.ROTATION_180:
//                degree = 180;
//                break;
//            case Surface.ROTATION_270:
//                degree = 270;
//                break;
//        }

        if (info.orientation == 0) {
            camera.setDisplayOrientation(0);
        } else if (info.orientation == 90) {
            camera.setDisplayOrientation(90);
        } else if (info.orientation == 180) {
            camera.setDisplayOrientation(180);
        } else if (info.orientation == 270) {
            camera.setDisplayOrientation(270);
        }

//        TODO
        Camera.Parameters parameters = camera.getParameters();
        List<String> flashModes = parameters.getSupportedFlashModes();//闪光模式的种类
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);//这里可以用按钮来切换闪光的模式

//        设置预览照片的大小,目的是为了不让图片变形-------------------------------------
        Camera.Size previewSize = getFitPreviewSize(parameters, width, height);
        if (null == previewSize) {
            parameters.setPreviewSize(height, width);
        } else {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }

//        设置预览照片每秒显示多少帧的最小和最大值
        parameters.setPreviewFpsRange(4, 25);//据说30最大，待查资料
//        设置照片的格式
        parameters.setPictureFormat(ImageFormat.JPEG);
//        设置图片的质量
        parameters.set("jpeg-quantity", 100);

//        设置图片的大小------------------------------------------
        Camera.Size pictureSize = getFitPictureSize(parameters, width, height);
        if (null == pictureSize) {
            parameters.setPictureSize(height, width);
        } else {
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
        }

//        对焦
//        TODO
        List<String> focusModes = parameters.getSupportedFocusModes();
        focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);

        camera.setParameters(parameters);

        try {
//            通过 SurfaceView显示取景画面
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        开始预览
        camera.startPreview();
//        自动获取焦点

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {

            }
        });
    }

    //    这里需要注意的是屏幕的宽高一般是竖屏的
    //    camera图片的宽高是横屏的，他们是相反的
    private Camera.Size getFitPictureSize(Camera.Parameters parameters, int width, int height) {
        return getFitPreviewSize(parameters, width, height, 2);
    }

    private Camera.Size getFitPreviewSize(Camera.Parameters parameters, int width, int height) {
        return getFitPreviewSize(parameters, width, height, 1);
    }

    //    这里需要注意的是屏幕的宽高一般是竖屏的
    //    camera预览的宽高是横屏的，他们是相反的
    private Camera.Size getFitPreviewSize(Camera.Parameters parameters, int width, int height, int type) {
        int previewWidth = 0;
        int previewHeight = 0;
        if (Configuration.ORIENTATION_PORTRAIT == getWindowOrientation1()) {
            previewWidth = height;
            previewHeight = width;
        } else if (Configuration.ORIENTATION_LANDSCAPE == getWindowOrientation1()) {
            previewWidth = width;
            previewHeight = height;
        }

//        若传进来的宽高是根据屏幕的大小来传的，这一步for都会找，若定义的是小窗口基本会走下面的比例算法
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        if (2 == type) {
            sizes = parameters.getSupportedPictureSizes();
        }

        for (Camera.Size size : sizes) {
            if (previewWidth == size.width && previewHeight == size.height) {
                return size;
            }
        }

        float ratio = ((float) previewWidth) / ((float) previewHeight);
        float supportRatio;
        float fit;
        float minFit = Float.MAX_VALUE;
        ;
        float max = Float.MAX_VALUE;
        float min = Float.MIN_VALUE;
        Log.d("1111111111111", "getFitPreviewSize: " + max);
        for (int i = 0; i < sizes.size(); i++) {
            supportRatio = ((float) sizes.get(i).width) / ((float) sizes.get(i).height);
            fit = Math.abs(ratio - supportRatio);
            if (fit < max) {
                minFit = Math.min(fit, minFit);
            }
        }

        for (int i = 0; i < sizes.size(); i++) {
            supportRatio = ((float) sizes.get(i).width) / ((float) sizes.get(i).height);
            fit = Math.abs(ratio - supportRatio);
            if (fit == minFit) {
                return sizes.get(i);
            }
        }

        return null;
    }

    private int getWindowOrientation1() {
        return getResources().getConfiguration().orientation;
    }

    private int getWindowOrientation2() {
        return getRequestedOrientation();
    }

    //  Configuration.ORIENTATION_PORTRAIT竖屏
//  Configuration.ORIENTATION_LANDSCAPE横屏
    private void setWindowOrientation(int orientation) {
        setRequestedOrientation(orientation);
    }

    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    private boolean hasCameraHardware() {

        boolean x = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        boolean y = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean hasCameraHardware2() {
        int num = Camera.getNumberOfCameras();
        if (num < 1) {
            return false;
        }
        return true;
    }

    private void releaseCamera() {
//        关闭摄像头资源
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.take_btn:
                take_btn.setClickable(false);
                takepic();
                break;
            case R.id.neg_btn:
                again();
                break;
            case R.id.pos_btn:
                store();
                break;
        }
    }

    private void again() {
        take_btn.setClickable(true);
        take_btn.setVisibility(View.VISIBLE);
        nev_pos_rl.setVisibility(View.GONE);
        camera.startPreview();
    }

    private void store() {
        again();
        pic_image.setImageBitmap(bitmap);

        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String time = String.valueOf(System.currentTimeMillis());
        File file = new File(dir + File.separator + time + ".jpg");
        Log.d("TAG", "store: " + file.toString());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        boolean status = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        Log.d("TAG", "store: " + status);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            Log.d("TAG", "store: " + contentUri.toString());
            this.sendBroadcast(mediaScanIntent);
        } else {
//            让图集去加载刚刚拍照的图片
//            4.4以后是不允许这样操作的
//            所有要这样写，做版本适配
            Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED);
            intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
            sendBroadcast(intent);
        }
    }

    private void takepic() {
        if (camera == null) {
            return;
        }
        camera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
//                按下快门的回调
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
//                原始照片的回调
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
//                返回的jepg的回调
                if (data == null) {
                    return;
                }
                take_btn.setVisibility(View.GONE);
                nev_pos_rl.setVisibility(View.VISIBLE);
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    /**
     * 请求CAMERA权限码
     */
    public static final int REQUEST_CAMERA_PERM = 101;


    /**
     * EsayPermissions接管权限处理逻辑
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

//    EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)
//    // Ask for one permission
//    EasyPermissions.requestPermissions(this, "需要请求camera权限",REQUEST_CAMERA_PERM, Manifest.permission.CAMERA);

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Toast.makeText(this, "执行onPermissionsGranted()...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "执行onPermissionsDenied()...", Toast.LENGTH_SHORT).show();
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle("当前App需要申请camera权限,需要打开设置页面么?")
                    .setPositiveButton("确认")
                    .setNegativeButton("取消")
                    .setRequestCode(REQUEST_CAMERA_PERM)
                    .build()
                    .show();
        }
    }
}


