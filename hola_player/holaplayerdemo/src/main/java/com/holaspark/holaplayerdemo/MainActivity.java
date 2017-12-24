package com.holaspark.holaplayerdemo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.PlayItem;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
public class MainActivity extends AppCompatActivity {
private HolaPlayer m_hola_player = null;
private static final String TAG = "HolaPlayerDemo";
private String m_state = "PLAY";
@Override
protected void onCreate(Bundle saved_state){
    // XXX pavelki: restore saved state onCreate
    super.onCreate(saved_state);
    setContentView(R.layout.activity_main);
    m_hola_player = (HolaPlayer)findViewById(R.id.player);
    m_hola_player.queue(new PlayItem(getString(R.string.ad_tag),
        getString(R.string.video_url_hls)));
    Log.d(TAG, "Hola Player Demo player load data");
}
@Override
protected void onResume(){
    super.onResume();
    m_hola_player.setVisibility(VISIBLE);
    m_hola_player.float_mode(true);
    m_hola_player.play();
}
@Override
protected void onPause(){
    super.onPause();
    m_hola_player.pause();
    m_hola_player.setVisibility(GONE);
}
@Override
protected void onDestroy(){
    if (m_hola_player!=null)
        m_hola_player.uninit();
    super.onDestroy();
}
}
