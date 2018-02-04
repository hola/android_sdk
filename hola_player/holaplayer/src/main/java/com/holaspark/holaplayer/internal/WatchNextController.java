package com.holaspark.holaplayer.internal;

import android.content.DialogInterface;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;
import android.view.ViewGroup;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.PlayListItem;
import com.holaspark.holaplayer.R;

public class WatchNextController {
private HolaPlayer m_hola_player;
private View m_bottom_bar;
private View m_view;
private BottomSheetBehavior m_behavior;
private ViewGroup m_holder;

public WatchNextController(HolaPlayer hola_player, PlayListItem[] items){
    m_hola_player = hola_player;
    m_view = hola_player.findViewById(R.id.watch_next);
    m_bottom_bar = hola_player.findViewById(R.id.bottom_bar_holder);
    m_holder = m_view.findViewById(R.id.playlist_items_holder);
    m_behavior = BottomSheetBehavior.from(m_view);
    m_hola_player.get_fullscreen().setOnShowListener(new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialog){ show(true); }
    });
    m_hola_player.get_fullscreen().setOnDismissListener(new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog){ show(false); }
    });
    init(items);
}

public void show(boolean visible){
    boolean is_vr = m_hola_player.get_vr_mode();
    if (is_vr)
        visible = false;
    m_view.setVisibility(visible ? View.VISIBLE : View.GONE);
    m_holder.setVisibility(is_vr ? View.GONE : View.VISIBLE);
    View v = m_bottom_bar;
    // XXX andrey: restore original bottom padding
    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),v.getPaddingRight(),
        visible ? m_behavior.getPeekHeight() : 0);
}

public void init(PlayListItem[] items){
    if (items==null || items.length==0)
        return;
    for (PlayListItem item: items)
    {
        PlaylistItemView item_view = new PlaylistItemView(m_view.getContext(),
            m_hola_player, item);
        m_holder.addView(item_view);
    }
}
}