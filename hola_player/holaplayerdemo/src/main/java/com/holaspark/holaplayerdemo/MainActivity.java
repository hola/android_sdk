package com.holaspark.holaplayerdemo;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.PlayItem;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
public class MainActivity extends ListActivity {
static final String TAG = "HolaPlayerDemo";
@Override
public void onCreate(Bundle saved_state){
    // XXX pavelki: restore saved state onCreate
    super.onCreate(saved_state);
    setContentView(R.layout.main_activity);
    String[] values =
        new String[]{"Main demo", "360 VR player"};
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
        values);
    setListAdapter(adapter);
}
@Override
protected void onListItemClick(ListView listview, View view, int pos, long id){
    final Class[] choices = new Class[] { MainDemo.class, VR360.class};
    startActivity(new Intent(this, choices[pos]));
}
}
