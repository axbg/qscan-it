package com.example.qr.qscan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qr.qscan.constant.Constant;
import com.example.qr.qscan.network.HTTPManager;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Constant {

    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private TextView qrTextView;
    private TextView responseTextView;
    private BarcodeDetector barcodeDetector;
    private Boolean shouldDetect = false;
    private final int REQUEST_PERMISSION_CAMERA = 1;
    private Button detect;
    private String token;
    private HTTPManager httpManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initActivity();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initActivity(){

        Intent intent = getIntent();
        token = intent.getStringExtra(CREDENTIALS);

        this.qrTextView = findViewById(R.id.main_tv_qr);
        this.responseTextView = findViewById(R.id.main_tv_response);

        this.detect = findViewById(R.id.main_btn_detect);

        this.detect.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        shouldDetect = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        shouldDetect = false;
                        break;
                }
                return false;
            }
        });

        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        } else {
            initCamera();
        }
    }

    private void initCamera(){
        this.barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();

        this.cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 520).build();

        this.surfaceView = findViewById(R.id.main_surface_view);

        this.surfaceView.setVisibility(View.VISIBLE);

        this.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED){
                    return;
                }

                try {
                    cameraSource.start(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>(){

            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if(shouldDetect) {
                    shouldDetect = false;
                    final SparseArray<Barcode> qrCode = detections.getDetectedItems();

                    if (qrCode.size() != 0) {
                        qrTextView.post(new Runnable() {
                            @SuppressLint("StaticFieldLeak")
                            @Override
                            public void run() {

                                httpManager = new HTTPManager(){
                                    @Override
                                    protected void onPostExecute(String s) {
                                        try {
                                            JSONObject obj = new JSONObject(s);
                                            String message = obj.getString("message");

                                            //check if returned message is ok
                                            //circle loader removed

                                            responseTextView.setText(message);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                };

                                Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                vibrator.vibrate(200);

                                String qrURL = qrCode.valueAt(0).displayValue;

                                if(qrURL.contains("https://scanit.sisc.ro")){

                                    //add REST to the decoded url
                                    String RESTURL = "asd";

                                    //a circle loader can be added here
                                    httpManager.execute(RESTURL, GET, token);

                                } else {
                                    Toast.makeText(getApplicationContext(), "QRCode recognized",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.initCamera();
            }
        }
    }



}