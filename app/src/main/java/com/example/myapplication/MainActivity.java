package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import java.io.File;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    static {
        if (OpenCVLoader.initDebug()) Log.d(TAG, "installed");
        else Log.d(TAG, "uninstalled");
    }

    EditText mResultEt;
    ImageView mPreviewIv;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    String[] cameraPermission;
    String[] storagePermission;

    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle("Click + button to insert image");
        }

        mResultEt = findViewById(R.id.resultEt);
        mPreviewIv = findViewById(R.id.imageIv);

        //camera permission
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    //actionbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    //handle actionbar item click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.addImage){
            showImageImportDialog();
        }
        else if(id == R.id.settings){
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageImportDialog() {
        //item to display in dialog
        String[] items = {"Camera","Gallery"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        //set title
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0){
                    Toast.makeText(getApplicationContext(), "click Camera", Toast.LENGTH_SHORT).show();
                    //camera option clicked
                    //for OS marshmallow and above we need to ask runtime permission for camera and storage
                        if(!checkCameraPermission()){
                            //camera permission not allow, request it
                            requestCameraPermission();
                        }
                        else{
                            //permission allowed, take picture
                            pickCamera();
                        }
                }

                if(which == 1){
                    Toast.makeText(getApplicationContext(), "click Gallery", Toast.LENGTH_SHORT).show();
                    //gallery option clicked
                    if(!checkStoragePermission()){
                        //Storage permission not allow, request it
                        Toast.makeText(getApplicationContext(), "Gallery Permission not allowed", Toast.LENGTH_SHORT).show();
                        requestStoragePermission();
                    }
                    else{
                        //permission allowed, take picture
                        Toast.makeText(getApplicationContext(), "Gallery Permission allowed", Toast.LENGTH_SHORT).show();
                        pickGallery();
                    }
                }
            }
        });
        dialog.create().show(); //show dialog
    }

    private boolean checkCameraPermission() {

        //check camera permission and return the result
        //in order to get high quality image we have to save image to external storage first
        //before inserting to image view that's why storage permission will also be required

        Toast.makeText(getApplicationContext(), "check camera permission", Toast.LENGTH_SHORT).show();
        boolean result_cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result_write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result_cam && result_write;
    }
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }
    private void pickCamera() {
        //intent to take image from camera, it will also be save to storage to get high quality image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NewPic"); //title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image To Text"); //description
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Toast.makeText(this, "pick camera", Toast.LENGTH_SHORT).show();
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }


    private boolean checkStoragePermission() {
        Toast.makeText(getApplicationContext(), "check storage permission", Toast.LENGTH_SHORT).show();
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);
    }
    private void pickGallery() {
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set intent type to image
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);

    }

    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera();
                    } else {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickGallery();
                    } else {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        }
    }

    //handle image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //got image from camera
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) { //Crop image from gallery
                //got image from gallery now crop it
                Toast.makeText(this, "to crop gallery", Toast.LENGTH_SHORT).show();

                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)//enable image guidelines
                        .start(this);

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) { //Crop image taken by camera
                //got image from camera now crop it
                Toast.makeText(this, "to crop gallery", Toast.LENGTH_SHORT).show();
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON)//enable image guidelines
                        .start(this);
            }
        }
        //get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri(); // get image uri

                //set image to image view
                mPreviewIv.setImageURI(resultUri);

                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                int sourceWidth = 1366; // To scale to
                int thresholdMin = 85; // Threshold 80 to 105 is Ok
                int thresholdMax = 255; // Always 255

                Mat origin = new Mat();

                Utils.bitmapToMat(bitmap,origin);
                Size imgSize = origin.size();
                info("orgin size is ==> "+imgSize.height+" "+imgSize.width);
//                mPreviewIv.setImageBitmap(toBitmap(origin));

                //1.scaling image
                //Scaling of Image refers to the resizing of images. It is useful in image processing and manipulation in machine learning applications as it can reduce the time of training as less number of pixels, less is the complexity of the model.
                Mat origin_resize = new Mat();
                Imgproc.resize(origin,origin_resize, new Size(sourceWidth,imgSize.height * sourceWidth / imgSize.width),1.0, 1.0,
                        INTER_CUBIC); //INTER_CUBIC – a bicubic interpolation over 4×4 pixel neighborhood
                //ควรใช้เทคนิคแบบ Bicubic แทน เพราะให้ผลลัพธ์ที่ดีกว่า Bicubic เป็นเทคนิคที่ให้ผลลัพธ์ที่ดีกว่าสองแบบแรก เพราะจะมีการไล่โทนภาพที่สวยงามกว่า เหมาะสมกับการตกแต่งภาพถ่ายทุกประเภท
                //https://anuntachaaoo.wordpress.com/2015/11/19/%E0%B8%AD%E0%B8%98%E0%B8%B4%E0%B8%9A%E0%B8%B2%E0%B8%A2-%E0%B9%80%E0%B8%A1%E0%B8%99%E0%B8%B9-image-interpolation/

                writeImage("resize.jpg", origin_resize);
                info("origin resize size is ==> "+origin_resize.size().height+" "+origin_resize.size().width);
//                mPreviewIv.setImageBitmap(toBitmap(origin_resize));

                // 2. Convert the image to GRAY
                Mat originGray = new Mat();
                Imgproc.cvtColor(origin_resize, originGray, COLOR_BGR2GRAY);
                writeImage("gray.jpg", originGray);
//                mPreviewIv.setImageBitmap(toBitmap(originGray));

                //3. process noise
                //เป็น kernel
                Mat element1 = getStructuringElement(MORPH_RECT, new Size(2, 2), new Point(1, 1));
                Mat element2 = getStructuringElement(MORPH_RECT, new Size(2, 2), new Point(1, 1));

                //https://phyblas.hinaboshi.com/oshi12 ==> เทคนิคนี้ใช้กับภาพที่มีแค่สีขาวดำ
                Imgproc.dilate(originGray, originGray, element1); //การพอง หากมีช่องใดช่องหนึ่งเป็นสีขาวก็จะให้ผลเป็นสีขาวทันที ดังนั้นบริเวณที่อยู่ใกล้สีขาวก็จะเป็นสีขาวไปด้วย ทำให้บริเวณสีขาวขยายตัวขึ้น
                Imgproc.erode(originGray, originGray, element2); //"การกร่อน" (erode) ในที่นี้คือการนำเอาตัวกรองมาไล่กวาดบนภาพเพื่อลบบางส่วนทิ้งออกไป เช่นเดียวกับการที่ชายฝั่งหรือโขดหินเมื่อโดนน้ำกัดเซาะก็จะเกิดการกร่อนสูญเสียส่วนขอบไปแล้วยุบตัวลงได้
                //จะได้ภาพที่มีอาณาเขตสีขาวลดลง เปลี่ยนกลายเป็นสีดำ เหมือนกับว่าโดนกัดเซาะให้กร่อนหายไป
                mPreviewIv.setImageBitmap(toBitmap(originGray));

                //4. Image Smoothing techniques help in reducing the noise.
                Mat GaussianBlurMat = new Mat();
                Imgproc.GaussianBlur(originGray, GaussianBlurMat, new Size(3, 3), 0);
                mPreviewIv.setImageBitmap(toBitmap(GaussianBlurMat));

                //5.threshold ==> Thresholding is a technique in OpenCV, which is the assignment of pixel values in relation to the threshold value provided
                //เป็นหลักการที่ใช้ค่าคงที่ค่าหนึ่ง ในการเปรียบเทียบกับค่าของ Pixel ในแต่ล่ะพื้น ถ้าค่าของ Pixel ในพื้นที่นั้นมีค่าน้อยกว่าค่าคงที่ ก็จะเปลี่ยนค่า Pixel ของพื้นที่นั้นเป็น 0 แต่ถ้าค่าของ Pixel ในพื้นที่นั้นมีค่ามากกว่าก็จะเปลี่ยนค่า Pixel ของพื้นที่นั้นเป็น 255
                //https://www.pyimagesearch.com/2021/04/28/opencv-thresholding-cv2-threshold/
                Mat thresholdMat = new Mat();
                //ไม่ผ่าน ขาวมาก
                //Imgproc.threshold(GaussianBlurMat, thresholdMat, thresholdMin, thresholdMax, THRESH_BINARY); //basic thresholding  + thresholdMin = our threshold
                //ผ่าน
                //Imgproc.adaptiveThreshold(GaussianBlurMat,thresholdMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
                //ผ่าน
                //Imgproc.adaptiveThreshold(GaussianBlurMat,thresholdMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
                mPreviewIv.setImageBitmap(toBitmap(thresholdMat));

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                if (!recognizer.isOperational()) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                } else {
                    Frame frame = new Frame.Builder().setBitmap(toBitmap(thresholdMat)).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();

                    //get text from sb until there is no text
                    for (int i = 0; i < items.size(); i++) {
                        TextBlock myItem = items.valueAt(i);
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }
                    //set text to edit text
                    mResultEt.setText(sb.toString());
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                //if there is any error show it
                Exception error = result.getError();
                Toast.makeText(this, "" + error, Toast.LENGTH_SHORT).show();
            }
        }

    }
    private void writeImage(String name, Mat origin_resize) {

        String appPath = Environment.getExternalStorageDirectory()+"/Pictures/iLLergi/RecognizeTextOCR/";
        info("Writing " + appPath + name + "...");
        File path = new File(appPath);
        if(!path.exists()) {
            path.mkdir();
            info("make dir ");
        }
        Imgcodecs.imwrite(appPath + name, origin_resize);

    }

    public static Bitmap toBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    public static void info(Object msg) {
        Log.i(TAG, msg.toString());
    }


}