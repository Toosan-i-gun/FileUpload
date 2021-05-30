package com.itcc.camera.cameravideocommunicationdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.itcc.camera.utils.CheckPermissionsUtils;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initBind();
        initData();
    }

    private void initView() {

    }

    private void initBind() {

    }

    private void initData() {
        String[] permissions = CheckPermissionsUtils.checkPermission(this);
        if (permissions.length == 0) {
            //权限都申请了
        } else {
            //申请权限 这种写法兼容低版本  下面不能兼容
            ActivityCompat.requestPermissions(this, permissions, 100);
//            requestPermissions(this, permissions, 100);
        }
    }

    public void communication(View view) {
        Intent intent = new Intent(this, CommunicationActivity.class);
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 请求CAMERA权限码
     */
    public static final int REQUEST_CAMERA_PERM = 101;
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
