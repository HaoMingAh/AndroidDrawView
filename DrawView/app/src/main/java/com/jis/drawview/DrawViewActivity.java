package com.jis.drawview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.byox.drawview.enums.DrawingCapture;
import com.byox.drawview.views.DrawView;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by JIS on 1/30/2017.
 */

public class DrawViewActivity extends AppCompatActivity implements View.OnClickListener, DrawView.OnDrawViewListener {

    public static final String INTENT_DATA_IMAGE_URI = "image_uri";
    private final int STORAGE_PERMISSIONS = 1000;
    private final int STROKE_WIDTH = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawview);

        _inputImageUri = Uri.parse(getIntent().getStringExtra(INTENT_DATA_IMAGE_URI));

        loadLayout();
    }

    private void loadLayout() {

        ui_fltContainer = (FrameLayout) findViewById(R.id.activity_drawview_flt_container);
        ui_fltContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ui_fltContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                _contatinerW = ui_fltContainer.getMeasuredWidth();
                _contatinerH = ui_fltContainer.getMeasuredHeight();
                loadImage();
            }
        });

        ui_drawView = (DrawView) findViewById(R.id.draw_view);
        ui_drawView.setDrawWidth(STROKE_WIDTH);
        ui_drawView.setEnabled(false);
        ui_drawView.setOnDrawViewListener(this);

        ImageView imvClose = (ImageView) findViewById(R.id.activity_drawview_imv_close);
        imvClose.setOnClickListener(this);

        ui_imvUndo = (ImageView) findViewById(R.id.activity_drawview_imv_undo);
        ui_imvUndo.setOnClickListener(this);
        canUndo();

        ImageView imvEdit = (ImageView) findViewById(R.id.activity_drawview_imv_edit);
        imvEdit.setOnClickListener(this);

        ui_imvEditFill = (ImageView) findViewById(R.id.activity_drawview_imv_edit_fill);
        ui_imvEditFill.setVisibility(View.GONE);

        ImageView imvSend = (ImageView) findViewById(R.id.activity_drawview_imv_send);
        imvSend.setOnClickListener(this);

        ui_fltSeekbar = (FrameLayout) findViewById(R.id.activity_drawview_flt_seekbar);
        ui_fltSeekbar.setVisibility(View.GONE);

        ui_colorBar = (VerticalSeekBar_Reverse) findViewById(R.id.activity_drarwview_color_seekbar);
        ui_colorBar.setProgress(50);
        ui_colorBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                int color = getColorFromProgress(seekBar.getProgress());
                ui_imvEditFill.setColorFilter(Color.parseColor(String.format("#%06X", (0xFFFFFF & color))));
                ui_drawView.setDrawColor(color);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void loadImage() {

        if (_inputImageUri != null) {

            try {
                _loadedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), _inputImageUri);
                calculateDrawViewWH();
                ui_drawView.setBackground(new BitmapDrawable(getResources(), _loadedBitmap));
                ui_drawView.invalidate();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private void calculateDrawViewWH() {

        if (_loadedBitmap == null)
            return;

        int dw = 0;
        int dh = 0;

        int width = _loadedBitmap.getWidth();
        int height = _loadedBitmap.getHeight();

        float imageRatio = (float) width / (float) height;

        if (imageRatio * _contatinerH > _contatinerW) {

            dw = _contatinerW;
            dh = (int) (_contatinerW / imageRatio);
        } else {
            dw = (int) (_contatinerH * imageRatio);
            dh = _contatinerH;
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dw, dh);
        params.gravity = Gravity.CENTER;
        ui_drawView.setLayoutParams(params);
    }

    private String saveImage() {

        String filename = String.valueOf(System.currentTimeMillis()) + ".png";
        String path = getLocalFolderPath() + filename;

        try {
            File image = new File(path);
            image.createNewFile();

            FileOutputStream fileOutputStream = new FileOutputStream(image);

            Bitmap bitmap = (Bitmap) ui_drawView.createCapture(DrawingCapture.BITMAP);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return path;
    }

    private static String getLocalFolderPath() {

        String w_strTempFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DrawView/";
        File w_fTempFolder = new File(w_strTempFolderPath);
        if (w_fTempFolder.exists() && w_fTempFolder.isDirectory()) {
            return w_strTempFolderPath;
        } else {
            w_fTempFolder.mkdir();
            return w_strTempFolderPath;
        }
    }

    private void onClose() {

        finish();
    }

    private void updateUI() {

        ui_drawView.setEnabled(_isEditing);

        if (_isEditing) {
            ui_imvEditFill.setVisibility(View.VISIBLE);
            int color = getColorFromProgress(ui_colorBar.getProgress());
            ui_imvEditFill.setColorFilter(Color.parseColor(String.format("#%06X", (0xFFFFFF & color))));
            ui_drawView.setDrawColor(color);
            ui_fltSeekbar.setVisibility(View.VISIBLE);

        } else {
            ui_imvEditFill.setVisibility(View.GONE);
            ui_fltSeekbar.setVisibility(View.GONE);
        }
    }


    private void onEditToggle() {

        _isEditing = !_isEditing;
        updateUI();
    }

    private void canUndo() {

        if (ui_drawView.canUndo()) {
            ui_imvUndo.setVisibility(View.VISIBLE);
        } else {
            ui_imvUndo.setVisibility(View.GONE);
        }
    }

    private void onUndo() {

        if (ui_drawView.canUndo()) {
            ui_drawView.undo();
            canUndo();
        }

    }

    private void requestPermissions(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(DrawViewActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(DrawViewActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(DrawViewActivity.this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSIONS);
            } else {
                saveDraw();
            }
        } else {
            saveDraw();
        }
    }

    private void saveDraw() {

        String path = saveImage();

        if (path != null) {

            Intent intent = new Intent();
            intent.putExtra(MainActivity.INTENT_DATA_IMAGE_SAVEDPATH, path);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void onSend() {
        requestPermissions();
    }

    public int getColorFromProgress(int progress)
    {
        int color1 = 0, color2 = 0, color = 0;
        float p = 0;
        if(progress <= 19) /* black to red */
        {
            color1 = 0xffffff;
            color2 = 0x000000;
            p = progress / 19.0f;
        }
        else if(progress <= 33) /* red to yellow */
        {
            color1 = 0xff0014;
            color2 = 0xfffd00;
            p = (progress - 19) / 14.0f;
        }
        else if(progress <= 47) /* yellow to lime green */
        {
            color1 = 0xfffd00;
            color2 = 0x00ff0e;
            p = (progress - 33) / 14.0f;
        }
        else if(progress <= 61) /* lime green to aqua */
        {
            color1 = 0x00ff0e;
            color2 = 0x00dbff;
            p = (progress - 47) / 14.0f;
        }
        else if(progress <= 75) /* aqua to blue */
        {
            color1 = 0x00dbff;
            color2 = 0x0055ff;
            p = (progress - 61) / 14.0f;
        }
        else if(progress <= 89) /* blue to fuchsia */
        {
            color1 = 0x0055ff;
            color2 = 0xff00e5;
            p = (progress - 75) / 14.0f;
        }
        else /* fuchsia to white */
        {
            color1 = 0xff00e5;
            color2 = 0xff0013;
            p = (progress - 89) / 11.0f;
        }


        int r1 = (color1 >> 16) & 0xff;
        int r2 = (color2 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int g2 = (color2 >> 8) & 0xff;
        int b1 = (color1) & 0xff;
        int b2 = (color2) & 0xff;

        int r3 = (int) ((r2 * p) + (r1 * (1.0f-p)));
        int g3 = (int) (g2 * p + g1 * (1.0f-p));
        int b3 = (int) (b2 * p + b1 * (1.0f-p));

        color = r3 << 16 | g3 << 8 | b3;

        return color;
    }


    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.activity_drawview_imv_close:
                onClose();
                break;

            case R.id.activity_drawview_imv_edit:
                onEditToggle();
                break;

            case R.id.activity_drawview_imv_undo:
                onUndo();
                break;

            case R.id.activity_drawview_imv_send:
                onSend();
                break;
        }
    }


    @Override
    public void onStartDrawing() {
        canUndo();
    }

    @Override
    public void onEndDrawing() {

        if (ui_drawView.canUndo()) {
            ui_imvUndo.setVisibility(View.VISIBLE);
        } else {
            ui_imvUndo.setVisibility(View.GONE);
        }

    }

    @Override
    public void onClearDrawing() {
        canUndo();
    }

    @Override
    public void onRequestText() {

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isEditing", _isEditing);
        outState.putInt("color", ui_colorBar.getProgress());
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        _isEditing = savedInstanceState.getBoolean("isEditing");
        ui_colorBar.setProgress(savedInstanceState.getInt("color"));

        updateUI();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSIONS:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            saveDraw();
                        }
                    }, 100);
                }
                break;
        }
    }

    private FrameLayout ui_fltContainer, ui_fltSeekbar;
    private ImageView ui_imvUndo, ui_imvEditFill;
    private DrawView ui_drawView;
    private VerticalSeekBar_Reverse ui_colorBar;
    private Uri _inputImageUri;
    private boolean _isEditing = false;
    private int _contatinerW = 0;
    private int _contatinerH = 0;
    private Bitmap _loadedBitmap = null;


}
