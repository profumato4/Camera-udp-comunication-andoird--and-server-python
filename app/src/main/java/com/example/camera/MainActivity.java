package com.example.camera;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaActionSound;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cameraxexample.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String SSID = "TIM-29562191";
    private static final String PASSWORD = "rq7ngXZMQlc3NTVl1pZCNTlK";
    private final Context context = MainActivity.this;
    private WifiManager wifiManager;
    private final Executor executor = Executors.newSingleThreadExecutor ( );

    ImageButton capture, toggleFlash, flipCamera;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private final ActivityResultLauncher < String > activityResultLauncher = registerForActivityResult ( new ActivityResultContracts.RequestPermission ( ) , result -> {
        if ( result ) {
            startCamera ( cameraFacing );
        }
    } );


    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate ( savedInstanceState );
        setContentView ( R.layout.activity_main );

        previewView = findViewById ( R.id.cameraPreview );
        capture = findViewById ( R.id.capture );
        toggleFlash = findViewById ( R.id.toggleFlash );
        flipCamera = findViewById ( R.id.flipCamera );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            if ( ContextCompat.checkSelfPermission ( MainActivity.this , Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
                activityResultLauncher.launch ( Manifest.permission.CAMERA );
            } else {
                startCamera ( cameraFacing );
            }
        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ) {
            if ( ContextCompat.checkSelfPermission ( MainActivity.this , Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions ( MainActivity.this , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE} , 123 );

            } else if ( ContextCompat.checkSelfPermission ( MainActivity.this , Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions ( MainActivity.this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} , 369 );

            } else {

                startCamera ( cameraFacing );

            }


        }


        if ( !isWifiEnabled ( ) ) {
            enableWifi ( );
        } else if ( isWifiEnabled ( ) ) {
            connectToWifi2 ( );
        }

        flipCamera.setOnClickListener ( view -> {
            if ( cameraFacing == CameraSelector.LENS_FACING_BACK ) {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera ( cameraFacing );
        } );
    }


    private void connectToWifi ( ) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            final WifiNetworkSuggestion wifi =
                    new WifiNetworkSuggestion.Builder ( )
                            .setSsid ( SSID )
                            .setWpa2Passphrase ( PASSWORD )
                            .build ( );

            wifiManager = ( WifiManager ) context.getApplicationContext ( ).getSystemService ( Context.WIFI_SERVICE );
            wifiManager.addNetworkSuggestions ( Collections.singletonList ( wifi ) );
        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ) {
            WifiConfiguration wifiConfig = new WifiConfiguration ( );
            wifiConfig.SSID = "\"" + SSID + "\"";
            wifiConfig.preSharedKey = "\"" + PASSWORD + "\"";
            wifiConfig.allowedKeyManagement.set ( WifiConfiguration.KeyMgmt.WPA_PSK );

            wifiManager = ( WifiManager ) context.getApplicationContext ( ).getSystemService ( Context.WIFI_SERVICE );

            int networkId = wifiManager.addNetwork ( wifiConfig );

            if ( networkId != -1 ) {
                wifiManager.enableNetwork ( networkId , true );
                wifiManager.reconnect ( );
            }
        }


    }

    private boolean checkWifiConnection ( ) {
        wifiManager = ( WifiManager ) context.getApplicationContext ( ).getSystemService ( Context.WIFI_SERVICE );
        WifiInfo wifiInfo = wifiManager.getConnectionInfo ( );

        if ( wifiInfo != null && wifiInfo.getNetworkId ( ) != -1 ) {
            String currentSSID = wifiInfo.getSSID ( ).replace ( "\"" , "" );
            if ( currentSSID.equals ( SSID ) ) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isWifiEnabled ( ) {
        wifiManager = ( WifiManager ) context.getApplicationContext ( ).getSystemService ( Context.WIFI_SERVICE );
        return wifiManager.isWifiEnabled ( );
    }

    private void connectToWifi2 ( ) {
        new Thread ( ( ) -> {
            if ( isWifiEnabled ( ) ) {
                while (!checkWifiConnection ( )) {
                    connectToWifi ( );
                    try {
                        Thread.sleep ( 1000 );
                    } catch ( InterruptedException e ) {
                        e.printStackTrace ( );
                    }
                }
            }
        } ).start ( );

    }

    private void enableWifi ( ) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            Intent intent = new Intent ( Settings.Panel.ACTION_WIFI );
            startActivity ( intent );
        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ) {
            wifiManager.setWifiEnabled ( true );
        }

    }

    public void startCamera ( int cameraFacing ) {
        int aspectRatio = aspectRatio ( previewView.getWidth ( ) , previewView.getHeight ( ) );
        ListenableFuture < ProcessCameraProvider > listenableFuture = ProcessCameraProvider.getInstance ( this );

        listenableFuture.addListener ( ( ) -> {
            try {
                ProcessCameraProvider cameraProvider = listenableFuture.get ( );

                Preview preview = new Preview.Builder ( ).setTargetAspectRatio ( aspectRatio ).build ( );

                ImageCapture imageCapture = new ImageCapture.Builder ( )
                        .setCaptureMode ( ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY )
                        .setTargetRotation ( getWindowManager ( ).getDefaultDisplay ( ).getRotation ( ) ).build ( );

                CameraSelector cameraSelector = new CameraSelector.Builder ( )
                        .requireLensFacing ( cameraFacing ).build ( );

                cameraProvider.unbindAll ( );

                Camera camera = cameraProvider.bindToLifecycle ( this , cameraSelector , preview , imageCapture );

                capture.setOnClickListener ( view -> {
                    if ( ContextCompat.checkSelfPermission ( MainActivity.this , Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED ) {
                        activityResultLauncher.launch ( Manifest.permission.WRITE_EXTERNAL_STORAGE );
                    }
                    takePicture ( imageCapture );
                } );

                toggleFlash.setOnClickListener ( view -> setFlashIcon ( camera ) );

                preview.setSurfaceProvider ( previewView.getSurfaceProvider ( ) );
            } catch ( ExecutionException | InterruptedException e ) {
                e.printStackTrace ( );
            }
        } , ContextCompat.getMainExecutor ( this ) );
    }


    public void takePicture ( @NonNull ImageCapture imageCapture ) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            final File file = new File ( getExternalFilesDir ( Environment.DIRECTORY_PICTURES ) , "image" + System.currentTimeMillis ( ) + ".jpeg" );

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder ( file ).build ( );

            imageCapture.takePicture ( outputFileOptions , Executors.newCachedThreadPool ( ) , new ImageCapture.OnImageSavedCallback ( ) {
                @Override
                public void onImageSaved ( @NonNull ImageCapture.OutputFileResults outputFileResults ) {
                    ContentValues values = new ContentValues ( );
                    values.put ( MediaStore.Images.Media.DISPLAY_NAME , file.getName ( ) );
                    values.put ( MediaStore.Images.Media.MIME_TYPE , "image/jpeg" );
                    values.put ( MediaStore.Images.Media.DATA , file.getAbsolutePath ( ) );
                    values.put ( MediaStore.Images.Media.IS_PENDING , 1 );

                    new Thread ( ( ) -> {

                        MediaActionSound mediaActionSound = new MediaActionSound ( );
                        mediaActionSound.play ( MediaActionSound.SHUTTER_CLICK );

                    } ).start ( );

                    ContentResolver resolver = getContentResolver ( );
                    Uri imageUri = resolver.insert ( MediaStore.Images.Media.EXTERNAL_CONTENT_URI , values );

                    try {
                        if ( imageUri != null ) {
                            OutputStream outputStream = resolver.openOutputStream ( imageUri );
                            Bitmap bitmap = BitmapFactory.decodeFile ( file.getAbsolutePath ( ) );
                            if ( cameraFacing == CameraSelector.LENS_FACING_BACK ) {
                                bitmap = rotateBitmap ( bitmap , -270 );
                            } else {
                                bitmap = rotateBitmap ( bitmap , 270 );
                            }
                            bitmap.compress ( Bitmap.CompressFormat.JPEG , 100 , outputStream );
                            UDP udp = new UDP (context );
                            udp.execute ( );
                            outputStream.close ( );
                        }
                    } catch ( Exception e ) {
                        e.printStackTrace ( );
                    } finally {
                        values.put ( MediaStore.Images.Media.IS_PENDING , 0 );
                        if ( imageUri != null ) {
                            resolver.update ( imageUri , values , null , null );
                        }
                    }

                    runOnUiThread ( ( ) -> {
                        Toast.makeText ( MainActivity.this , "Immagine salvata in: " + file.getPath ( ) , Toast.LENGTH_SHORT ).show ( );
                        System.out.println ( file.getPath ( ) );
                    } );
                    startCamera ( cameraFacing );
                }

                @Override
                public void onError ( @NonNull ImageCaptureException exception ) {
                    runOnUiThread ( ( ) -> Toast.makeText ( MainActivity.this , "Fallito: " + exception.getMessage ( ) , Toast.LENGTH_SHORT ).show ( ) );
                    startCamera ( cameraFacing );
                }
            } );
        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ) {
            final File file = new File ( getExternalFilesDir ( Environment.DIRECTORY_PICTURES ) , "image" + System.currentTimeMillis ( ) + ".jpeg" );

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder ( file ).build ( );

            imageCapture.takePicture ( outputFileOptions , Executors.newCachedThreadPool ( ) , new ImageCapture.OnImageSavedCallback ( ) {
                @Override
                public void onImageSaved ( @NonNull ImageCapture.OutputFileResults outputFileResults ) {
                    try {
                        String filePath = file.getAbsolutePath ( );


                        Bitmap bitmap = BitmapFactory.decodeFile ( filePath );

                        try (OutputStream outputStream = new FileOutputStream ( filePath )) {
                            bitmap.compress ( Bitmap.CompressFormat.JPEG , 100 , outputStream );
                            new Thread ( ( ) -> {

                                MediaActionSound mediaActionSound = new MediaActionSound ( );
                                mediaActionSound.play ( MediaActionSound.SHUTTER_CLICK );

                            } ).start ( );
                            UDP udp = new UDP ( context);
                            udp.execute ( );
                            outputStream.flush ( );
                        } catch ( IOException e ) {
                            e.printStackTrace ( );
                        }

                        runOnUiThread ( ( ) -> {
                            Toast.makeText ( MainActivity.this , "Immagine salvata in: " + filePath , Toast.LENGTH_SHORT ).show ( );
                            System.out.println ( filePath );
                            startCamera ( cameraFacing );
                        } );


                    } catch ( Exception e ) {
                        e.printStackTrace ( );
                    }
                }

                @Override
                public void onError ( @NonNull ImageCaptureException exception ) {
                    runOnUiThread ( ( ) -> Toast.makeText ( MainActivity.this , "Fallito: " + exception.getMessage ( ) , Toast.LENGTH_SHORT ).show ( ) );
                    startCamera ( cameraFacing );
                }
            } );
        }


    }

    private void setFlashIcon ( @NonNull Camera camera ) {
        if ( camera.getCameraInfo ( ).hasFlashUnit ( ) ) {
            if ( camera.getCameraInfo ( ).getTorchState ( ).getValue ( ) == 0 ) {
                camera.getCameraControl ( ).enableTorch ( true );
                toggleFlash.setImageResource ( R.drawable.baseline_flash_off_24 );
            } else {
                camera.getCameraControl ( ).enableTorch ( false );
                toggleFlash.setImageResource ( R.drawable.baseline_flash_on_24 );
            }
        } else {
            runOnUiThread ( ( ) -> Toast.makeText ( MainActivity.this , "Il flash non è disponibile" , Toast.LENGTH_SHORT ).show ( ) );
        }
    }


    private int aspectRatio ( int width , int height ) {
        double previewRatio = ( double ) Math.max ( width , height ) / Math.min ( width , height );
        if ( Math.abs ( previewRatio - 4.0 / 3.0 ) <= Math.abs ( previewRatio - 16.0 / 9.0 ) ) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }


    public Bitmap rotateBitmap ( Bitmap source , float angle ) {
        Matrix matrix = new Matrix ( );
        matrix.postRotate ( angle );

        return Bitmap.createBitmap ( source , 0 , 0 , source.getWidth ( ) , source.getHeight ( ) , matrix , true );
    }

}