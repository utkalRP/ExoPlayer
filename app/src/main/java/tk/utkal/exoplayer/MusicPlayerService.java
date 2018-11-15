package tk.utkal.exoplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
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
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.common.images.WebImage;

import java.util.ArrayList;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
import static com.google.android.gms.cast.framework.CastState.CONNECTED;

public class MusicPlayerService extends Service {

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    AudioManager audioManager;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    private SimpleExoPlayer player;
    private CastContext castContext;
    private CastPlayer castPlayer;
    PowerManager.WakeLock wakeLock;

    ArrayList<String> stationLanguages = new ArrayList<String>();
    ArrayList<String> stationGenres = new ArrayList<String>();
    String stationName, stationTag, stationUrl, stationLogo;

    public MusicPlayerService() {
        castContext = CastContext.getSharedInstance(this);
        castPlayer = new CastPlayer(castContext);
        castPlayer.setSessionAvailabilityListener(new CastPlayer.SessionAvailabilityListener() {
            @Override
            public void onCastSessionAvailable() {
                player.stop();
                setCurrentCastStation();
            }

            @Override
            public void onCastSessionUnavailable() {
                castPlayer.stop();
                //preparePlayer();
            }
        });
        Log.d("utkal","MusicPlayerService::constructor");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("utkal","MusicPlayerService::onBind");
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Acquire the wake-lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ExoPlayer::WakelockTag");
        wakeLock.acquire();

        Log.d("utkal","MusicPlayerService::onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("utkal","MusicPlayerService::onStartCommand");
        if (player == null) {
            TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);

            player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), new DefaultTrackSelector(adaptiveTrackSelectionFactory), new DefaultLoadControl());

            audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if(player == null)
                        return;
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        // Permanent loss of audio focus
                        player.stop();
                    } else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
                        // Pause playback
                        player.setPlayWhenReady(false);
                    } else // Lower the volume, keep playing
                        if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                            player.setVolume((float) 0.5);
                        } else if (focusChange == audioManager.AUDIOFOCUS_GAIN) {
                            // Your app has been granted audio focus again
                            // Raise volume to normal, restart playback if necessary
                            player.setPlayWhenReady(true);
                        }
                }
            };

            int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, // Music streaming
                    AudioManager.AUDIOFOCUS_GAIN); // Permanent focus
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Play the audio
                player.setPlayWhenReady(true);
            }
        }

        stationName = intent.getStringExtra("name");
        stationTag = intent.getStringExtra("tag");
        stationLogo = intent.getStringExtra("logo");
        stationUrl = intent.getStringExtra("url");
        stationGenres.addAll(intent.getStringArrayListExtra("genre"));
        stationLanguages.addAll(intent.getStringArrayListExtra("lang"));

        if (castContext.getCastState() == CONNECTED) {
            setCurrentCastStation();
        }
        else {
            preparePlayer();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        if (castPlayer != null) {
            castPlayer.release();
        }

        wakeLock.release();
    }

    private MediaSource buildMediaSource(Uri uri) {

        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER);

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

        String genre = "";
        for (int i = 0; i < stationGenres.size(); i++) {
            String temp = stationGenres.get(i);
            if (genre.isEmpty())
                genre += temp;
            else
                genre += " | " + temp;
        }

        for (int i = 0; i < stationLanguages.size(); i++) {
            String temp = stationLanguages.get(i);
            if (genre.isEmpty())
                genre += temp;
            else
                genre += " | " + temp;
        }

        stationMetadata.putString(MediaMetadata.KEY_ARTIST, genre);
        stationMetadata.addImage(new WebImage(Uri.parse(stationLogo)));

        MediaInfo mediaInfo = new MediaInfo.Builder(stationUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(MimeTypes.AUDIO_UNKNOWN)
                .setMetadata(stationMetadata).build();

        final MediaQueueItem[] mediaItems = {new MediaQueueItem.Builder(mediaInfo).build()};
        castPlayer.setPlayWhenReady(true);
        castPlayer.loadItems(mediaItems, 0, 0, Player.REPEAT_MODE_OFF);
    }
}
