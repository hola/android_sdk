package com.holaspark.holaplayer.internal;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.ext.ima.ImaAdsMediaSource;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.holaspark.holaplayer.PlayItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
public class ExoPlayerController {
private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();
private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
private Context m_context;
private ExoPlayer m_exoplayer;
private Listener m_listener;
private Renderer[] m_renderers;
private DefaultTrackSelector m_trackselector;
private LoadControl m_loadcontrol;
private Looper m_looper;
private Handler m_handler;
private DefaultDataSourceFactory m_datasource;
private MediaSource m_mediasource = null;
private EventListener m_clientlistener;
private ImaAdsLoader m_ads_loader;
private View m_overlay;
private Surface m_surface;
private SurfaceHolder surface_holder;
public ExoPlayerController(){}
public boolean init(Context context, AttributeSet attributes, View overlay)
{
    m_context = context;
    m_overlay = overlay;
    RenderersFactory factory = new DefaultRenderersFactory(context);
    m_looper = Looper.myLooper() != null ? Looper.myLooper() :
        Looper.getMainLooper();
    m_handler = new Handler(m_looper);
    m_listener = new Listener();
    m_renderers = factory.createRenderers(m_handler, m_listener, m_listener,
        m_listener, m_listener);
    TrackSelection.Factory adaptive_selection_factory =
        new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
    m_trackselector = new DefaultTrackSelector(adaptive_selection_factory);
    m_loadcontrol = new DefaultLoadControl();
    m_exoplayer = ExoPlayerFactory.newInstance(m_renderers, m_trackselector,
        m_loadcontrol);
    m_datasource = new DefaultDataSourceFactory(m_context, BANDWIDTH_METER,
        new DefaultHttpDataSourceFactory(Util.getUserAgent(m_context,
        "HolaPlayer"), BANDWIDTH_METER));
    return true;
}
public void play(){ m_exoplayer.setPlayWhenReady(true); }
public void pause(){ m_exoplayer.setPlayWhenReady(false); }
public void seek(long position){ m_exoplayer.seekTo(position); }
public void load(String uri){ queue(new PlayItem(null, uri)); }
public void queue(PlayItem item){
    // XXX pavelki: no real queue at the moment
    MediaSource media_source = null;
    String ad_tag = item.get_ad_tag();
    Uri uri = Uri.parse(item.get_media());
    int media_type = ContentTypeDetector.detect_source(uri);
    switch (media_type)
    {
    case C.TYPE_HLS:
        media_source = new HlsMediaSource(uri, m_datasource, m_handler,
            m_listener);
        break;
    case C.TYPE_DASH:
        media_source = new DashMediaSource(uri, m_datasource,
            new DefaultDashChunkSource.Factory(m_datasource), m_handler,
            m_listener);
        break;
    case C.TYPE_OTHER:
        media_source = new ExtractorMediaSource(uri, m_datasource,
            new DefaultExtractorsFactory(), m_handler, m_listener);
        break;
    }
    if (media_source==null)
        return;
    if (ad_tag==null || m_overlay==null)
        m_mediasource = media_source;
    else
    {
        m_ads_loader = new ImaAdsLoader(m_context, Uri.parse(ad_tag));
        m_mediasource = new ImaAdsMediaSource(media_source, m_datasource,
            m_ads_loader, (ViewGroup) m_overlay);
    }
    m_exoplayer.prepare(m_mediasource);
}
public void set_texture_view(TextureView texture_view){
    Log.d(Const.TAG, "set texture view "+texture_view);
    SurfaceTexture video_holder = null;
    if (texture_view!=null)
    {
        texture_view.setSurfaceTextureListener(m_listener);
        video_holder = texture_view.isAvailable() ?
            texture_view.getSurfaceTexture() : null;
    }
    set_surface(video_holder==null ? null : new Surface(video_holder));
}
private void set_surface(Surface surface){
    Surface surf = surface==null || !surface.isValid() ? null : surface;
    Log.d(Const.TAG, "set surface "+surf);
    if (m_surface!=null)
        m_surface.release();
    Vector<ExoPlayer.ExoPlayerMessage> messages = new Vector<>();
    for (Renderer renderer : m_renderers)
    {
        if (renderer.getTrackType() != C.TRACK_TYPE_VIDEO)
            continue;
        messages.add(new ExoPlayer.ExoPlayerMessage(renderer, C.MSG_SET_SURFACE,
            surf));
    }
    m_exoplayer.sendMessages(messages.toArray(
        new ExoPlayer.ExoPlayerMessage[messages.size()]));
    m_surface = surface;
}
public float get_aspect(){ return m_listener.m_video_aspect; }
public List<QualityItem> get_quality_items(){
    List<QualityItem> res = new ArrayList<>();
    MappingTrackSelector.MappedTrackInfo info = m_trackselector.getCurrentMappedTrackInfo();
    if (info==null)
        return res;
    TrackGroupArray groups = null;
    int renderer_index = 0;
    for (int i = 0; i<info.length; i++)
    {
        if (m_exoplayer.getRendererType(i)!=C.TRACK_TYPE_VIDEO)
            continue;
        groups = info.getTrackGroups(i);
        if (groups.length>0)
        {
            renderer_index = i;
            break;
        }
    }
    if (groups==null)
        return res;
    for (int i = 0; i<groups.length; i++)
    {
        TrackGroup group = groups.get(i);
        for (int j = 0; j<group.length; j++)
            res.add(new QualityItem(groups, renderer_index, i, j));
    }
    return res;
}
public void set_quality(QualityItem item){
    Log.d(Const.TAG, "set quality "+(item==null ? "auto" : item));
    if (item==null)
    {
        m_trackselector.clearSelectionOverrides();
        return;
    }
    MappingTrackSelector.SelectionOverride override = new MappingTrackSelector.SelectionOverride(
        FIXED_FACTORY, item.m_group_index, item.m_track_index);
    m_trackselector.setSelectionOverride(item.m_renderer_index, item.m_groups, override);
}
public void set_controlbar(PlayerControlView m_controlbar){
    m_controlbar.setPlayer(m_exoplayer);
    m_controlbar.set_player_controller(this);
}
public void add_event_listener(EventListener listener){
    m_clientlistener = listener;
}
public void uninit(){
    set_surface(null);
    m_exoplayer.release();
}
public interface EventListener {
    public void on_video_aspect();
    public void on_rendered_first();
}
private final class Listener implements VideoRendererEventListener,
    AudioRendererEventListener, TextRenderer.Output, MetadataRenderer.Output,
    TransferListener<DataSource>, AdaptiveMediaSourceEventListener,
    ExtractorMediaSource.EventListener, TextureView.SurfaceTextureListener
{
    private float m_video_aspect = 0;
    @Override
    public void onVideoEnabled(DecoderCounters counters){}
    @Override
    public void onVideoDecoderInitialized(String decoder_name, long timestamp,
        long duration)
    {
    }
    @Override
    public void onVideoInputFormatChanged(Format format){}
    @Override
    public void onDroppedFrames(int count, long elapsed){}
    @Override
    public void onVideoSizeChanged(int width, int height, int rotation,
        float pixel_aspect)
    {
        Log.d(Const.TAG, "on video size changed w "+width+" h "+height);
        float new_aspect = height==0 ? 1 : (width*pixel_aspect)/height;
        if (new_aspect!=m_video_aspect)
        {
            m_video_aspect = new_aspect;
            if (ExoPlayerController.this.m_clientlistener!=null)
                ExoPlayerController.this.m_clientlistener.on_video_aspect();
        }
    }
    @Override
    public void onRenderedFirstFrame(Surface surface){
        if (ExoPlayerController.this.m_clientlistener!=null)
            ExoPlayerController.this.m_clientlistener.on_rendered_first();
    }
    @Override
    public void onVideoDisabled(DecoderCounters counters){}
    @Override
    public void onAudioEnabled(DecoderCounters counters){}
    @Override
    public void onAudioSessionId(int audioSessionId){}
    @Override
    public void onAudioDecoderInitialized(String decoder_name, long timestamp,
        long duration)
    {
    }
    @Override
    public void onAudioInputFormatChanged(Format format){}
    @Override
    public void onAudioTrackUnderrun(int buffer_size, long buffer_size_ms,
        long elapsed)
    {
    }
    @Override
    public void onAudioDisabled(DecoderCounters counters){}
    @Override
    public void onMetadata(Metadata metadata){
        Log.d(Const.TAG, "metadata "+metadata);
    }
    @Override
    public void onCues(List<Cue> cues){}
    @Override
    public void onTransferStart(DataSource source, DataSpec data_spec){}
    @Override
    public void onBytesTransferred(DataSource source, int transferred){}
    @Override
    public void onTransferEnd(DataSource source){}
    @Override
    public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs)
    {
    }
    @Override
    public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
        long loadDurationMs, long bytesLoaded)
    {
    }
    @Override
    public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
        long loadDurationMs, long bytesLoaded)
    {
    }
    @Override
    public void onLoadError(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
        long loadDurationMs, long bytesLoaded, IOException error,
        boolean wasCanceled)
    {
    }
    @Override
    public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs,
        long mediaEndTimeMs)
    {
    }
    @Override
    public void onDownstreamFormatChanged(int trackType, Format trackFormat,
        int trackSelectionReason, Object trackSelectionData, long mediaTimeMs)
    {
        Log.d(Const.TAG, "downstream format "+trackFormat);
    }
    @Override
    public void onLoadError(IOException error){}
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
        int height)
    {
        set_surface(new Surface(surface));
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
        int height)
    {
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){
        set_surface(null);
        return true;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface){}
}
}
