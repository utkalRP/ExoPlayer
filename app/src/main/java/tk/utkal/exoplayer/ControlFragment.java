package tk.utkal.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


public class ControlFragment extends Fragment {

    private ImageView ivCurrentStation;
    private TextView tvCurrentStation;
    private TextView tvCurrentTrack;
    private ImageButton ibPlayPause;

    private String url, curName, curTrack;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCurName(String curName) {
        this.curName = curName;
    }

    public void setCurTrack(String curTrack) {
        this.curTrack = curTrack;
    }

    public ControlFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        ivCurrentStation = (ImageView) view.findViewById(R.id.ivCurrentStation);
        tvCurrentStation = (TextView) view.findViewById(R.id.tvCurrentStation);
        tvCurrentTrack = (TextView) view.findViewById(R.id.tvCurrentTrack);
        ibPlayPause = (ImageButton) view.findViewById(R.id.ibPlayPause);
        ibPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (((MainActivity)getActivity()).isPlaying()) {
                    ((MainActivity) getActivity()).StopService();
                    ibPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    ((MainActivity)getActivity()).StartService();
                    ibPlayPause.setImageResource(R.drawable.ic_pause);
                }
            }
        });

        Picasso.get().load(url)
                .placeholder(R.drawable.radio)
                .into(ivCurrentStation);
        tvCurrentStation.setText(curName);
        tvCurrentTrack.setText(curTrack);

        if (((MainActivity)getActivity()).isPlaying()) {
            ibPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            ibPlayPause.setImageResource(R.drawable.ic_play);
        }

        return view;
    }
}
