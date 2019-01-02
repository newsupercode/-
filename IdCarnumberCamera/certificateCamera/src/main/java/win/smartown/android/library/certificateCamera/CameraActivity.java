package win.smartown.android.library.certificateCamera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.edmodo.cropper.CropImageView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by smartown on 2018/2/24 11:46.
 * <br>
 * Desc:
 * <br>
 * 拍照界面
 */
public class CameraActivity extends Activity implements View.OnClickListener {

    /**
     * 拍摄类型-身份证正面
     */
    public final static int TYPE_IDCARD_FRONT = 1;
    /**
     * 拍摄类型-身份证反面
     */
    public final static int TYPE_IDCARD_BACK = 2;
    /**
     * 拍摄类型-竖版营业执照
     */
    public final static int TYPE_COMPANY_PORTRAIT = 3;
    /**
     * 拍摄类型-横版营业执照
     */
    public final static int TYPE_COMPANY_LANDSCAPE = 4;
    private boolean isToast;

    public final static int REQUEST_CODE = 0X11;//请求码
    public final static int RESULT_CODE = 0X12;//结果码
    public final static int PERMISSION_CODE_FIRST = 0x13;//权限请求码
    private View iv_camera_take;
    private TextView mViewCameraCropBottom;

    private Bitmap mCropBitmap;

    private CameraPreview cameraPreview;
    private View containerView;
    private ImageView cropView;
    private ImageView flashImageView;
    private View optionView;

    private int type;
    private CropImageView mCropImageView;
    private View ll_camera_result;
    private ImageView iv_camera_result_ok;
    private ImageView iv_camera_result_cancel;
    private SeekBar seekBar;

    /**
     * @param type {@link #TYPE_IDCARD_FRONT}
     *             {@link #TYPE_IDCARD_BACK}
     *             {@link #TYPE_COMPANY_PORTRAIT}
     *             {@link #TYPE_COMPANY_LANDSCAPE}
     */
    public static void openCertificateCamera(Activity activity, int type) {
        Intent intent = new Intent(activity, CameraActivity.class);
        intent.putExtra("type", type);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * @return 结果文件路径
     */
    public static String getResult(Intent data) {
        if (data != null) {
            return data.getStringExtra("result");
        }
        return "";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        /*动态请求需要的权限*/
//        boolean checkPermissionFirst = PermissionUtils.checkPermissionFirst(this, PERMISSION_CODE_FIRST,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
//        if (checkPermissionFirst) {
//            init();
//        }
        type = getIntent().getIntExtra("type", 0);
        if (type == TYPE_COMPANY_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            Log.e("TAG", "TYPE_COMPANY_PORTRAIT");
            setContentView(R.layout.activity_camera_portrait);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            Log.e("TAG", "SCREEN_ORIENTATION_LANDSCAPE");
            setContentView(R.layout.activity_camera_land);
        }

        iv_camera_take = findViewById(R.id.camera_take);
        Log.e("TAG", "iv_camera_take===null?" + (iv_camera_take == null));
        initView();
        initListener();

    }


    private void initView() {

        mViewCameraCropBottom = findViewById(R.id.view_camera_crop_bottom);
        cameraPreview = (CameraPreview) findViewById(R.id.camera_surface);
        containerView = findViewById(R.id.camera_crop_container);
        cropView = (ImageView) findViewById(R.id.camera_crop);
        flashImageView = (ImageView) findViewById(R.id.camera_flash);
        optionView = findViewById(R.id.camera_option);
        mCropImageView = findViewById(R.id.CropImageView);
        ll_camera_result = findViewById(R.id.ll_camera_result);
        iv_camera_result_ok = findViewById(R.id.iv_camera_result_ok);
        iv_camera_result_cancel = findViewById(R.id.iv_camera_result_cancel);
        switch (type) {
            case TYPE_IDCARD_FRONT:
                cropView.setImageResource(R.mipmap.camera_idcard_front);
                break;
            case TYPE_IDCARD_BACK:
                cropView.setImageResource(R.mipmap.camera_idcard_back);
                break;
            case TYPE_COMPANY_PORTRAIT:
                cropView.setImageResource(R.mipmap.camera_company);
                break;
            case TYPE_COMPANY_LANDSCAPE:
                cropView.setImageResource(R.mipmap.camera_company_landscape);
                break;
        }
        //获取屏幕最小边，设置为cameraPreview较窄的一边
        float screenMinSize = Math.min(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        //根据screenMinSize，计算出cameraPreview的较宽的一边，长宽比为标准的16:9
        float maxSize = screenMinSize / 9.0f * 16.0f;
        RelativeLayout.LayoutParams layoutParams;
        if (type == TYPE_COMPANY_PORTRAIT) {
            layoutParams = new RelativeLayout.LayoutParams((int) screenMinSize, (int) maxSize);
        } else {
            layoutParams = new RelativeLayout.LayoutParams((int) maxSize, (int) screenMinSize);
        }
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        cameraPreview.setLayoutParams(layoutParams);


        if (type == TYPE_COMPANY_PORTRAIT) {
            float width = (int) (screenMinSize * 0.95);
            float height = (int) (width * 43.0f / 63.0f);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) height);
            LinearLayout.LayoutParams cropParams = new LinearLayout.LayoutParams((int) width, (int) height);
            containerView.setLayoutParams(containerParams);
            cropView.setLayoutParams(cropParams);

            //放大效果
            seekBar = findViewById(R.id.seekBar);
            seekBar.setVisibility(View.VISIBLE);
//            RelativeLayout.LayoutParams layoutParamszoom = new RelativeLayout.LayoutParams((int) (widthScreen * 23 / 24), RelativeLayout.LayoutParams.WRAP_CONTENT);
//            layoutParamszoom.addRule(RelativeLayout.CENTER_HORIZONTAL);
//            layoutParamszoom.topMargin = (int) (100);
//            seekBar.setLayoutParams(layoutParamszoom);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    cameraPreview.setFocallength((int) (cameraPreview.getFocal() * progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
//                    cameraPreview.setRecogsuspended(true);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
//                    cameraPreview.setRecogsuspended(false);
                }
            });


        } else if (type == TYPE_COMPANY_LANDSCAPE) {
            float height = (int) (screenMinSize * 0.8);
            float width = (int) (height * 43.0f / 30.0f);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams((int) width, ViewGroup.LayoutParams.MATCH_PARENT);
            LinearLayout.LayoutParams cropParams = new LinearLayout.LayoutParams((int) width, (int) height);
            containerView.setLayoutParams(containerParams);
            cropView.setLayoutParams(cropParams);
        } else {
            float height = (int) (screenMinSize * 0.75);
            float width = (int) (height * 75.0f / 47.0f);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams((int) width, ViewGroup.LayoutParams.MATCH_PARENT);
            LinearLayout.LayoutParams cropParams = new LinearLayout.LayoutParams((int) width, (int) height);
            containerView.setLayoutParams(containerParams);
            cropView.setLayoutParams(cropParams);
        }


//        resultView = findViewById(R.id.camera_result);
    }

    private void initListener() {
        cameraPreview.setOnClickListener(this);
        findViewById(R.id.camera_close).setOnClickListener(this);
        iv_camera_take.setOnClickListener(this);
        flashImageView.setOnClickListener(this);
        iv_camera_result_cancel.setOnClickListener(this);
        iv_camera_result_ok.setOnClickListener(this);

          /*增加0.5秒过渡界面，解决个别手机首次申请权限导致预览界面启动慢的问题*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cameraPreview.setVisibility(View.VISIBLE);
                    }
                });
            }
        }, 500);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (cameraPreview != null) {
            cameraPreview.onStart();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cameraPreview != null) {
            cameraPreview.onStop();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.camera_surface) {
            cameraPreview.focus();
        } else if (id == R.id.camera_close) {
            finish();
        } else if (id == R.id.camera_take) {
            takePhoto();
        } else if (id == R.id.camera_flash) {
            boolean isFlashOn = cameraPreview.switchFlashLight();
            flashImageView.setImageResource(isFlashOn ? R.mipmap.camera_flash_on : R.mipmap.camera_flash_off);
        }
        if (id == R.id.iv_camera_result_ok) {
            Bitmap croppedImage = mCropImageView.getCroppedImage();

            final File cropFile = getCropFile();
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(cropFile));
                croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();

                confirm();
            } catch (Exception e) {
                e.printStackTrace();
            }



        } else if (id == R.id.iv_camera_result_cancel) {
            cameraPreview.setEnabled(true);
            cameraPreview.startPreview();
            flashImageView.setImageResource(R.mipmap.camera_flash_off);
            setTakePhotoLayout();
        }
    }

    /**
     * 设置拍照布局
     */
    private void setTakePhotoLayout() {
        cropView.setVisibility(View.VISIBLE);
        cameraPreview.setVisibility(View.VISIBLE);
        optionView.setVisibility(View.VISIBLE);
        iv_camera_take.setVisibility(View.VISIBLE);
        mCropImageView.setVisibility(View.GONE);
        ll_camera_result.setVisibility(View.GONE);
        mViewCameraCropBottom.setText(getString(R.string.touch_to_focus));
        seekBar.setVisibility(View.VISIBLE);
        cameraPreview.focus();
    }

    /**
     * 设置裁剪布局
     */
    private void setCropLayout() {
        cropView.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.GONE);
        optionView.setVisibility(View.GONE);
        iv_camera_take.setVisibility(View.GONE);
        mCropImageView.setVisibility(View.VISIBLE);
        ll_camera_result.setVisibility(View.VISIBLE);
        mViewCameraCropBottom.setText("");
        seekBar.setVisibility(View.GONE);

    }


//    /**
//     * 处理请求权限的响应
//     *
//     * @param requestCode  请求码
//     * @param permissions  权限数组
//     * @param grantResults 请求权限结果数组
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        boolean isPermissions = true;
//        for (int i = 0; i < permissions.length; i++) {
//            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
//                isPermissions = false;
//                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) { //用户选择了"不再询问"
//                    if (isToast) {
//                        Toast.makeText(this, "请手动打开该应用需要的权限", Toast.LENGTH_SHORT).show();
//                        isToast = false;
//                    }
//                }
//            }
//        }
//        isToast = true;
//        if (isPermissions) {
//            Log.d("onRequestPermission", "onRequestPermissionsResult: " + "允许所有权限");
//            init();
//        } else {
//            Log.d("onRequestPermission", "onRequestPermissionsResult: " + "有权限不允许");
//            finish();
//        }
//    }

    private void takePhoto() {
        cameraPreview.setEnabled(false);
        cameraPreview.takePhoto(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                //子线程处理图片，防止ANR
                new Thread(new Runnable() {
                    @Override
                    public void run() {
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        File originalFile = getOriginalFile();
                        FileOutputStream originalFileOutputStream = null;
                        try {
                            originalFileOutputStream = new FileOutputStream(originalFile);
                            originalFileOutputStream.write(data);
                            originalFileOutputStream.close();
// 取得图片旋转角度
                            int angle = readPictureDegree(originalFile.getPath());
                            Log.e("TAG", "degree====" + angle);

                            Bitmap bitmapori = BitmapFactory.decodeFile(originalFile.getPath());

                            // 修复图片被旋转的角度
                            Bitmap bitmap = rotaingImageView(angle, bitmapori);

                            //计算裁剪位置
                            float left, top, right, bottom;
                            if (type == TYPE_COMPANY_PORTRAIT) {
                                left = (float) cropView.getLeft() / (float) cameraPreview.getWidth();
                                top = ((float) containerView.getTop() + optionView.getHeight() - (float) cameraPreview.getTop()) / (float) cameraPreview.getHeight();
//                                top = (float) cropView.getTop() / (float) cameraPreview.getHeight();
                                right = (float) cropView.getRight() / (float) cameraPreview.getWidth();
                                bottom = ((float) containerView.getBottom() + optionView.getHeight()) / (float) cameraPreview.getHeight();
                            } else {
                                left = ((float) containerView.getLeft() + optionView.getWidth() - (float) cameraPreview.getLeft()) / (float) cameraPreview.getWidth();
                                top = (float) cropView.getTop() / (float) cameraPreview.getHeight();
                                right = ((float) containerView.getRight() + optionView.getWidth()) / (float) cameraPreview.getWidth();
                                bottom = ((float) cropView.getBottom()) / (float) cameraPreview.getHeight();
                            }
                            Log.e("TAG", "cropView.getLeft()==" + cropView.getLeft());
                            Log.e("TAG", "cropView.getRight==" + cropView.getRight());
                            Log.e("TAG", "cropView.getTop==" + cropView.getTop());
                            Log.e("TAG", "cropView.getBottom==" + cropView.getBottom());

                            Log.e("TAG", "containerView.getTop==" + containerView.getTop());
                            Log.e("TAG", "containerView.getBottom==" + containerView.getBottom());
                            Log.e("TAG", "containerView.getLeft==" + containerView.getLeft());
                            Log.e("TAG", "containerView.getRight==" + containerView.getRight());

                            Log.e("TAG", "cameraPreview.getTop==" + cameraPreview.getTop());
                            Log.e("TAG", "cameraPreview.getLeft==" + cameraPreview.getLeft());
                            Log.e("TAG", "cameraPreview.getWidth==" + cameraPreview.getWidth());
                            Log.e("TAG", "cameraPreview.getHeight==" + cameraPreview.getHeight());

                            Log.e("TAG", "bimap.getHeight==" + bitmap.getHeight());
                            Log.e("TAG", "left===" + left);
                            Log.e("TAG", "right===" + right);
                            Log.e("TAG", "top===" + top);
                            Log.e("TAG", "bottom===" + bottom);

                            Log.e("TAG", "optionView.getHeight()===" + optionView.getHeight());
                            //裁剪及保存到文件
                            mCropBitmap = Bitmap.createBitmap(bitmap,
                                    (int) (left * (float) bitmap.getWidth()),
                                    (int) (top * (float) bitmap.getHeight()),
                                    (int) ((right - left) * (float) bitmap.getWidth()),
                                    (int) ((bottom - top) * (float) bitmap.getHeight()));

                            Log.e("TAG", "bitmap.getWidth()===" + bitmap.getWidth());
                            Log.e("TAG", "mCropBitmap" + mCropBitmap.getWidth());


//                            final File cropFile = getCropFile();
//                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cropFile));
//                            mCropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
//                            bos.flush();
//                            bos.close();
//剪切等比例
//                            Bitmap bitmap2 = decodeSampledBitmapFromPath(cropFile.getAbsolutePath(), cropBitmap.getWidth() / 2, cropBitmap.getHeight() / 2);
////剪切按大小
//                            mCropBitmap = comp(bitmap2);
//
//
//                            final File cropRotateFile = getRotateFile();
//                            BufferedOutputStream bosRotate = new BufferedOutputStream(new FileOutputStream(cropRotateFile));
//                            mCropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bosRotate);
//                            bosRotate.flush();
//                            bosRotate.close();


                        /*手动裁剪*/
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //将裁剪区域设置成与扫描框一样大
                                    mCropImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                    mCropImageView.setGuidelines(2);
                                    setCropLayout();
                                    mCropImageView.setImageBitmap(mCropBitmap);

                                }
                            });
//                            confirm();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                }).start();

            }
        });
    }

    /**
     * @return 拍摄图片原始文件
     */
    private File getOriginalFile() {
        switch (type) {
            case TYPE_IDCARD_FRONT:
                return new File(getExternalCacheDir(), "idCardFront.jpg");
            case TYPE_IDCARD_BACK:
                return new File(getExternalCacheDir(), "idCardBack.jpg");
            case TYPE_COMPANY_PORTRAIT:
            case TYPE_COMPANY_LANDSCAPE:
                return new File(getExternalCacheDir(), "companyInfo.jpg");
        }
        return new File(getExternalCacheDir(), "picture.jpg");
    }

    /**
     * @return 拍摄图片裁剪文件
     */
    private File getCropFile() {
        switch (type) {
            case TYPE_IDCARD_FRONT:
                return new File(getExternalCacheDir(), "idCardFrontCrop.jpg");
            case TYPE_IDCARD_BACK:
                return new File(getExternalCacheDir(), "idCardBackCrop.jpg");
            case TYPE_COMPANY_PORTRAIT:
            case TYPE_COMPANY_LANDSCAPE:
                return new File(getExternalCacheDir(), "companyInfoCrop.jpg");
        }
        return new File(getExternalCacheDir(), "pictureCrop.jpg");
    }

    private File getRotateFile() {
        switch (type) {
            case TYPE_IDCARD_FRONT:
                return new File(getExternalCacheDir(), "idRotateCardFrontCrop.jpg");
            case TYPE_IDCARD_BACK:
                return new File(getExternalCacheDir(), "idRotateCardBackCrop.jpg");
            case TYPE_COMPANY_PORTRAIT:
            case TYPE_COMPANY_LANDSCAPE:
                return new File(getExternalCacheDir(), "RotatecompanyInfoCrop.jpg");
        }
        return new File(getExternalCacheDir(), "RotatepictureCrop.jpg");
    }
//    /**
//     * 点击对勾，使用拍照结果，返回对应图片路径
//     */
//    private void goBack() {
//        Intent intent = new Intent();
//        intent.putExtra("result", getCropFile().getPath());
//        setResult(RESULT_CODE, intent);
//        finish();
//    }

    /**
     * 点击确认，返回图片路径
     */
    private void confirm() {
        /*裁剪图片*/

        if (mCropBitmap == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.crop_fail), Toast.LENGTH_SHORT).show();
            finish();
        }
        Intent intent = new Intent();
        intent.putExtra("result", getCropFile().getPath());
        Log.e("TAG", "imagePath===" + getCropFile().getPath());
        setResult(RESULT_CODE, intent);
        finish();
    }

    public static Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = caculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    public static int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round((float) width * 1.0F / (float) reqWidth);
            int heightRadio = Math.round((float) height * 1.0F / (float) reqHeight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }

        return inSampleSize;
    }

    public static Bitmap comp(Bitmap image) {
        if (image == null) {
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            if (baos.toByteArray().length / 100 > 1024) {
                baos.reset();
                image.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            }

            ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(isBm, (Rect) null, newOpts);
            newOpts.inJustDecodeBounds = false;
            int w = newOpts.outWidth;
            int h = newOpts.outHeight;
            float hh = 800.0F;
            float ww = 480.0F;
            int be = 1;
            if (w > h && (float) w > ww) {
                be = (int) ((float) newOpts.outWidth / ww);
            } else if (w < h && (float) h > hh) {
                be = (int) ((float) newOpts.outHeight / hh);
            }

            if (be <= 0) {
                be = 1;
            }

            newOpts.inSampleSize = be;
            isBm = new ByteArrayInputStream(baos.toByteArray());
            Bitmap bitmap = BitmapFactory.decodeStream(isBm, (Rect) null, newOpts);
            return compressImage(bitmap);
        }
    }

    private static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        for (int options = 100; baos.toByteArray().length / 1024 > 100; options -= 10) {
            baos.reset();
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }

        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, (Rect) null, (BitmapFactory.Options) null);
        return bitmap;
    }


    /**
     * 读取照片旋转角度
     *
     * @param path 照片路径
     * @return 角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.e("TAG", "原图被旋转角度： ========== " + orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 旋转图片
     *
     * @param angle  被旋转角度
     * @param bitmap 图片对象
     * @return 旋转后的图片
     */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        Log.e("TAG", "angle===" + angle);
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }
}
