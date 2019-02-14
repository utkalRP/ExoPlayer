package tk.utkal.exoplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class MyRvAdapter extends RecyclerView.Adapter<MyRvAdapter.ViewHolder> {
    private ArrayList<RadioStation> channels;
    private Context context;

    public MyRvAdapter(Context context, ArrayList<RadioStation> channels) {
        this.context = context;
        this.channels = channels;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.activity_list_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int index) {

        String slogan_genre_lang = channels.get(index).getTag();
        for(String str : channels.get(index).getGenres()){
            slogan_genre_lang = slogan_genre_lang.isEmpty() ? slogan_genre_lang + str : slogan_genre_lang + (" | " + str);
        }
        for(String str : channels.get(index).getLanguages()){
            slogan_genre_lang = slogan_genre_lang.isEmpty() ? slogan_genre_lang + str : slogan_genre_lang + (" | " + str);
        }

        viewHolder.tvChannelTitle.setText(channels.get(index).getName());
        viewHolder.tvChannelSlogan.setText(slogan_genre_lang);
        Picasso.get()
                .load(channels.get(index).getLogoUrl())
                .placeholder(R.drawable.radio)
                .into(viewHolder.ivChannelLogo);
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvChannelTitle;
        private TextView tvChannelSlogan;
        private ImageView ivChannelLogo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvChannelTitle = (TextView) itemView.findViewById(R.id.tvChannelTitle);
            tvChannelSlogan = (TextView) itemView.findViewById(R.id.tvChannelSubtitle);
            ivChannelLogo = (ImageView) itemView.findViewById(R.id.ivChanneLogo);
        }
    }

    public  void updateAdaptor(ArrayList<RadioStation> newArrayList) {
        channels = new ArrayList<RadioStation>();
        channels.addAll(newArrayList);
        notifyDataSetChanged();
    }
}
