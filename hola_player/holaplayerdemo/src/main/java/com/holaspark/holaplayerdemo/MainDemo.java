package com.holaspark.holaplayerdemo;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.HolaPlayerCallback;
import com.holaspark.holaplayer.PlayItem;
import com.holaspark.holaplayer.PlayListItem;
import com.holaspark.holaplayer.internal.Const;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
public class MainDemo extends AppCompatActivity implements View.OnClickListener {
private HolaPlayer m_hola_player = null;
private String m_customer;
private List<PlayListItem> m_playlist;
@Override
protected void onCreate(Bundle saved_state){
    // XXX pavelki: restore saved state onCreate
    super.onCreate(saved_state);
    setContentView(R.layout.main_demo);
    m_hola_player = (HolaPlayer)findViewById(R.id.float_player);
    Log.d(MainActivity.TAG, "Hola Player main demo");
    Button btn = (Button)findViewById(R.id.search_btn);
    btn.setOnClickListener(this);
}
@Override
protected void onResume(){
    super.onResume();
    m_hola_player.setVisibility(View.VISIBLE);
    m_hola_player.play();
}
@Override
protected void onPause(){
    super.onPause();
    m_hola_player.pause();
    m_hola_player.setVisibility(View.GONE);
}
@Override
protected void onDestroy(){
    if (m_hola_player!=null)
        m_hola_player.uninit();
    super.onDestroy();
}
@Override
public void onClick(View view){
    m_customer = ((TextView)findViewById(R.id.editText)).getText().toString();
    m_hola_player.set_customer(m_customer);
    View focus_view = this.getCurrentFocus();
    if (focus_view != null) {
        InputMethodManager imm = (InputMethodManager)getSystemService(
            Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(focus_view.getWindowToken(),
            InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
    m_hola_player.load_playlists(new HolaPlayerCallback(){
        @Override
        public void done(String data){
            JSONArray json = null;
            try
            {
                data = data.substring(data.indexOf('['), data.lastIndexOf(']')+1);
                json = new JSONArray(data);
                m_playlist = new LinkedList<>();
                int len = Math.min(json.length(), 4);
                for (int i = 0; i<json.length() && m_playlist.size()<len; i++)
                {
                    Log.d(Const.TAG, json.getJSONObject(i).toString());
                    JSONObject row = json.getJSONObject(i)
                        .getJSONObject("video_info");
                    Log.d(MainActivity.TAG, "row "+row);
                    String desc = "N/A", poster = "", url = "";
                    if (row.has("description"))
                        desc = row.getString("description");
                    else if (row.has("title"))
                        desc = row.getString("title");
                    if (row.has("poster"))
                        poster = row.getString("poster");
                    else if (row.has("video_poster"))
                        poster = row.getString("video_poster");
                    if (!row.has("url"))
                        continue;
                    url = row.getString("url");
                    m_playlist.add(new PlayListItem(url, poster, desc));
                }
                if (m_playlist.size()<1)
                    return;
                m_hola_player.queue(new PlayItem(getString(R.string.ad_tag),
                    m_playlist.get(0).m_video_url));
                PlayListItem[] items = (PlayListItem[])m_playlist.toArray(
                    new PlayListItem[m_playlist.size()]);
                items = Arrays.copyOfRange(items, 1, items.length);
                m_hola_player.set_watch_next_items(items);
            } catch(JSONException e){
                Log.d(MainActivity.TAG, "JSON exception "+e); }
        }
    });
}
}
