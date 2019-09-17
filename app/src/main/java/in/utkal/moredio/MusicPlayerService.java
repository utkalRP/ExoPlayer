package in.utkal.moredio;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.common.images.WebImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.google.android.gms.cast.framework.CastState.CONNECTED;
import static in.utkal.moredio.App.CHANNEL_ID;

public class MusicPlayerService extends Service {

    private SimpleExoPlayer player;
    private CastContext castContext;
    private CastPlayer castPlayer;
    PowerManager.WakeLock wakeLock;

    String stationName, stationTag, stationUrl, stationLogo, stationGenreLang;
    NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        castContext = CastContext.getSharedInstance(this);
        castPlayer = new CastPlayer(castContext);
        castPlayer.setSessionAvailabilityListener(new SessionAvailabilityListener() {
            @Override
            public void onCastSessionAvailable() {
                player.stop();
                setCurrentCastStation();
            }

            @Override
            public void onCastSessionUnavailable() {
                castPlayer.stop();
                preparePlayer();
            }
        });
        
        //Acquire the wake-lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ExoPlayer::WakelockTag");
        wakeLock.acquire();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationBuilder.setContentTitle(getString(R.string.app_tag));
        notificationBuilder.setSmallIcon(R.drawable.ic_radio);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setContentIntent(pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(this);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build();
            player.setAudioAttributes(audioAttributes, true);
        }

        stationName = intent.getStringExtra("name");
        stationTag = intent.getStringExtra("tag");
        stationLogo = intent.getStringExtra("logo");
        stationUrl = intent.getStringExtra("url");
        stationGenreLang = intent.getStringExtra("genre_lang");

        notificationBuilder.setContentText(stationName);
        startForeground(1, notificationBuilder.build());

        if (castContext.getCastState() == CONNECTED) {
            setCurrentCastStation();
        }
        else {
            preparePlayer();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        if (castPlayer != null) {
            castPlayer.release();
        }

        wakeLock.release();
        stopSelf();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("utkal","MusicPlayerService::onBind");
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    private MediaSource buildMediaSource(Uri uri) {

        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("ua");
        int type = Util.inferContentType(uri);

        switch (type) {
            case C.TYPE_SS:
                SsChunkSource.Factory ssChunkSourceFactory = new DefaultSsChunkSource.Factory(dataSourceFactory);
                return new SsMediaSource.Factory(ssChunkSourceFactory, dataSourceFactory).createMediaSource(uri);

            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);

            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);

            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private void preparePlayer() {

        Uri uri = Uri.parse(stationUrl);

        MediaSource mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource, true, false);
        player.setPlayWhenReady(true);
    }

    private void setCurrentCastStation() {
        MediaMetadata stationMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        stationMetadata.putString(MediaMetadata.KEY_TITLE, stationName);
        stationMetadata.putString(MediaMetadata.KEY_SUBTITLE, stationTag);
        stationMetadata.putString(MediaMetadata.KEY_ARTIST, stationGenreLang);
        stationMetadata.addImage(new WebImage(Uri.parse(stationLogo)));

        MediaInfo mediaInfo = new MediaInfo.Builder(stationUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(MimeTypes.AUDIO_UNKNOWN)
                .setMetadata(stationMetadata).build();

        final MediaQueueItem[] mediaItems = {new MediaQueueItem.Builder(mediaInfo).build()};
        castPlayer.setPlayWhenReady(true);
        castPlayer.loadItems(mediaItems, 0, 0, Player.REPEAT_MODE_OFF);
    }

    private String getCurrentTrack(String stationId) {
        final String[] curTrack = {""};

        Map<String, String> params = new HashMap();
        params.put("stationId", stationId);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                "https://directory.shoutcast.com/Player/GetCurrentTrack",
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject station = (JSONObject) response.get("Station");
                            curTrack[0] = station.getString("CurrentTrack");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }}, null);

        return curTrack[0];
    }
}
