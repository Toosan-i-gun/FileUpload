package com.itcc.camera.cameravideocommunicationdemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.itcc.camera.widget.CommicationSurfaceView;


public class CommunicationActivity extends AppCompatActivity {

    private ImageView large_sv;
    private CommicationSurfaceView small_sv;

    private Paint paint;
    private Matrix matrix;
    private Canvas canvas;
    private Bitmap largeBitmap;
    private int width;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //        设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.comm_activity);
        initView();
        initData();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    private void initView() {
        large_sv = findViewById(R.id.large_sv);
        small_sv = findViewById(R.id.small_sv);
        small_sv.setBitmapCallback(new CommicationSurfaceView.BitmapCallback() {

            @Override
            public void getBitmap(Bitmap bitmap, Camera camera) {

                //TODO
//                需要想到方法
//                用YUV旋转
//                largeBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
//                canvas = new Canvas(largeBitmap);
//                matrix.setRotate(-90, 0, 0);
//                matrix.postTranslate(0,height);
//                canvas.drawBitmap(bitmap, matrix, paint);
                large_sv.setImageBitmap(bitmap);

            }
        });
    }

    private void initData() {
        width = getWindowManager().getDefaultDisplay().getWidth();
        height = getWindowManager().getDefaultDisplay().getHeight();
        matrix = new Matrix();
        paint = new Paint();

        small_sv.init(width, height);
    }
}
