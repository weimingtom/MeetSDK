package com.pplive.meetplayer.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Bundle;
import android.util.Log;

public class LogReportHandler {

    public static String post(String url, Bundle param, File file) {
        try {
            return conn(url, param, file);
        } catch (IOException e) {
            Log.e(LogcatHelper.TAG_LOGCAT, e.getMessage(), e);
        }

        return "";
    }

    private static String conn(String url, Bundle param, File file)
            throws IOException {

        String BOUNDARY = AtvUtils.generateUUID();
        String PREFIX = "--", LINEND = "\r\n";
        String MULTIPART_FORM_DATA = "multipart/form-data";
        String CHARSET = "UTF-8";
        HttpURLConnection conn = (HttpURLConnection) new URL(url)
                .openConnection();
        conn.setReadTimeout(5 * 1000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("connection", "keep-alive");
        conn.setRequestProperty("Charsert", CHARSET);
        conn.setRequestProperty("Content-Type", MULTIPART_FORM_DATA
                + ";boundary=" + BOUNDARY);

        StringBuilder sb = new StringBuilder();
        for (String key : param.keySet()) {
            sb.append(PREFIX);
            sb.append(BOUNDARY);
            sb.append(LINEND);
            sb.append("Content-Disposition: form-data; name=\"" + key + "\""
                    + LINEND);
            sb.append("Content-Type: text/plain; charset=" + CHARSET + LINEND);
            sb.append("Content-Transfer-Encoding: 8bit" + LINEND);
            sb.append(LINEND);
            sb.append(param.getString(key));
            sb.append(LINEND);
        }

        DataOutputStream outStream = new DataOutputStream(
                conn.getOutputStream());
        outStream.write(sb.toString().getBytes());

        File zipFile = file;

        StringBuilder sb1 = new StringBuilder();
        sb1.append(PREFIX);
        sb1.append(BOUNDARY);
        sb1.append(LINEND);
        sb1.append("Content-Disposition: form-data; name=\"annex\"; filename=\""
        		+ zipFile.getName() + "\"" + LINEND);
        sb1.append("Content-Type: application/x-gzip" + LINEND);
        sb1.append(LINEND);
        outStream.write(sb1.toString().getBytes());

        FileInputStream is = new FileInputStream(zipFile);
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }

        is.close();
        outStream.write(LINEND.getBytes());
        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
        outStream.write(end_data);
        outStream.flush();

        InputStream in = conn.getInputStream();

        InputStreamReader isReader = new InputStreamReader(in);
        BufferedReader bufReader = new BufferedReader(isReader);
        String line = null;
        String data = "";

        while ((line = bufReader.readLine()) != null)
            data += line;

        outStream.close();
        conn.disconnect();
        zipFile.delete();
        return data;
    }
}
