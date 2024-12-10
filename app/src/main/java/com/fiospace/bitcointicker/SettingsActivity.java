package com.fiospace.bitcointicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    private EditText apiKeyEditText;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        apiKeyEditText = findViewById(R.id.apiKeyEditText);
        saveButton = findViewById(R.id.saveButton);
        //sharedPreferences = ("WeatherPrefs", MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Load the saved API key if it exists
        String savedApiKey = sharedPreferences.getString("API_KEY", "");
        apiKeyEditText.setText(savedApiKey);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apiKey = apiKeyEditText.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("API_KEY", apiKey);
                editor.apply();
                finish(); // Close the activity after saving
            }
        });
    }
}
