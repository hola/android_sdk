# android_sdk

This document describes integration of a native Android app to HolaCDN.

- Currently supports MediaPlayer, VideoView and ExoPlayer (HLS mode only)
- Android version 4.4 and above is required.

Note: An [iOS version] (https://github.com/hola/ios_sdk) is also available.

If you have any questions, email us at cdn-help@hola.org, or skype: holacdn

## Initialization

- On app start init HolaCDN by calling init method.

  Signature:
  void org.hola.cdn_sdk.init(Context ctx, String customer, Bundle extra,
      Handler callback);

- Initialization occurs asynchronically. During that process two messages may
  be send to callback handler (with different "what" properties):

  org.hola.cdn_sdk.api.SERVICE_CONNECTED: it's send when api is connected to
    CDN service.

  org.hola.cdn_sdk.api.WEBSOCKET_CONNECTED: it's send when CDN service is
    connected to internal CDN code.

- function to check state:

  boolean org.hola.cdn_sdk.api.is_inited() - returns true if service is started

  boolean org.hola.cdn_sdk.api.is_connected() - returns true if service exists
  and is connected to the internal CDN code

  boolean org.hola.cdn_sdk.api.is_attached() - returns true if service exists
  and the wrapper is attached to MediaPlayer

- init example (inside an Activity):

  org.hola.cdn_sdk.api m_hola_cdn = new org.hola.cdn_sdk.api();
  Handler m_callback = new Handler(){
      @Override
      public void handleMessage(Message msg){
          Log.d("Demo", Integer.toString(msg.what); }
  };
  Bundle extra = new Bundle();
  extra.putString("hola_mode", "stats");
  m_hola_cdn.init(this, "demo", extra, m_callback);

## Attaching

Attachment is required to activate HolaCDN features. At the moment, cdn mode
for MediaPlayer object and HLS source is functional. Example:

  MediaPlayer m_player = new MediaPlayer();
  if (!m_hola_cdn.is_connected())
      Log.d("Demo", "HolaCDN isn't connected, skip attaching");
  else
      m_player = m_hola_cdn.attach(m_player);

Afterwards, m_player instance can be used as a regular MediaPlayer object.

Also, HolaCDN SDK supports:

- VideoView+Google IMA (see imademo)
- ExoPlayer+HLS (see exoplayerdemo)

## Reporting

  void org.hola.cdn_sdk.api.get_stats() - outputs current HolaCDN stats to
  logcat
