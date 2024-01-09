package com.example.camera;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;


public class UDP extends AsyncTask < Void, Void, Void > {
    static Socket s; // Socket Variable
    private String path;
    private Context context;

    public UDP ( Context context ) {
        this.context = context;
    }

    @Override
    protected Void doInBackground ( Void... params ) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            try {
                s = new Socket ( "192.168.1.11" , 50001 );

                File directory = Environment.getExternalStoragePublicDirectory ( Environment.DIRECTORY_PICTURES );
                File[] files = directory.listFiles ( );

                if ( files != null && files.length > 0 ) {
                    Arrays.sort ( files , new Comparator < File > ( ) {
                        @Override
                        public int compare ( File f1 , File f2 ) {
                            return Long.compare ( f2.lastModified ( ) , f1.lastModified ( ) );
                        }
                    } );

                    File photoPath = files[0];

                    InputStream input = new FileInputStream ( photoPath.getAbsolutePath ( ) );

                    try {
                        try {
                            int bytesRead;
                            byte[] buffer = new byte[1024];
                            while ((bytesRead = input.read ( buffer )) != -1) {
                                s.getOutputStream ( ).write ( buffer , 0 , bytesRead );
                            }
                        } finally {
                            s.getOutputStream ( ).flush ( );
                            s.close ( );
                        }
                    } finally {
                        input.close ( );
                    }
                }

            } catch ( IOException e ) {
                e.printStackTrace ( );
            }
        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ) {

            try {
                s = new Socket ( "192.168.1.11" , 50001 );
            } catch ( IOException e ) {
                throw new RuntimeException ( e );
            }
            File directory = context.getExternalFilesDir ( Environment.DIRECTORY_PICTURES );
            File[] files = directory.listFiles ( );

            if ( files != null && files.length > 0 ) {
                Arrays.sort ( files , new Comparator < File > ( ) {
                    @Override
                    public int compare ( File f1 , File f2 ) {
                        return Long.compare ( f2.lastModified ( ) , f1.lastModified ( ) );
                    }
                } );

                File photoPath = files[0];
                try {
                    InputStream input = new FileInputStream ( photoPath.getAbsolutePath ( ) );
                    try {
                        try {
                            int bytesRead;
                            byte[] buffer = new byte[1024];
                            while ((bytesRead = input.read ( buffer )) != -1) {
                                s.getOutputStream ( ).write ( buffer , 0 , bytesRead );
                            }
                        } finally {
                            s.getOutputStream ( ).flush ( );
                            s.close ( );
                        }
                    } finally {
                        input.close ( );
                    }
                } catch ( IOException e ) {
                    throw new RuntimeException ( e );
                }

            }

        }
        return null;

    }

}
