package com.holaspark.holaplayer.internal;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
public class FullScreenPlayer extends Dialog {
private boolean m_active;
private View m_player;
private PlayerViewManager m_viewmanager;
public FullScreenPlayer(@NonNull Context context){
    super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    m_active = false;
    m_viewmanager = new PlayerViewManager();
}
public void activate(View player){
    m_player = player;
    m_viewmanager.detach(player);
    addContentView(player, new ViewGroup.LayoutParams(ViewGroup.LayoutParams
        .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    m_active = true;
    show();
}
public void restore_player(){
    if (!m_active)
        return;
    m_viewmanager.restore(m_player);
    m_active = false;
    dismiss();
}
@Override
public void onBackPressed(){
    restore_player();
    super.onBackPressed();
}
}
