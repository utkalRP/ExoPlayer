package tk.utkal.exoplayer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ListViewAdaptor listViewAdaptor;
    ProgressDialog mProgressDialog;
    int nCurrentStationId;
    private ArrayList<RadioStation> radioStations = new ArrayList<RadioStation>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle("");
        mProgressDialog.setMessage("Please wait ...");
        mProgressDialog.show();

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, getString(R.string.json_url), null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                ArrayList<String> languages = new ArrayList<String>();
                ArrayList<String> genres = new ArrayList<String>();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject channel = (JSONObject) response.get(i);

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

                        RadioStation station = new RadioStation(
                                (String) channel.get("name"),
                                (String) channel.get("tag"),
                                (String) channel.get("low"),
                                (String) channel.get("high"),
                                (String) channel.get("thumb"),
                                (String) channel.get("web"),
                                languages, genres);

                        radioStations.add(station);
                    }

                    //Set the listview adaptor
                    listView.setAdapter(listViewAdaptor);

                    //Dismiss the progress dialog
                    if (mProgressDialog != null && mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
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

        //Add the Volley request to queue
        queue.add(jsonArrayRequest);

        //Prepare the listview
        listView = (ListView) findViewById(R.id.listView);
        listViewAdaptor = new ListViewAdaptor();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                nCurrentStationId = i;
                StartService();
            }
        });
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

    private void StartService() {
        Intent intent = new Intent(MainActivity.this, MusicPlayerService.class);

        String url = radioStations.get(nCurrentStationId).getHighUrl();
        if (url.isEmpty())
            url = radioStations.get(nCurrentStationId).getLowUrl();

        String genre_lang = "";
        for (int i = 0; i < radioStations.get(nCurrentStationId).getGenres().size(); i++) {
            String temp = radioStations.get(nCurrentStationId).getGenres().get(i);
            if (genre_lang.isEmpty())
                genre_lang += temp;
            else
                genre_lang += " | " + temp;
        }

        for (int i = 0; i < radioStations.get(nCurrentStationId).getLanguages().size(); i++) {
            String temp = radioStations.get(nCurrentStationId).getLanguages().get(i);
            if (genre_lang.isEmpty())
                genre_lang += temp;
            else
                genre_lang += " | " + temp;
        }

        intent.putExtra("url", url);
        intent.putExtra("name", radioStations.get(nCurrentStationId).getName());
        intent.putExtra("tag", radioStations.get(nCurrentStationId).getTag());
        intent.putExtra("logo", radioStations.get(nCurrentStationId).getLogoUrl());
        intent.putExtra("genre_lang", genre_lang);

        startService(intent);
    }

    private void StopService() {
        Intent intent = new Intent(MainActivity.this, MusicPlayerService.class);
        stopService(intent);
    }


    @Override
    protected void onDestroy() {
        StopService();
        super.onDestroy();
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
            Picasso.get().load(thumbUrl)
                    .placeholder(R.drawable.radio)
                    .into(imageView);

            textViewTop.setText(radioStations.get(i).getName());

            String line2 = radioStations.get(i).getTag();
            for (int j = 0; j < radioStations.get(i).getLanguages().size(); j++) {
                String temp = radioStations.get(i).getLanguages().get(j);
                if (line2.isEmpty())
                    line2 += temp;
                else
                    line2 += " | " + temp;
            }

            for (int j = 0; j < radioStations.get(i).getGenres().size(); j++) {
                String temp = radioStations.get(i).getGenres().get(j);
                if (line2.isEmpty())
                    line2 += temp;
                else
                    line2 += " | " + temp;
            }

            textViewBottom.setText(line2);

            return view;
        }
    }
}
