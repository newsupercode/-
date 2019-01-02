package com.example.administrator.myapplication;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import win.smartown.android.library.certificateCamera.CameraActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.iv_image);
    }

    /**
     * 车牌号
     */
    public void numberIdCard(View view) {
//        CameraActivity.openCertificateCamera(this, CameraActivity.TYPE_COMPANY_PORTRAIT);
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("type", CameraActivity.TYPE_COMPANY_PORTRAIT);
        startActivityForResult(intent, CameraActivity.REQUEST_CODE);
    }

    /**
     * 行驶证
     */
    public void driveIdCard(View view) {
//        CameraActivity.openCertificateCamera(this, CameraActivity.TYPE_COMPANY_LANDSCAPE);
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("type", CameraActivity.TYPE_COMPANY_LANDSCAPE);
        startActivityForResult(intent, CameraActivity.REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("MainActivity", "onActivityResult");
        if (requestCode == CameraActivity.REQUEST_CODE && resultCode == CameraActivity.RESULT_CODE) {
            //获取图片路径，显示图片
//            final String path = CameraActivity.getResult(data);
            String path = data.getStringExtra("result");

            Log.e("MainActivity", "path=====" + path);
            if (!TextUtils.isEmpty(path)) {
                imageView.setImageBitmap(BitmapFactory.decodeFile(path));
            }
        }
    }
}
