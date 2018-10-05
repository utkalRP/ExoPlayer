package tk.utkal.exoplayer;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class FileDownloadAsync extends AsyncTask {
    private String strUrl;
    private String strData;

    public FileDownloadAsync(String url) {
        this.strUrl = url;
        this.strData = "";
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            URL url = new URL(strUrl);
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String strLine = bufferedReader.readLine();
            while (strLine != null) {
                strData += strLine;
                strLine = bufferedReader.readLine();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        try {
            JSONObject jsonObject = new JSONObject(strData);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
