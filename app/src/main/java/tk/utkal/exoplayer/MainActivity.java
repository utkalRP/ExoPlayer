package tk.utkal.exoplayer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FileDownloadCallback {

    public static int version = 1;
    public static ArrayList<RadioStation> radioStations = new ArrayList<RadioStation>();

    private SimpleExoPlayer player;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();


    int[] arrayImages = {R.drawable.sarthak_fm,
            R.drawable.radio_odisha,
            R.drawable.odia_radio,
            R.drawable.radio_chocolate,
            R.drawable.radio_mirchi,
            R.drawable.big_fm,
            R.drawable.red_fm,
            R.drawable.radio_city,
            R.drawable.radio,
            R.drawable.radio,
            R.drawable.radio,
            R.drawable.radio,
            R.drawable.radio,
            R.drawable.nuke_radio,
            R.drawable.bbc_hindi,
            R.drawable.air_vividh_bharti,
            R.drawable.air_logo,
            R.drawable.air_fm_gold};

    ListView listView;
    ListViewAdaptor     listViewAdaptor;
    PlayerBufferingTask mBufferingTask;
    ProgressDialog      mProgressDialog;

    int                 nCurrentStationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create and initialize the player
        createAndInitializePlayer();

        listView = (ListView) findViewById(R.id.listView);


        //Download the stations json from web
        FileDownloadAsync fileDownloadAsync = new FileDownloadAsync(getString(R.string.json_url),this);
        fileDownloadAsync.execute();
    }

    private void createAndInitializePlayer() {

        TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);

        player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this),
                new DefaultTrackSelector(adaptiveTrackSelectionFactory),
                new DefaultLoadControl());

        player.setPlayWhenReady(true);
    }

    private MediaSource buildMediaSource(Uri uri) {

        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER);

        int type = Util.inferContentType(uri);

        switch (type){
            case C.TYPE_DASH:
                DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory( dataSourceFactory);
                return new DashMediaSource.Factory(dashChunkSourceFactory, dataSourceFactory).createMediaSource(uri);

            case C.TYPE_SS:
                SsChunkSource.Factory ssChunkSourceFactory = new DefaultSsChunkSource.Factory(dataSourceFactory);
                return new SsMediaSource.Factory(ssChunkSourceFactory, dataSourceFactory).createMediaSource(uri);

            case C.TYPE_HLS:
                return new HlsMediaSource.Factory (dataSourceFactory).createMediaSource(uri);

            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory (dataSourceFactory).createMediaSource(uri);

            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private void preparePlayer(String url) {
        Uri uri = Uri.parse(url);

        MediaSource mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource, true, false);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        if(mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Exit")
                .setMessage("Do you want to switch off your radio?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void processData() {
        listViewAdaptor = new ListViewAdaptor();
        listView.setAdapter(listViewAdaptor);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);

                nCurrentStationId = i;

                mBufferingTask = new PlayerBufferingTask();
                mBufferingTask.execute(radioStations.get(i).getLowUrl());
            }
        });
    }


    class ListViewAdaptor extends BaseAdapter {

        @Override
        public int getCount() {
            return arrayImages.length;
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
            view = getLayoutInflater().inflate(R.layout.activity_list_item, null);
            ImageView imageView = (ImageView) view.findViewById(R.id.imageViewLogo);
            TextView textViewTop = (TextView) view.findViewById(R.id.textViewTop);
            TextView textViewBottom = (TextView) view.findViewById(R.id.textViewBottom);

            imageView.setImageResource(arrayImages[i]);
            textViewTop.setText(radioStations.get(i).getName());
            textViewBottom.setText(radioStations.get(i).getTag());

            return view;
        }
    }


    class PlayerBufferingTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setTitle("");
            mProgressDialog.setMessage(getString(R.string.app_tuning) + "\t" + radioStations.get(nCurrentStationId).getName() + "\nPlease wait ...");
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            player.stop(true);
            preparePlayer(strings[0]);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    }
}
