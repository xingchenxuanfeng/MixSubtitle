package com.xc.mixsubtitle;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by xc on 17-9-18.
 */

public class Web {

    private static final String TAG = "Web";

    public static String getTips(String urlString) {
        StringBuilder str = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            InputStream inputStream = httpURLConnection.getInputStream();
            byte[] buff = new byte[1024];
            while (inputStream.read(buff) > 0) {
                str.append(new String(buff));
            }
            inputStream.close();
            httpURLConnection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str.toString().trim();
    }
}
