/*
     This file is part of the Android app ch.bfh.securevote.
     (C) 2023 Benjamin Fehrensen (and other contributing authors)
     This library is free software; you can redistribute it and/or
     modify it under the terms of the GNU Lesser General Public
     License as published by the Free Software Foundation; either
     version 2.1 of the License, or (at your option) any later version.
     This library is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     Lesser General Public License for more details.
     You should have received a copy of the GNU Lesser General Public
     License along with this library; if not, write to the Free Software
     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package ch.bfh.securevote.utils;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkPostMessage implements Runnable {
    public interface ResultListener {
        void onResult(String result);
    }
    private ResultListener listener;
    private final String TAG = NetworkPostMessage.class.getName();
    protected String urlStr;
    protected byte[] postData;
    protected int timeout;

    public NetworkPostMessage(String url, byte[] postData, int timeout) {
        this.urlStr = url;
        this.postData = postData;
        this.timeout=timeout;
        this.listener = null;
    }

    /**
     * In case the sender is interested in the result ;-)
     * @param l (listener function)
     */
    public void setResultListener(ResultListener l){
        this.listener=l;
    }

    public void run(){
        String result="";
        HttpURLConnection connection;

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setDoOutput( true );
            connection.setInstanceFollowRedirects( false );
            connection.setRequestMethod( "POST" );
            connection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty( "Content-Length", Integer.toString( postData.length ));
            connection.setUseCaches( false );
            connection.connect();
            try( DataOutputStream wr = new DataOutputStream( connection.getOutputStream())) {
                wr.write( postData );
            }
            InputStream stream = connection.getInputStream();
            result=getReaderAsString(stream);
            if (listener != null)
                listener.onResult(result); //Pass information to listener ...
            return;

        } catch (IOException e) {
            Log.e(TAG,String.format("Failed to access internet: %s", e));
            e.printStackTrace();
        } catch (Exception ex){
            Log.e(TAG,String.format("Failed to get data '%s': %s", result, ex));
        }
        if (listener != null)
            listener.onResult(null);
    }

    public static String getReaderAsString(InputStream is) throws IOException {
        StringWriter sw = new StringWriter();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        char[] buf = new char[8192]; // buffer size
        int n;
        while ((n = in.read(buf)) > 0) {
            sw.write(buf, 0, n);
        }
        return sw.toString();
    }
}
