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
//        ????????????
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
//        ??????surfaceHolder?????????????????????????????????
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
//              ?????????
                initCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
//                ???????????????
                releaseCamera();
            }
        });

        //        ??????????????????????????????
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
            //??????????????????
        } else {
            //???????????? ???????????????????????????  ??????????????????
            ActivityCompat.requestPermissions(this, permissions, 100);
//            requestPermissions(this, permissions, 100);
        }
//        ?????????????????????
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
//        ??????????????????????????????-----------------
        if (!hasCameraHardware()) {
            Log.d("CameraPictureActivity", "initCamera hasCameraHardware: " + hasCameraHardware());
            return;
        }

//        ?????????????????????????????????------------------------
//        ???????????????????????????????????????
        camera = Camera.open();
        if (null == camera) {
            Log.d("CameraPictureActivity", "initCamera camera: " + camera);
            Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        Log.d("CameraPictureActivity", "initCamera: " + info.orientation);//?????????info.orientation???90

//        int orientation = getWindowManager().getDefaultDisplay().getOrientation();//?????????orientation???0
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
        List<String> flashModes = parameters.getSupportedFlashModes();//?????????????????????
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);//?????????????????????????????????????????????

//        ???????????????????????????,?????????????????????????????????-------------------------------------
        Camera.Size previewSize = getFitPreviewSize(parameters, width, height);
        if (null == previewSize) {
            parameters.setPreviewSize(height, width);
        } else {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }

//        ????????????????????????????????????????????????????????????
        parameters.setPreviewFpsRange(4, 25);//??????30?????????????????????
//        ?????????????????????
        parameters.setPictureFormat(ImageFormat.JPEG);
//        ?????????????????????
        parameters.set("jpeg-quantity", 100);

//        ?????????????????????------------------------------------------
        Camera.Size pictureSize = getFitPictureSize(parameters, width, height);
        if (null == pictureSize) {
            parameters.setPictureSize(height, width);
        } else {
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
        }

//        ??????
//        TODO
        List<String> focusModes = parameters.getSupportedFocusModes();
        focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);

        camera.setParameters(parameters);

        try {
//            ?????? SurfaceView??????????????????
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        ????????????
        camera.startPreview();
//        ??????????????????

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {

            }
        });
    }

    //    ?????????????????????????????????????????????????????????
    //    camera????????????????????????????????????????????????
    private Camera.Size getFitPictureSize(Camera.Parameters parameters, int width, int height) {
        return getFitPreviewSize(parameters, width, height, 2);
    }

    private Camera.Size getFitPreviewSize(Camera.Parameters parameters, int width, int height) {
        return getFitPreviewSize(parameters, width, height, 1);
    }

    //    ?????????????????????????????????????????????????????????
    //    camera????????????????????????????????????????????????
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

//        ??????????????????????????????????????????????????????????????????for?????????????????????????????????????????????????????????????????????
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

    //  Configuration.ORIENTATION_PORTRAIT??????
//  Configuration.ORIENTATION_LANDSCAPE??????
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
//        ?????????????????????
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
//            ???????????????????????????????????????
//            4.4?????????????????????????????????
//            ????????????????????????????????????
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
//                ?????????????????????
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
//                ?????????????????????
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
//                ?????????jepg?????????
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
     * ??????CAMERA?????????
     */
    public static final int REQUEST_CAMERA_PERM = 101;


    /**
     * EsayPermissions????????????????????????
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
//    EasyPermissions.requestPermissions(this, "????????????camera??????",REQUEST_CAMERA_PERM, Manifest.permission.CAMERA);

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Toast.makeText(this, "??????onPermissionsGranted()...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "??????onPermissionsDenied()...", Toast.LENGTH_SHORT).show();
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle("??????App????????????camera??????,????????????????????????????")
                    .setPositiveButton("??????")
                    .setNegativeButton("??????")
                    .setRequestCode(REQUEST_CAMERA_PERM)
                    .build()
                    .show();
        }
    }
}


