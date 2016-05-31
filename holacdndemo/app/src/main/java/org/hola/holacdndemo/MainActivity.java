package org.hola.holacdndemo;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import org.hola.cdn_sdk.api;
import java.io.IOException;
public class MainActivity extends AppCompatActivity
    implements SurfaceHolder.Callback
{
public final static String TAG = "HolaCDN_demo";
private String video = "http://player.h-cdn.org/static/hls/cdn2/master.m3u8";
private boolean m_finished = false;
private boolean m_prepared = false;
private boolean m_starting = false;
private int m_orientation = 0;
private api m_hola_cdn;
private MediaPlayer m_player;
private SurfaceHolder m_holder;
private SurfaceView m_surface;
private ProgressDialog m_pd;
private Handler m_callback = new Handler(){
    @Override
    public void handleMessage(Message msg){
        switch (msg.what)
        {
        case api.MSG_WEBSOCKET_CONNECTED:
            m_player = MediaPlayer.create(MainActivity.this, Uri.parse(video),
                m_holder);
            m_player.stop();
            m_player.reset();
            m_player = m_hola_cdn.attach(m_player);
            m_player.setOnVideoSizeChangedListener(
                new MediaPlayer.OnVideoSizeChangedListener(){
                @Override
                public void onVideoSizeChanged(MediaPlayer mediaPlayer,
                    int w, int h)
                {
                    surface_update(w, h);
                }
            });
            Log.i(TAG, "CDN attached");
            break;
        case api.MSG_TIMEUPDATE:
            m_seekbar.setProgress(msg.arg1);
            break;
        }
    }
};
private SeekBar m_seekbar;
private long m_lastseektime = 0;
private void surface_update(int w, int h){
    Rect rect = new Rect();
    getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
    int sh = rect.bottom-rect.top, sw = rect.right-rect.left;
    ViewGroup.LayoutParams params =
        m_surface.getLayoutParams();
    params.width = w>h ? sw : sh*w/h;
    params.height = w>h ? sw*h/w : sh;
    m_surface.setLayoutParams(params);
}
@Override
public void onConfigurationChanged(Configuration newConfig){
    super.onConfigurationChanged(newConfig);
    if (newConfig.orientation!=m_orientation && m_prepared)
    {
        m_orientation = newConfig.orientation;
        surface_update(m_player.getVideoWidth(), m_player.getVideoHeight());
    }
}
@Override
protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Bundle extra = new Bundle();
    extra.putString("hola_zone", getString(R.string.hola_zone));
    extra.putString("hola_mode", getString(R.string.hola_mode));
    m_hola_cdn = new api();
    m_hola_cdn.init(this, getString(R.string.customer), extra, m_callback);
    m_seekbar = (SeekBar) findViewById(R.id.seekbar);
    m_surface = (SurfaceView) findViewById(R.id.surfview);
    m_holder = m_surface.getHolder();
    m_holder.addCallback(MainActivity.this);
    surface_update(640, 360);
    Log.i(TAG, "Player created");
    ((Button) findViewById(R.id.button))
    .setOnClickListener(new Button.OnClickListener(){
        @Override
        public void onClick(View view){
            if (m_starting|| m_finished)
                return;
            m_starting = true;
            if (!m_prepared)
            {
                try {
                    video = ((EditText) findViewById(R.id.editText)).getText()
                    .toString();
                    Uri.parse(video);
                    findViewById(R.id.editText).setVisibility(View.INVISIBLE);
                    findViewById(R.id.textView).setVisibility(View.INVISIBLE);
                    m_player.setDataSource(video);
                    m_player.setOnPreparedListener(
                        new MediaPlayer.OnPreparedListener(){
                            @Override
                            public void onPrepared(MediaPlayer mplayer){
                                m_seekbar.setMax(m_player.getDuration());
                                m_player.setAudioStreamType(
                                    AudioManager.STREAM_MUSIC);
                                Log.i(TAG, "Video prepared");
                                m_prepared = true;
                                m_player.start();
                                Toast.makeText(MainActivity.this,
                                    "Playback started", Toast.LENGTH_SHORT)
                                .show();
                            }
                        });
                    m_player.prepareAsync();
                } catch(IOException i){
                    i.printStackTrace();
                } catch(NullPointerException ne){
                    Toast.makeText(MainActivity.this, "Invalid URL",
                        Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (!m_player.isPlaying())
            {
                Toast.makeText(MainActivity.this, "Playback resumed",
                    Toast.LENGTH_SHORT).show();
                m_player.start();
            }
        }
    });
    ((Button) findViewById(R.id.button2))
    .setOnClickListener(new Button.OnClickListener(){
        @Override
        public void onClick(View view){
            if (m_finished)
                return;
            if (m_player.isPlaying())
            {
                Toast.makeText(MainActivity.this, "Playback paused",
                    Toast.LENGTH_SHORT).show();
                m_player.pause();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Playback resumed",
                    Toast.LENGTH_SHORT).show();
                m_player.start();
            }
        }
    });
    ((Button) findViewById(R.id.button3))
    .setOnClickListener(new Button.OnClickListener(){
        @Override
        public void onClick(View view){
            m_player.stop();
            findViewById(R.id.button).setEnabled(false);
            findViewById(R.id.button2).setEnabled(false);
            m_finished = true;
            Toast.makeText(MainActivity.this, "Playback stopped",
                Toast.LENGTH_SHORT).show();
        }
    });
    EditText edit = (EditText) findViewById(R.id.editText);
    edit.setText(video);
    ((SeekBar) findViewById(R.id.seekbar))
    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
        @Override
        public void onProgressChanged(SeekBar seekBar, int position,
            boolean from_user)
        {
            if (from_user)
            {
                if (System.currentTimeMillis()-m_lastseektime < 500)
                    return;
                m_lastseektime = System.currentTimeMillis();
                m_player.seekTo(position);
                Toast.makeText(MainActivity.this, "Seek to "+position/1000,
                    Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar){}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar){}
    });
    m_pd = ProgressDialog.show(this, "HolaCDN demo", "Loading...");
    new Thread(){
        @Override
        public void run(){
            while (true)
            {
                if (m_hola_cdn.is_attached())
                {
                    m_pd.dismiss();
                    break;
                }
                try { Thread.sleep(200); }
                catch(InterruptedException ie){}
            }
        }
    }.start();
}
@Override
protected void onDestroy(){
    super.onDestroy();
    m_player.release();
}
@Override
public void surfaceCreated(SurfaceHolder surfaceHolder){}
@Override
public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2){
}
@Override
public void surfaceDestroyed(SurfaceHolder surfaceHolder){}
}
