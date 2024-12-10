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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fiospace.bitcoin_price_fetcher.BitcoinPriceFetcher;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener  {
    private static final String TAG = "MainActivity";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private FusedLocationProviderClient fusedLocationClient;
    private Toolbar toolbar;
    private String toolbarTitle;
    private MenuItem settingsItem;
    private MaterialTextView textViewTime;
    private MaterialTextView textViewDate;
    private MaterialTextView textViewWeather;

    private Handler handler = new Handler();
    private Runnable runnable;
    private boolean showColon = true;

    private Handler weatherUpdateHandler;
    private Runnable weatherUpdateRunnable;
    private Handler marketUpdateHandler;
    private Runnable marketUpdateRunnable;
    private int marketUpdateFrequency = 30000 * 1; // 5 minutes
    private int updateFrequency = 60000 * 60; // Default frequency in milliseconds (1 hour)

    private SharedPreferences sharedPreferences;

    private MaterialTextView textViewBTC;
    private ExecutorService executorService;

    BitcoinPriceWrapper bitcoinPriceWrapper;

    String marketDataSource = "coincap";
    String secondaryMarketDataSource = "Coinbase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPreferences = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
        // Register SharedPreferences change listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        String apiKey = sharedPreferences.getString("API_KEY", "");

        // Load the initial market data source from SharedPreferences
        marketDataSource = sharedPreferences.getString("MARKET_DATA_SOURCE", "coincap");


        if (apiKey.isEmpty()) {
            // Toast.makeText(this, "API key not set. Please go to settings and set the API key.", Toast.LENGTH_SHORT).show();
            // set the font color for the toolbar and overflow item to white

            //toolbar = findViewById(R.id.toolbar);
           // setSupportActionBar(toolbar);
            //return;
        } else {
            // Hide the status bar
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (apiKey.isEmpty()) {
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            //return;
        } else {
            // Hide the Toolbar when entering fullscreen mode
            /*
            toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setVisibility(View.GONE);
            }

             */
        }

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        textViewTime = findViewById(R.id.textViewTime);
        textViewDate = findViewById(R.id.textViewDate);
        textViewWeather = findViewById(R.id.textViewWeather);
        textViewBTC = findViewById(R.id.textViewBTC);
        //adjustFontSizes();

        /*
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        */
        executorService = Executors.newSingleThreadExecutor();

        runnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);


        //startWeatherUpdates();
        startMarketUpdates();
        //fetchMarketData();
    }

    private void getLocationAndFetchWeather() {
        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    fetchWeather(lat, lon);
                } else {
                    Toast.makeText(MainActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchWeather(double lat, double lon) {
        Log.i(TAG, "fetchWeather API call");

        String apiKey = sharedPreferences.getString("API_KEY", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key not set. Please go to settings and set the API key.", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        Log.d(TAG,retrofit.toString());

        WeatherService weatherService = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = weatherService.getCurrentWeather(lat, lon, apiKey, "imperial");
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    double temp = weatherResponse.getMain().getTemp();
                    int roundedTemp = (int) Math.round(temp); // Round temperature to 0 decimal places

                    String tempStringH = getString(R.string.temp, roundedTemp);
                    textViewWeather.setText(tempStringH);
                } else {
                    Log.e(TAG, "Response unsuccessful or body is null");
                    Log.e(TAG,response.message());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch weather", t);
            }
        });
    }

    private void startWeatherUpdates() {
        weatherUpdateHandler = new Handler(Looper.getMainLooper());
        weatherUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                getLocationAndFetchWeather();
                weatherUpdateHandler.postDelayed(this, updateFrequency);
            }
        };
        weatherUpdateHandler.post(weatherUpdateRunnable);
    }

    private void stopWeatherUpdates() {
        if (weatherUpdateHandler != null && weatherUpdateRunnable != null) {
            weatherUpdateHandler.removeCallbacks(weatherUpdateRunnable);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndFetchWeather();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
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

    /**
     * async calls to get market prices
     */
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
            try {
                /**/
                String formattedPrice = BitcoinPriceWrapper.getPrice(marketDataSource);
                /*
                if (formattedPrice.isEmpty())
                     formattedPrice= BitcoinPriceFetcher.getPrice(secondaryMarketDataSource);
                */
                Log.i(TAG,marketDataSource + " BTC Price: " + formattedPrice);
                runOnUiThread(() -> textViewBTC.setText(formattedPrice));
                runOnUiThread(() -> textViewWeather.setText(marketDataSource));

                /**
                Log.i(TAG, "fetchMarketData() API call.");

                URL url = new URL("https://api.coinbase.com/v2/prices/spot?currency=USD");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    String result = response.toString();
                    JSONObject jsonObject = new JSONObject(result);
                    double btcPrice = jsonObject.getJSONObject("data").getDouble("amount");
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
                    currencyFormat.setMaximumFractionDigits(0);
                    String formattedPrice = currencyFormat.format(btcPrice);
                    runOnUiThread(() -> textViewBTC.setText(formattedPrice));
                    Log.i(TAG, "BTC Price: " + formattedPrice);
                } finally {
                    urlConnection.disconnect();
                }
                 **/
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWeatherUpdates();
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

        // Adjust the font sizes based on the scale
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

        // enter the API KEY
        if (item.getItemId() == R.id.settings) {
            Log.d("MainActivity", "Settings menu item clicked");

            // Set the market data sources before starting the activity
            SettingsActivity.setMarketDataSources(BitcoinPriceWrapper.getConfiguredMarketDataSources());

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        /**
        // choose font for time
        if (item.getItemId() == R.id.action_choose_font) {
            // Show font chooser dialog
            showFontChooserDialog();
            return true;
        }
         **/
        return super.onOptionsItemSelected(item);
    }


    private void showFontChooserDialog() {
        // Create and show the font chooser dialog
        // You can use DialogFragment or create a custom dialog
    }

    /**
    @Override
    protected void onResume() {
        Log.i(TAG,"onResume():" );

        super.onResume();
    }
    **/

    @Override
    protected void onPause() {
        Log.i(TAG,"onPause():");

        super.onPause();
    }

    /**
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG,"key:" + key);
        // Check if the key is related to weather settings
        if (key.equals("API_KEY")) {
            // Fetch weather data
            Log.i(TAG,"Resetting API_KEY and startWeatherUpdates()");
            startWeatherUpdates();
        }
    }
     **/

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "key: " + key);
        if (key.equals("API_KEY")) {
            // Fetch weather data
            Log.i(TAG, "Resetting API_KEY and startWeatherUpdates()");
            startWeatherUpdates();
        } else if (key.equals("MARKET_DATA_SOURCE")) {
            // Update the market data source when it changes in settings
            marketDataSource = sharedPreferences.getString(key, "coincap");
            Log.i(TAG, "Market data source updated to: " + marketDataSource);
            // Restart market updates with the new source
            stopMarketUpdates();
            startMarketUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure marketDataSource is updated when resuming, in case it was changed
        marketDataSource = sharedPreferences.getString("MARKET_DATA_SOURCE", "coincap");
    }
}
