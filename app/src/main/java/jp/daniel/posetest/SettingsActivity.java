package jp.daniel.posetest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    private EditText url_box;
    private EditText api_key_box;
    private EditText project_id_box;

    private Button ok_button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        url_box = findViewById(R.id.URL_box);
        project_id_box = findViewById(R.id.PROJECT_ID_box);
        api_key_box = findViewById(R.id.API_KEY_box);

        setFromIntent();

        Button b = findViewById(R.id.reset_button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFromIntent();
            }
        });

        b = findViewById(R.id.ok_button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIntent();
            }
        });
    }

    private void setFromIntent() {
        Intent i = getIntent();
        String s = i.getStringExtra("URL");
        url_box.setText(s);
        s = i.getStringExtra("PROJECT_ID");
        project_id_box.setText(s);
        s = i.getStringExtra("API_KEY");
        api_key_box.setText(s);
    }

    private void setIntent() {

        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        String s = url_box.getText().toString();
        settingsIntent.putExtra("URL", url_box.getText().toString());
        settingsIntent.putExtra("PROJECT_ID", project_id_box.getText().toString());
        settingsIntent.putExtra("API_KEY", api_key_box.getText().toString());
        setResult(RESULT_OK, settingsIntent);
        finish();
    }


}