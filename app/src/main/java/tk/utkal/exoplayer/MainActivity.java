package tk.utkal.exoplayer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.common.images.WebImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.google.android.gms.cast.framework.CastState.CONNECTED;

public class MainActivity extends AppCompatActivity {


    ListView listView;
    ListViewAdaptor listViewAdaptor;
    ProgressDialog mProgressDialog;
    int nCurrentStationId;
    private ArrayList<RadioStation> radioStations = new ArrayList<RadioStation>();

    private CastContext castContext;
    private CastPlayer castPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create and initialize the player
        createAndInitializeCastWidget();

        listView = (ListView) findViewById(R.id.listView);

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle("");
        mProgressDialog.setMessage("Please wait ...");
        mProgressDialog.show();

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, getString(R.string.json_url), null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                String name = "";
                String tag = "";
                String low = "";
                String high = "";
                String thumb = "";
                String web = "";
                ArrayList<String> languages = new ArrayList<String>();
                ArrayList<String> genres = new ArrayList<String>();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject channel = (JSONObject) response.get(i);
                        name = (String) channel.get("name");
                        tag = (String) channel.get("tag");
                        low = (String) channel.get("low");
                        high = (String) channel.get("high");
                        thumb = (String) channel.get("thumb");
                        web = (String) channel.get("web");

                        genres.clear();
                        languages.clear();

                        JSONArray jsonGenre = channel.getJSONArray("genre");
                        for (int j = 0; j < jsonGenre.length(); j++) {
                            genres.add(jsonGenre.getString(j));
                        }

                        JSONArray jsonLang = channel.getJSONArray("lang");
                        for (int j = 0; j < jsonLang.length(); j++) {
                            languages.add(jsonLang.getString(j));
                        }

                        RadioStation station = new RadioStation(name, tag, low, high, thumb, web, languages, genres);
                        radioStations.add(station);
                    }

                    if (mProgressDialog != null && mProgressDialog.isShowing())
                        mProgressDialog.dismiss();

                    processData();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error

            }
        });

        queue.add(jsonArrayRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.appbar_menu, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.menu_cast);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_cast:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createAndInitializeCastWidget() {

        castContext = CastContext.getSharedInstance(this);
        castPlayer = new CastPlayer(castContext);
        castPlayer.setSessionAvailabilityListener(new CastPlayer.SessionAvailabilityListener() {
            @Override
            public void onCastSessionAvailable() {
                StopService();
                setCurrentCastStation();
            }

            @Override
            public void onCastSessionUnavailable() {
                castPlayer.stop();
                StartService();
            }
        });
    }

    private void StartService() {
        Intent intent = new Intent(MainActivity.this, MusicPlayerService.class);

        String url = radioStations.get(nCurrentStationId).getHighUrl();
        if(url.isEmpty())
            url = radioStations.get(nCurrentStationId).getLowUrl();

        intent.putExtra("curStationId", url);
        startService(intent);
    }

    private void StopService() {
        Intent intent = new Intent(MainActivity.this, MusicPlayerService.class);
        stopService(intent);
    }

    private void setCurrentCastStation() {
        MediaMetadata stationMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        stationMetadata.putString(MediaMetadata.KEY_TITLE, radioStations.get(nCurrentStationId).getName());
        stationMetadata.putString(MediaMetadata.KEY_SUBTITLE, radioStations.get(nCurrentStationId).getTag());

        String genre = "";
        for (int i = 0; i < radioStations.get(nCurrentStationId).getGenres().size(); i++) {
            String temp = radioStations.get(nCurrentStationId).getGenres().get(i);
            if (genre.isEmpty())
                genre += temp;
            else
                genre += " | " + temp;
        }

        for (int i = 0; i < radioStations.get(nCurrentStationId).getLanguages().size(); i++) {
            String temp = radioStations.get(nCurrentStationId).getLanguages().get(i);
            if (genre.isEmpty())
                genre += temp;
            else
                genre += " | " + temp;
        }

        stationMetadata.putString(MediaMetadata.KEY_ARTIST, genre);
        stationMetadata.addImage(new WebImage(Uri.parse(radioStations.get(nCurrentStationId).getLogoUrl())));

        String url = radioStations.get(nCurrentStationId).getHighUrl();
        if(url.isEmpty())
            url = radioStations.get(nCurrentStationId).getLowUrl();

        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(MimeTypes.AUDIO_UNKNOWN)
                .setMetadata(stationMetadata).build();

        final MediaQueueItem[] mediaItems = {new MediaQueueItem.Builder(mediaInfo).build()};
        castPlayer.setPlayWhenReady(true);
        castPlayer.loadItems(mediaItems, 0, 0, Player.REPEAT_MODE_OFF);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StopService();
        if (castPlayer != null) {
            castPlayer.release();
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Exit")
                .setMessage("Do you want to switch off your radio?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StopService();
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void processData() {
        listViewAdaptor = new ListViewAdaptor();
        listView.setAdapter(listViewAdaptor);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                nCurrentStationId = i;

                if (castContext.getCastState() == CONNECTED) {
                    setCurrentCastStation();
                }
                else {
                    StartService();
                }
            }
        });
    }

    class ListViewAdaptor extends BaseAdapter {

        @Override
        public int getCount() {
            return radioStations.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Context c = viewGroup.getContext();

            view = getLayoutInflater().inflate(R.layout.activity_list_item, null);
            ImageView imageView = (ImageView) view.findViewById(R.id.imageViewLogo);
            TextView textViewTop = (TextView) view.findViewById(R.id.textViewTop);
            TextView textViewBottom = (TextView) view.findViewById(R.id.textViewBottom);

            String thumbUrl = radioStations.get(i).getLogoUrl();
            Glide.with(c)
                    .load(thumbUrl)
                    .fitCenter()
                    .placeholder(R.drawable.radio)
                    .crossFade()
                    .into(imageView);

            textViewTop.setText(radioStations.get(i).getName());

            String line2 = radioStations.get(i).getTag();
            for (int j = 0; j < radioStations.get(i).getLanguages().size(); j++) {
                String temp = radioStations.get(i).getLanguages().get(j);
                if(line2.isEmpty())
                    line2 += temp;
                else
                    line2 += " | " + temp;
            }

            for (int j = 0; j < radioStations.get(i).getGenres().size(); j++) {
                String temp = radioStations.get(i).getGenres().get(j);
                if(line2.isEmpty())
                    line2 += temp;
                else
                    line2 += " | " + temp;
            }

            textViewBottom.setText(line2);

            return view;
        }
    }
}
