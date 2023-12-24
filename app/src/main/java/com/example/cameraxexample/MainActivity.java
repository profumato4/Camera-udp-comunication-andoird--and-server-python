package com.example.cameraxexample;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
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
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String SSID = "TIM-29562191";
    private static final String PASSWORD = "rq7ngXZMQlc3NTVl1pZCNTlK";
    private final Context context = MainActivity.this;
    private WifiManager wifiManager;

    ImageButton capture, toggleFlash, flipCamera;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private final ActivityResultLauncher < String > activityResultLauncher = registerForActivityResult ( new ActivityResultContracts.RequestPermission ( ) , new ActivityResultCallback < Boolean > ( ) {
        @Override
        public void onActivityResult ( Boolean result ) {
            if ( result ) {
                startCamera ( cameraFacing );
            }
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

        if ( ContextCompat.checkSelfPermission ( MainActivity.this , Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
            activityResultLauncher.launch ( Manifest.permission.CAMERA );
        } else {
            startCamera ( cameraFacing );
            connectToWifi2 ( );
        }
        flipCamera.setOnClickListener ( new View.OnClickListener ( ) {
            @Override
            public void onClick ( View view ) {
                if ( cameraFacing == CameraSelector.LENS_FACING_BACK ) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera ( cameraFacing );
            }
        } );
    }


    private void connectToWifi ( ) {
        final WifiNetworkSuggestion wifi =
                new WifiNetworkSuggestion.Builder ( )
                        .setSsid ( SSID )
                        .setWpa2Passphrase ( PASSWORD )
                        .build ( );

        wifiManager = ( WifiManager ) context.getSystemService ( Context.WIFI_SERVICE );
        wifiManager.addNetworkSuggestions ( Collections.singletonList ( wifi ) );

    }

    private boolean checkWifiConnection ( ) {
        wifiManager = ( WifiManager ) context.getSystemService ( Context.WIFI_SERVICE );
        WifiInfo wifiInfo = wifiManager.getConnectionInfo ( );

        if ( wifiInfo != null && wifiInfo.getNetworkId ( ) != -1 ) {
            String currentSSID = wifiInfo.getSSID ( ).replace ( "\"" , "" );
            if ( currentSSID.equals ( SSID ) ) {
                checkResult ( true );
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isWifiEnabled ( ) {
        wifiManager = ( WifiManager ) context.getSystemService ( Context.WIFI_SERVICE );
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
            } else {
                runOnUiThread ( ( ) -> {
                    Intent intent = new Intent ( Settings.Panel.ACTION_WIFI );
                    startActivity ( intent );
                } );
            }


        } ).start ( );
    }


    /*
        public void sendPhotoViaUDP(String ipAddress, int port, Bitmap photo) {
            AsyncTask.execute(() -> {
                try {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    DatagramSocket socket = new DatagramSocket();
                    InetAddress serverAddr = InetAddress.getByName(ipAddress);
                    DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, serverAddr, port);

                    socket.send(packet);
                    socket.close();

                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Foto inviata con successo via UDP", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Errore nell'invio della foto via UDP", Toast.LENGTH_SHORT).show();
                        Log.e("UDP", "Exception during UDP photo transmission", e);
                    });
                }
            });
        }


    */

    public void startCamera ( int cameraFacing ) {
        int aspectRatio = aspectRatio ( previewView.getWidth ( ) , previewView.getHeight ( ) );
        ListenableFuture < ProcessCameraProvider > listenableFuture = ProcessCameraProvider.getInstance ( this );

        listenableFuture.addListener ( ( ) -> {
            try {
                ProcessCameraProvider cameraProvider = ( ProcessCameraProvider ) listenableFuture.get ( );

                Preview preview = new Preview.Builder ( ).setTargetAspectRatio ( aspectRatio ).build ( );

                ImageCapture imageCapture = new ImageCapture.Builder ( )
                        .setCaptureMode ( ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY )
                        .setTargetRotation ( getWindowManager ( ).getDefaultDisplay ( ).getRotation ( ) ).build ( );

                CameraSelector cameraSelector = new CameraSelector.Builder ( )
                        .requireLensFacing ( cameraFacing ).build ( );

                cameraProvider.unbindAll ( );

                Camera camera = cameraProvider.bindToLifecycle ( this , cameraSelector , preview , imageCapture );

                capture.setOnClickListener ( new View.OnClickListener ( ) {
                    @Override
                    public void onClick ( View view ) {
                        if ( ContextCompat.checkSelfPermission ( MainActivity.this , Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED ) {
                            activityResultLauncher.launch ( Manifest.permission.WRITE_EXTERNAL_STORAGE );
                        }
                        takePicture ( imageCapture );
                    }
                } );

                toggleFlash.setOnClickListener ( new View.OnClickListener ( ) {
                    @Override
                    public void onClick ( View view ) {
                        setFlashIcon ( camera );
                    }
                } );

                preview.setSurfaceProvider ( previewView.getSurfaceProvider ( ) );
            } catch ( ExecutionException | InterruptedException e ) {
                e.printStackTrace ( );
            }
        } , ContextCompat.getMainExecutor ( this ) );
    }


    public void takePicture ( @NonNull ImageCapture imageCapture ) {
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

                ContentResolver resolver = getContentResolver ( );
                Uri imageUri = resolver.insert ( MediaStore.Images.Media.EXTERNAL_CONTENT_URI , values );

                try {
                    if ( imageUri != null ) {
                        OutputStream outputStream = resolver.openOutputStream ( imageUri );
                        Bitmap bitmap = BitmapFactory.decodeFile ( file.getAbsolutePath ( ) );
                        bitmap.compress ( Bitmap.CompressFormat.JPEG , 100 , outputStream );
                        UDP udp = new UDP ( );
                        udp.execute ( );
                        outputStream.close ( );
                    }
                } catch ( Exception e ) {
                    e.printStackTrace ( );
                } finally {
                    values.put ( MediaStore.Images.Media.IS_PENDING , 0 );
                    resolver.update ( imageUri , values , null , null );
                }

                runOnUiThread ( new Runnable ( ) {
                    @Override
                    public void run ( ) {
                        Toast.makeText ( MainActivity.this , "Immagine salvata in: " + file.getPath ( ) , Toast.LENGTH_SHORT ).show ( );
                        System.out.println ( file.getPath ( ) );
                    }
                } );
                startCamera ( cameraFacing );
            }

            @Override
            public void onError ( @NonNull ImageCaptureException exception ) {
                runOnUiThread ( new Runnable ( ) {
                    @Override
                    public void run ( ) {
                        Toast.makeText ( MainActivity.this , "Fallito: " + exception.getMessage ( ) , Toast.LENGTH_SHORT ).show ( );
                    }
                } );
                startCamera ( cameraFacing );
            }
        } );
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
            runOnUiThread ( new Runnable ( ) {
                @Override
                public void run ( ) {
                    Toast.makeText ( MainActivity.this , "Il flash non è disponibile" , Toast.LENGTH_SHORT ).show ( );
                }
            } );
        }
    }

    private int aspectRatio ( int width , int height ) {
        double previewRatio = ( double ) Math.max ( width , height ) / Math.min ( width , height );
        if ( Math.abs ( previewRatio - 4.0 / 3.0 ) <= Math.abs ( previewRatio - 16.0 / 9.0 ) ) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private void checkResult ( boolean isSuccess ) {
        if ( isSuccess ) {
            runOnUiThread ( new Runnable ( ) {
                @Override
                public void run ( ) {
                    Toast.makeText ( MainActivity.this , "Connessione stabilita" , Toast.LENGTH_SHORT ).show ( );
                }
            } );
        } else {
            runOnUiThread ( new Runnable ( ) {
                @Override
                public void run ( ) {
                    Toast.makeText ( MainActivity.this , "Connessione non stabilita" , Toast.LENGTH_SHORT ).show ( );
                }
            } );
        }
    }
}