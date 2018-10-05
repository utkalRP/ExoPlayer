package tk.utkal.exoplayer;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;



public class FileDownloadAsync extends AsyncTask {
    private String strUrl;
    private String strData;
    FileDownloadCallback callback;

    public FileDownloadAsync(String url, FileDownloadCallback cb) {
        this.strUrl = url;
        this.strData = "";
        this.callback = cb;
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

            MainActivity.version = (int) jsonObject.get("ver");

            String name = "", tag = "", low = "", high = "", thumb = "", web = "";
            ArrayList<String> langs = new ArrayList<String>(), genres = new ArrayList<String>();

            JSONArray jsonArray = jsonObject.getJSONArray("channels");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject channel = (JSONObject) jsonArray.get(i);
                name = (String) channel.get("name");
                tag = (String) channel.get("tag");
                low = (String) channel.get("low");
                high = (String) channel.get("high");
                thumb = (String) channel.get("thumb");
                web = (String) channel.get("web");

                JSONArray jsonGenre = channel.getJSONArray("genre");
                for(int j = 0; j < jsonGenre.length(); j++) {
                    genres.add(jsonGenre.getString(j));
                }

                JSONArray jsonLang = channel.getJSONArray("lang");
                for(int j = 0; j < jsonLang.length(); j++) {
                    langs.add(jsonLang.getString(j));
                }

                RadioStation station = new RadioStation(name, tag, low, high, thumb, web, genres, langs);
                MainActivity.radioStations.add(station);
            }

            callback.processData();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
