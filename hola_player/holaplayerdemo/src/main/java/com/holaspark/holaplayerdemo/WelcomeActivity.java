package com.holaspark.holaplayerdemo;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class WelcomeActivity extends Activity implements View.OnClickListener {
private EditText m_customer_edit;
private TextView m_check_customer_result;
@Override
public void onCreate(Bundle saved_state){
    super.onCreate(saved_state);
    setContentView(R.layout.welcome);
    Button start = findViewById(R.id.start);
    m_customer_edit = findViewById(R.id.customer_id);
    m_check_customer_result = findViewById(R.id.check_customer_result);
    start.setOnClickListener(this);
}
@Override
public void onClick(View v){
    final String customer_id = m_customer_edit.getText().toString();
    String url = "http://player.h-cdn.com/check_customer?customer="+customer_id;
    RequestQueue queue = Volley.newRequestQueue(this);
    StringRequest request = new StringRequest(Request.Method.GET, url,
        new Response.Listener<String>() {
            @Override
            public void onResponse(String response){
                m_check_customer_result.setText("");
                start(customer_id);
            }
        }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error){
            m_check_customer_result.setText(error.networkResponse.statusCode==403 ?
                "Error: no such customerID" : "Error: "+error.networkResponse.statusCode);
        }
    });
    m_check_customer_result.setText(R.string.loading);
    queue.add(request);
}
void start(String customer_id){
    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra("customer_id", customer_id);
    startActivityForResult(intent, 1);
}
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1 && resultCode == RESULT_FIRST_USER)
    {
        Toast.makeText(this, "No video samples were found", Toast.LENGTH_LONG)
            .show();
    }
}
}
