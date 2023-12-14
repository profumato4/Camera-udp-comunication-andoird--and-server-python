package com.example.cameraxexample;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;

public class UDP extends AsyncTask<Void, Void, Void> {
    static Socket s; // Socket Variable

    @Override
    protected Void doInBackground(Void... params) {
        try {
            s = new Socket("192.168.1.15", 50001);

            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File[] files = directory.listFiles();

            if (files != null && files.length > 0) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return Long.compare(f2.lastModified(), f1.lastModified());
                    }
                });

                File photoPath = files[0];

                InputStream input = new FileInputStream(photoPath.getAbsolutePath());

                try {
                    try {
                        int bytesRead;
                        byte[] buffer = new byte[1024];
                        while ((bytesRead = input.read(buffer)) != -1) {
                            s.getOutputStream().write(buffer, 0, bytesRead);
                        }
                    } finally {
                        s.getOutputStream().flush();
                        s.close();
                    }
                } finally {
                    input.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
