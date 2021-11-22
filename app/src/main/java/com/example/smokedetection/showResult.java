package com.example.smokedetection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class showResult extends AppCompatActivity {

    String currentImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);

        int blackGrade = -1;
        Bitmap imageBitmap = null;

        Uri imageURI = getIntent().getData();
        ImageView iv = (ImageView) findViewById(R.id.image);
        iv.setImageURI(imageURI);

        try {
            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageURI);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (imageBitmap != null){
            blackGrade = calcBlackGrade(imageBitmap);
        }

        TextView tv = (TextView) findViewById(R.id.result);
        String resultStr = "烟雾黑度为：";
        if (blackGrade == 5)
            tv.setTextColor(getResources().getColor(R.color.red));
        else if (blackGrade > 0 && blackGrade < 5)
            tv.setTextColor(getResources().getColor(R.color.sky_blue));
        else if (blackGrade == 0)
            tv.setTextColor(getResources().getColor(R.color.bright_green));
        else
            tv.setTextColor(getResources().getColor(R.color.black));
        resultStr += blackGrade + "级";
        tv.setText(resultStr);
    }

    public void shareResult(View view) {
        Bitmap shareImage = getShareImage();
        File shareImageFile = null;
        try {
            shareImageFile = createImageFile();
            FileOutputStream out = new FileOutputStream(shareImageFile);
            shareImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Continue only if the File was successfully created
        Uri shareImageURI = null;
        if (shareImageFile != null) {
            shareImageURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",
                    shareImageFile);
        }

        if (shareImageURI != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, shareImageURI);
            shareIntent.setType("image/*");   //分享文件类型
            startActivity(Intent.createChooser(shareIntent, "分享"));
        }
    }

    private Bitmap getShareImage() {
        View view = getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap screenShotBm = view.getDrawingCache();

        int width = screenShotBm.getWidth();
        int height = screenShotBm.getHeight();
        int segment = height / 10;
        Bitmap retImage = Bitmap.createBitmap(screenShotBm, 0, segment, width, segment * 7);
        view.destroyDrawingCache();//注意,这里需要释放缓存

        return retImage;
    }

    private static int calcBlackGrade(Bitmap img) {
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高

        int[] pixels = new int[width * height]; //通过位图的大小创建像素点数组

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int) ((double) red * 0.3 + (double) green * 0.59 + (double) blue * 0.11);
                pixels[width * i + j] = grey;
            }
        }

        double blackness = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int index = width * i + j;
                blackness += ((double) (255 - pixels[index])) / 255.0;
            }
        }
        blackness /= width * height;
        if (blackness > 0.9)
            return 5;
        else if (blackness > 0.7)
            return 4;
        else if (blackness > 0.5)
            return 3;
        else if (blackness > 0.3)
            return 2;
        else if (blackness > 0.1)
            return 1;
        else if (blackness > 0)
            return 0;
        return -1;
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("RESULT");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentImagePath = image.getAbsolutePath();
        return image;
    }
}
