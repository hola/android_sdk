package com.holaspark.holaplayerdemo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.PlayItem;
public class VR360 extends AppCompatActivity {
private HolaPlayer m_hola_player;
@Override
protected void onCreate(Bundle saved_state){
    // XXX pavelki: restore saved state onCreate
    super.onCreate(saved_state);
    setContentView(R.layout.vr_360_player);
    m_hola_player = (HolaPlayer)findViewById(R.id.vr_player);
    m_hola_player.queue(new PlayItem(null,
        getString(R.string.video_url_hls_vr)));
    Log.d(MainActivity.TAG, "Hola Player full screen demo");
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
public void onConfigurationChanged(Configuration new_conf){
    super.onConfigurationChanged(new_conf);
    m_hola_player.fullscreen(new_conf.orientation ==
        Configuration.ORIENTATION_LANDSCAPE);
}
}