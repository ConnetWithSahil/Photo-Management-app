package com.developer.PhotoManager;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CaptureImage extends AppCompatActivity {
    private ImageButton camerabtn , switchcamera;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result){
                startCamera(cameraFacing);
            }
        }

    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.capture_image);
        camerabtn = findViewById(R.id.capture);
        switchcamera = findViewById(R.id.switchCamera);
        previewView = findViewById(R.id.cameraPreview);

        if (ContextCompat.checkSelfPermission(CaptureImage.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){

            activityResultLauncher.launch(Manifest.permission.CAMERA);

        }else{
            startCamera(cameraFacing);
        }

        switchcamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraFacing== CameraSelector.LENS_FACING_BACK){
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                }else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing);
            }
        });
    }
    public void startCamera(int camerFacing){
        int aspectRatio = aspectRatio(previewView.getWidth(),previewView.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);
        listenableFuture.addListener(() -> {
            try {
                    ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();
                    Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

                    ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(camerFacing).build();

                    cameraProvider.unbindAll();

                    Camera camera = cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageCapture);

                    camerabtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (ContextCompat.checkSelfPermission(CaptureImage.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            }else {
                                takePicture(imageCapture);
                            }
                        }
                    });

                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(this));


    }

    public void takePicture(ImageCapture imageCapture){
        final File file = new File(getExternalFilesDir(null),System.currentTimeMillis()+".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CaptureImage.this,"Image saved at: "+file.getPath(),Toast.LENGTH_SHORT).show();
                    }
                });
                startCamera(cameraFacing);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       Toast.makeText(CaptureImage.this,"Failed to save: "+exception.getMessage(),Toast.LENGTH_SHORT).show();
                   }
               });
              startCamera(cameraFacing);

            }
        });
    }

    private int aspectRatio(int width ,int height){
        double previewRatio = (double) Math.max(width,height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <=Math.abs(previewRatio - 16.0 / 9.0)){
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }
}