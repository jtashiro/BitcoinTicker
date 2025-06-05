package com.fiospace.bitcointicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fiospace.bitcoin_price_fetcher.BitcoinPriceFetcher;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private FusedLocationProviderClient fusedLocationClient;
    private Toolbar toolbar;
    private String toolbarTitle;
    private MenuItem settingsItem;
    private MaterialTextView textViewTime;
    private MaterialTextView textViewDate;
    private MaterialTextView textViewWeather;
    private MaterialTextView textViewBTC;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean showColon = true;

    private Handler weatherUpdateHandler;
    private Runnable weatherUpdateRunnable;
    private Handler marketUpdateHandler;
    private Runnable marketUpdateRunnable;
    private int marketUpdateFrequency = 60000; // 60 seconds for testing, adjust as needed
    private int updateFrequency = 60000 * 60; // Default frequency in milliseconds (1 hour)

    private SharedPreferences sharedPreferences;
    private ExecutorService executorService;

    private String marketDataSource = "coinbase";
    private List<String> availableMarketSources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        String apiKey = sharedPreferences.getString("API_KEY", "");
        marketDataSource = sharedPreferences.getString("MARKET_DATA_SOURCE", marketDataSource);
        availableMarketSources = BitcoinPriceWrapper.getConfiguredMarketDataSources();

        if (apiKey.isEmpty()) {
            // Handle empty API key
        }

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textViewTime = findViewById(R.id.textViewTime);
        textViewDate = findViewById(R.id.textViewDate);
        textViewWeather = findViewById(R.id.textViewWeather);
        textViewBTC = findViewById(R.id.textViewBTC);

        executorService = Executors.newSingleThreadExecutor();

        runnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        startMarketUpdates();
    }

    private void updateTime() {
        String currentDate = new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date());
        textViewDate.setText(currentDate);

        // Format the time to show only hours and minutes
        String currentTime = new SimpleDateFormat(showColon ? "h:mm a" : "h mm a", Locale.getDefault()).format(new Date());
        textViewTime.setText(currentTime);

        // Toggle the colon every second
        showColon = !showColon;
        showColon = true;
    }

    private void startMarketUpdates() {
        marketUpdateHandler = new Handler(Looper.getMainLooper());
        marketUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchMarketData();
                marketUpdateHandler.postDelayed(this, marketUpdateFrequency);
            }
        };
        marketUpdateHandler.post(marketUpdateRunnable);
    }

    private void stopMarketUpdates() {
        if (marketUpdateHandler != null && marketUpdateRunnable != null) {
            marketUpdateHandler.removeCallbacks(marketUpdateRunnable);
        }
    }

    private void fetchMarketData() {
        executorService.execute(() -> {
            String formattedPrice = null;
            String successfulSource = null;
            boolean priceFetched = false;

            // Try the primary market data source first
            try {
                formattedPrice = BitcoinPriceWrapper.getPrice(marketDataSource);
                successfulSource = marketDataSource;
                priceFetched = true;
                Log.i(TAG, marketDataSource + " BTC Price: " + formattedPrice);
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch price from " + marketDataSource + ": " + e.getMessage());
            }

            // If primary source fails, try other available sources
            if (!priceFetched) {
                for (String source : availableMarketSources) {
                    if (!source.equalsIgnoreCase(marketDataSource)) {
                        try {
                            formattedPrice = BitcoinPriceWrapper.getPrice(source);
                            successfulSource = source;
                            priceFetched = true;
                            Log.i(TAG, source + " BTC Price: " + formattedPrice);
                            break; // Exit loop on successful fetch
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to fetch price from " + source + ": " + e.getMessage());
                        }
                    }
                }
            }

            // Create final copies for use in lambda
            final String finalFormattedPrice = formattedPrice;
            final String finalSuccessfulSource = successfulSource;
            final boolean finalPriceFetched = priceFetched;

            // Update UI on the main thread
            runOnUiThread(() -> {
                if (finalPriceFetched && finalFormattedPrice != null) {
                    textViewBTC.setText(finalFormattedPrice);
                    textViewBTC.requestLayout();
                    textViewWeather.setText(finalSuccessfulSource); // Display the source that worked
                } else {
                    textViewBTC.setText("N/A");
                    textViewWeather.setText("No data");
                    Toast.makeText(MainActivity.this, "Failed to fetch Bitcoin price from all sources", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMarketUpdates();
        executorService.shutdown();
        handler.removeCallbacks(runnable);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private float getFontScale() {
        Configuration configuration = getResources().getConfiguration();
        return configuration.fontScale;
    }

    private void adjustFontSizes() {
        float fontScale = getFontScale();
        Log.i(TAG, "fontScale=" + fontScale);

        textViewTime.setTextSize(500 * fontScale);
        textViewDate.setTextSize(80 * fontScale);
        textViewWeather.setTextSize(125 * fontScale);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MainActivity", "onCreateOptionsMenu called");
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MainActivity", "onOptionsItemSelected called with item id: " + item.getItemId());

        if (item.getItemId() == R.id.settings) {
            Log.d("MainActivity", "Settings menu item clicked");
            SettingsActivity.setMarketDataSources(BitcoinPriceWrapper.getConfiguredMarketDataSources());
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause():");
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "key: " + key);
        if (key.equals("API_KEY")) {
            Log.i(TAG, "Resetting API_KEY and startWeatherUpdates()");
        } else if (key.equals("MARKET_DATA_SOURCE")) {
            marketDataSource = sharedPreferences.getString(key, marketDataSource);
            Log.i(TAG, "Market data source updated to: " + marketDataSource);
            stopMarketUpdates();
            startMarketUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        marketDataSource = sharedPreferences.getString("MARKET_DATA_SOURCE", marketDataSource);
    }
}