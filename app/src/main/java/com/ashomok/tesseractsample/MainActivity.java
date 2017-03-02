package com.ashomok.tesseractsample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ashomok.tesseractsample.tools.BitmapUtils;
import com.ashomok.tesseractsample.tools.RequestPermissionsTool;
import com.ashomok.tesseractsample.tools.RequestPermissionsToolImpl;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    static final int PHOTO_REQUEST_CODE = 1;
    private TessBaseAPI tessBaseApi;

    TextView textView;
    Uri outputFileUri;
    private static final String lang = "ara";
    String result = "empty";
    private RequestPermissionsTool requestTool; //for API >=23 only

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/";
    private static final String TESSDATA = "tessdata";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button captureImg = (Button) findViewById(R.id.action_btn);
        if (captureImg != null) {
            captureImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textView.setText("Processing");
                    startCameraActivity();
                }
            });
        }

        textView = (TextView) findViewById(R.id.textResult);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }
    }


    /**
     * to get high resolution image from camera
     */
    private void startCameraActivity() {
        try {
            String IMGS_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/imgs";
            prepareDirectory(IMGS_PATH);

            String img_path = IMGS_PATH + "/ocr.jpg";

            outputFileUri = Uri.fromFile(new File(img_path));

            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, PHOTO_REQUEST_CODE);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        //making photo
        if (requestCode == PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            doOCR();
        } else {
            Toast.makeText(this, "ERROR: Image was not obtained.", Toast.LENGTH_SHORT).show();
        }
    }

    private void doOCR() {
        prepareTesseract();
        startOCR(outputFileUri);
    }

    /**
     * Prepare directory on external storage
     *
     * @param path
     * @throws Exception
     */
    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed, check does Android Manifest have " +
                        "permission to write to external storage.");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }
    }


    private void prepareTesseract() {
        try {
            prepareDirectory(DATA_PATH + TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        copyTessDataFiles(TESSDATA);
    }

    /**
     * Copy tessdata files (located on assets/tessdata) to destination directory
     *
     * @param path - name of directory with .traineddata files
     */
    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = getAssets().list(path);

            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + path + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = getAssets().open(path + "/" + fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.toString());
        }
    }


    /**
     * don't run this code in main thread - it stops UI thread. Create AsyncTask instead.
     * http://developer.android.com/intl/ru/reference/android/os/AsyncTask.html
     *
     * @param imgUri
     */
    private void startOCR(final Uri imgUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because
                    // you need more memory in the heap to store your data.
                    Bitmap bitmap = BitmapFactory.decodeFile(imgUri.getPath(), options);

                    bitmap = convertBitmap(bitmap);

                    Log.d("Jc", "Starting extract text");
                    result = extractText(bitmap);

                    Log.d("Jc", "Result: " + result);
                    updateText(result);

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

            private Bitmap convertBitmap(Bitmap bitmap) {
                String cacheDir = MainActivity.this.getExternalCacheDir().getAbsolutePath();
                BitmapUtils.storeBitmapIntoFile(bitmap, cacheDir + "/original.jpg");
                BitmapUtils.storeBitmapIntoFile(BitmapUtils.invertColor(bitmap), cacheDir + "/inverted.jpg");
                BitmapUtils.storeBitmapIntoFile(
                        BitmapUtils.convertToMonocromoBitmap(bitmap), cacheDir + "/monocromed.jpg");
                BitmapUtils.storeBitmapIntoFile(BitmapUtils.noiseReduction(bitmap), cacheDir + "/noiseReduction.jpg");
                BitmapUtils.storeBitmapIntoFile(BitmapUtils.convertGreyScale(bitmap), cacheDir + "/greyScale.jpg");
                BitmapUtils.storeBitmapIntoFile(
                        BitmapUtils.noiseReduction(
                                BitmapUtils.convertGreyScale(bitmap)), cacheDir + "/greyScaleAndNoiseReduction.jpg");
                BitmapUtils.storeBitmapIntoFile(
                        BitmapUtils.convertToMonocromoBitmap(
                                BitmapUtils.invertColor(bitmap)), cacheDir + "/invertedAndMonocrome.jpg");
                Bitmap invertedAndMonocromeAndNoiseReduction = BitmapUtils.noiseReduction(
                        BitmapUtils.convertToMonocromoBitmap(
                                BitmapUtils.invertColor(bitmap)));
                BitmapUtils.storeBitmapIntoFile(
                        invertedAndMonocromeAndNoiseReduction,
                        cacheDir + "/invertedAndMonocromeAndNoiseReduction.jpg");
                return invertedAndMonocromeAndNoiseReduction;
            }
        }).start();
    }

    @UiThread
    private void updateText(String text) {
        textView.setText(text);
    }


    private String extractText(Bitmap bitmap) {
        try {
            Log.d("Jc", "Creating TessBaseApi");
            tessBaseApi = new TessBaseAPI();
        } catch (Exception e) {
            Log.e("Jc", e.getMessage(), e);
            if (tessBaseApi == null) {
                Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
            }
        }


        Log.d("Jc", "Inittialazing TestBaseApi");
        tessBaseApi.init(DATA_PATH, lang);

//       //EXTRA SETTINGS
//        //For example if we only want to detect numbers
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,
// "ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz0123456789()+-* ");

        Log.d("Jc", "Seting Up WhiteList");
        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "٩ ٨ ٧ ٦ ٥ ٤ ٣ ٢ ١ ٠ؤ ء ئ ة ي و ه ن م ل ك ق ف غ ع ظ ط" +
                " ض ص ش س ز ر ذ د خ ح ج ث ت ب إ لإ أ لأ لآ ا()+-* ");
//
//        //blackList Example
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
//                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

        Log.d("Jc", "Training file loaded");
        tessBaseApi.setImage(bitmap);
        String extractedText = "empty result";
        try {
            extractedText = tessBaseApi.getUTF8Text();
        } catch (Exception e) {
            Log.e("Jc", "Error in recognizing text.", e);
        }

        Log.d("Jc", "Closing TessBaseApi");
        tessBaseApi.end();
        return extractedText;
    }


    private void requestPermissions() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestTool = new RequestPermissionsToolImpl();
        requestTool.requestPermissions(this, permissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {

        boolean grantedAllPermissions = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                grantedAllPermissions = false;
            }
        }

        if (grantResults.length != permissions.length || (!grantedAllPermissions)) {

            requestTool.onPermissionDenied();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }
}


