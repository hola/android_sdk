package org.hola.cdn_sdk;
import android.os.Handler;
public interface proxy_api {
public int getDuration();
public int getCurrentPosition();
public String get_url();
public int get_bitrate();
public int get_bandwidth();
public String get_state();
public boolean is_prepared();
public void set_bitrate(int br);
public void set_bandwidth(int br);
public void init(Object source, Handler handler);
}
