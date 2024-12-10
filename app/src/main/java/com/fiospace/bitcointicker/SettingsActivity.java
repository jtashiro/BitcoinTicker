package com.fiospace.bitcointicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private Spinner marketDataSourceSpinner;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    private static List<String> staticMarketDataSources;

    // Static method to set market data sources before activity creation
    public static void setMarketDataSources(List<String> marketDataSources) {
        SettingsActivity.staticMarketDataSources = marketDataSources;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        marketDataSourceSpinner = findViewById(R.id.marketDataSourceSpinner);
        saveButton = findViewById(R.id.saveButton);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Use the static list if it's set, otherwise initialize with default values
        List<String> marketDataSources = staticMarketDataSources != null ? staticMarketDataSources : new ArrayList<>();
        if (marketDataSources.isEmpty()) {
            // Add some default sources if none are provided
            marketDataSources.add("Coinbase");
            marketDataSources.add("Coincap");
        }

        // Set up the spinner with options for market data sources
        setupSpinner(marketDataSources);

        // Load the saved market data source if it exists
        String savedMarketDataSource = sharedPreferences.getString("MARKET_DATA_SOURCE", "");
        selectSpinnerItemByValue(marketDataSourceSpinner, savedMarketDataSource);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String marketDataSource = marketDataSourceSpinner.getSelectedItem().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("MARKET_DATA_SOURCE", marketDataSource);
                editor.apply();
                finish(); // Close the activity after saving
            }
        });
    }

    private void setupSpinner(List<String> marketDataSources) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, marketDataSources);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        marketDataSourceSpinner.setAdapter(adapter);

        marketDataSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // This listener can be used if you want to perform actions on selection change
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle if necessary
            }
        });
    }

    // Method to select spinner item by value
    private void selectSpinnerItemByValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("market_data_source", marketDataSourceSpinner.getSelectedItem().toString());
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}