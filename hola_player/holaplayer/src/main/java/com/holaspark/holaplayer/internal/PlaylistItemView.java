package com.holaspark.holaplayer.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.PlayItem;
import com.holaspark.holaplayer.PlayListItem;
import com.holaspark.holaplayer.R;

public class PlaylistItemView extends FrameLayout implements View.OnClickListener {
    private HolaPlayer m_hola_player;
    private TextView m_text;
    private NetworkImageView m_image;
    private PlayListItem m_playlist_item;
    private RequestQueue m_request_queue;
    private ImageLoader m_image_loader;

public PlaylistItemView(Context context, HolaPlayer hola_player, PlayListItem item) {
    super(context);
    m_hola_player = hola_player;
    m_request_queue = Volley.newRequestQueue(getContext());
    m_image_loader = new ImageLoader(m_request_queue, new ImageLoader.ImageCache() {
        private final LruCache<String, Bitmap> m_cache = new LruCache<>(10);
        public void putBitmap(String url, Bitmap bitmap) { m_cache.put(url, bitmap); }
        public Bitmap getBitmap(String url) { return m_cache.get(url); }
    });
    LayoutInflater.from(getContext()).inflate(R.layout.hola_playlist_item, this);
    m_image = findViewById(R.id.playlist_item_image);
    m_text = findViewById(R.id.playlist_item_text);
    m_playlist_item = item;
    m_text.setText(item.m_description);
    m_image.setImageUrl(item.m_poster_url, m_image_loader);
    setOnClickListener(this);
}

@Override
public void onClick(View v) {
    m_hola_player.queue(new PlayItem(null, m_playlist_item.m_video_url));
}
}
