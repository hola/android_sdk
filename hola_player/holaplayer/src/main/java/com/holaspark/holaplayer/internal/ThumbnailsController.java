package com.holaspark.holaplayer.internal;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.TimeBar;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.HolaPlayerConfig;
import com.holaspark.holaplayer.PlayItem;
import com.holaspark.holaplayer.R;
import com.holaspark.holaplayer.ThumbnailsConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ThumbnailsController extends Player.DefaultEventListener implements
    TimeBar.OnScrubListener
{
private ThumbnailsConfig m_conf;
private View m_thumb_holder;
private View m_ff_thumb_holder;
private View m_middle_buttons_holder;
private ImageView m_image;
private ImageView m_ff_image;
private DefaultTimeBar m_timebar;
private Player m_player;
private HolaPlayer m_hola_player;
private PlayItem m_play_item;
private int m_cur_image_index = -1;
private int m_cur_sprite_index = -1;
private RequestQueue m_request_queue;
private HolaPlayerConfig m_player_conf;
private boolean m_was_playing;
private boolean m_ff_thumb_active;
private ThumbImageRequest m_img_request;
private Bitmap m_cur_image;
public ThumbnailsController(Player player, HolaPlayer hola_player){
    m_player = player;
    m_hola_player = hola_player;
    m_timebar = hola_player.findViewById(com.google.android.exoplayer2.ui.R.id.exo_progress);
    m_thumb_holder = hola_player.findViewById(R.id.thumb_holder);
    m_image = hola_player.findViewById(R.id.thumb_image);
    m_ff_thumb_holder = hola_player.findViewById(R.id.ff_thumb_holder);
    m_ff_image = hola_player.findViewById(R.id.ff_thumb_image);
    m_middle_buttons_holder = hola_player.findViewById(R.id.middle_buttons_holder);
    m_player_conf = hola_player.get_config();
    m_timebar.addListener(this);
    m_player.addListener(this);
    m_request_queue = Volley.newRequestQueue(m_thumb_holder.getContext());
    load_conf();
}
public void set_play_item(PlayItem item){
    m_play_item = item;
    load_conf();
}
private void load_conf(){
    String customer = m_player_conf.m_customer;
    if (customer == null || m_play_item == null)
        return;
    set_conf(null);
    // XXX andrey: use hola_cdn api
    final String url = new Uri.Builder().scheme("http")
        .authority("holaspark-demo.h-cdn.com") // XXX andrey: use real customer id
        .path("api/get_thumb_info").appendQueryParameter("customer", customer)
        .appendQueryParameter("url", m_play_item.get_media()).build()
        .toString();
    JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
        new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response){
                JSONObject info = response.optJSONObject("info");
                if (info == null)
                {
                    Log.d(Const.TAG, "no thumbnails info, status="+
                        response.optString("status")+" url="+url);
                    return;
                }
                try
                {
                    JSONArray json_cdns = response.getJSONArray("cdns");
                    String[] cdns = new String[json_cdns.length()];
                    for (int i = 0; i<json_cdns.length(); i++)
                        cdns[i] = json_cdns.getJSONObject(i).getString("host");
                    JSONArray json_urls = response.getJSONArray("urls");
                    String[] urls = new String[json_urls.length()];
                    for (int i = 0; i<json_urls.length(); i++)
                        urls[i] = json_urls.getString(i);
                    set_conf(new ThumbnailsConfig(info.getInt("width"),
                        info.getInt("height"), info.getInt("group_size"),
                        info.getInt("interval")*1000, cdns, urls));
                } catch(Throwable e)
                {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error){
            VolleyLog.e(Const.TAG+" Error: "+error.getMessage());
        }
    }){
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError{
            Map<String, String> params = new HashMap<>();
            params.put("Referer", "http://player.h-cdn.com/webview?customer=demo");
            return params;
        }
    };
    m_request_queue.add(req);
}
private void set_conf(ThumbnailsConfig conf){
    m_conf = conf;
    if (conf == null)
        return;
    DisplayMetrics dm = new DisplayMetrics();
    Activity activity = (Activity) m_thumb_holder.getContext();
    activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
    m_image.setLayoutParams(new FrameLayout.LayoutParams((int) (m_conf.m_width*dm.density),
        (int) (m_conf.m_height*dm.density)));
}
private void load_image(long pos){
    int thumb_index = (int) pos/m_conf.m_interval;
    set_image(thumb_index/m_conf.m_group_size);
    set_sprite(thumb_index%m_conf.m_group_size);
}
private void set_image(int image_index){
    if (image_index == m_cur_image_index)
        return;
    m_cur_image = null;
    m_cur_image_index = image_index;
    if (m_img_request != null)
        m_img_request.cancel();
    m_img_request = new ThumbImageRequest(m_cur_image_index);
}
private void set_sprite(int sprite_index){
    if (m_cur_sprite_index == sprite_index)
        return;
    m_cur_sprite_index = sprite_index;
    update_image();
}
private void update_image(){
    if (m_cur_image == null)
        return;
    int n = (int) Math.sqrt(m_conf.m_group_size);
    int y = m_cur_sprite_index/n;
    int x = m_cur_sprite_index%n;
    Bitmap sprite = Bitmap
        .createBitmap(m_cur_image, x*m_conf.m_width, y*m_conf.m_height, m_conf.m_width, m_conf.m_height);
    m_image.setImageBitmap(sprite);
    if (m_player_conf.m_full_frame_thumbnails)
        m_ff_image.setImageBitmap(sprite);
}
@Override
public void onScrubStart(TimeBar timebar, long position){
    if (m_player == null || m_conf == null)
        return;
    m_thumb_holder.setVisibility(VISIBLE);
    if (m_player_conf.m_full_frame_thumbnails)
    {
        if (m_was_playing = m_player.getPlayWhenReady())
            m_hola_player.pause();
        m_ff_thumb_holder.setVisibility(VISIBLE);
        m_middle_buttons_holder.setVisibility(GONE);
        m_ff_thumb_active = true;
    }
}
@Override
public void onScrubMove(TimeBar timebar, long position){
    if (m_player == null || m_conf == null)
        return;
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    double percent = (double) position/m_player.getDuration();
    int width = m_thumb_holder.getWidth();
    int left = m_timebar.getLeft();
    p.leftMargin =
        Math.min(Math.max((int) (percent*m_timebar.getWidth())+left-width/2, 0),
            ((View) m_thumb_holder.getParent()).getWidth()-width);
    m_thumb_holder.setLayoutParams(p);
    load_image(position);
}
@Override
public void onScrubStop(TimeBar timebar, long position, boolean canceled){
    if (m_player == null || m_conf == null)
        return;
    m_thumb_holder.setVisibility(GONE);
}
@Override
public void onPlayerStateChanged(boolean playWhenReady, int state){
    if (!m_ff_thumb_active || state != Player.STATE_READY)
        return;
    if (m_was_playing)
        m_hola_player.play();
    m_ff_thumb_holder.setVisibility(GONE);
    m_middle_buttons_holder.setVisibility(VISIBLE);
    m_ff_thumb_active = false;
}
class CdnImageRequest extends ImageRequest {
    String m_path;
    CdnImageRequest(String host, String path, Response.Listener<Bitmap> listener, int max_width,
        int max_height, ImageView.ScaleType scale_type, Bitmap.Config decode_config,
        Response.ErrorListener error_listener)
    {
        super("http://"+host+path, listener, max_width, max_height, scale_type,
            decode_config, error_listener);
        m_path = path;
    }
    public String getCacheKey(){ return m_path; }
}
class ThumbImageRequest implements Response.Listener<Bitmap>, Response.ErrorListener {
    private int m_image_index;
    private int m_cdn_index = 0;
    private ImageRequest request;
    ThumbImageRequest(int image_index){
        m_image_index = image_index;
        request();
    }
    private void request(){
        request = new CdnImageRequest(m_conf.m_cdns[m_cdn_index], m_conf.m_urls[m_image_index],
            this, 0, 0, ImageView.ScaleType.FIT_XY, Bitmap.Config.ARGB_8888, this);
        request.setShouldRetryServerErrors(false);
        m_request_queue.add(request);
    }
    @Override
    public void onResponse(Bitmap response){
        if (m_cur_image_index != m_image_index)
            return;
        m_cur_image = response;
        update_image();
    }
    @Override
    public void onErrorResponse(VolleyError error){
        VolleyLog.e(Const.TAG+" Error: "+error.getMessage());
        if (++m_cdn_index>=m_conf.m_cdns.length)
            return;
        Log.d(Const.TAG, "retry image request");
        request();
    }
    void cancel(){ request.cancel(); }
}
}