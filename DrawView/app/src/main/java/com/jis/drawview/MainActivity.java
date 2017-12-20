package com.jis.drawview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static final String INTENT_DATA_IMAGE_SAVEDPATH = "image_savepath";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadLayout();
    }

    private void loadLayout() {
        ui_imvImage = (ImageView) findViewById(R.id.activity_main_imv_image);
    }


    public void loadImage(String path) {

        File imgFile = new File(path);

        if (imgFile.exists()) {

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ui_imvImage.setImageBitmap(myBitmap);

        }

    }

    public void onNext(View v) {

        Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.sample);

        Intent intent = new Intent(MainActivity.this, DrawViewActivity.class);
        intent.putExtra(DrawViewActivity.INTENT_DATA_IMAGE_URI, path.toString());
        startActivityForResult(intent, 100);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {

            String path = data.getStringExtra(INTENT_DATA_IMAGE_SAVEDPATH);
            loadImage(path);

        }
    }

    private ImageView ui_imvImage;
}
