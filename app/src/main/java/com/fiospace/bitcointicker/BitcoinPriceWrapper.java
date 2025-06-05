package com.fiospace.bitcointicker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;

public class BitcoinPriceWrapper {

    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";
    private static final String BITFINEX_API_URL = "https://api-pub.bitfinex.com/v2/tickers?symbols=tBTCUSD";
    private static final String BITSTAMP_API_URL = "https://www.bitstamp.net/api/v2/ticker/btcusd";
    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2/prices/spot?currency=USD";
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd";
    private static final String CRYPTOCOMPARE_API_URL = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD";
    private static final String GEMINI_API_URL = "https://api.gemini.com/v1/pubticker/btcusd";
    private static final String KRAKEN_API_URL = "https://api.kraken.com/0/public/Ticker?pair=XXBTZUSD";

    public static String getPrice(String exchange) throws Exception {
        if (exchange == null || exchange.trim().isEmpty()) {
            throw new IllegalArgumentException("Exchange cannot be null or empty");
        }

        // Normalize exchange name to lowercase
        String normalizedExchange = exchange.trim().toLowerCase();

        // Construct the method name (e.g., "getPriceFromBinance")
        String methodName = "getPriceFrom" + normalizedExchange.substring(0, 1).toUpperCase() + normalizedExchange.substring(1);

        BigDecimal price;
        try {
            // Get the method dynamically
            Method method = BitcoinPriceWrapper.class.getDeclaredMethod(methodName);
            // Invoke the method and cast the result to BigDecimal
            price = (BigDecimal) method.invoke(null);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Unsupported exchange: " + normalizedExchange, e);
        } catch (Exception e) {
            // Handle invocation errors (e.g., InvocationTargetException, IllegalAccessException)
            throw new Exception("Error fetching price from " + normalizedExchange + ": " + e.getCause().getMessage(), e);
        }

        // Format the price to include commas for thousands and zero decimal places
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);

        return formatter.format(price.longValue());
    }

    private static BigDecimal getPriceFromBinance() throws Exception {
        logURL(BINANCE_API_URL);
        URL url = new URL(BINANCE_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            JSONObject json = new JSONObject(in.readLine());
            return new BigDecimal(json.getString("price"));
        }
    }

    private static BigDecimal getPriceFromBitfinex() throws Exception {
        logURL(BITFINEX_API_URL);
        URL url = new URL(BITFINEX_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            JSONArray json = new JSONArray(in.readLine());
            return new BigDecimal(json.getJSONArray(0).getString(7));
        }
    }

    private static BigDecimal getPriceFromBitstamp() throws Exception {
        logURL(BITSTAMP_API_URL);
        URL url = new URL(BITSTAMP_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            JSONObject json = new JSONObject(in.readLine());
            return new BigDecimal(json.getString("last"));
        }
    }

    private static BigDecimal getPriceFromCoinbase() throws Exception {
        logURL(COINBASE_API_URL);
        URL url = new URL(COINBASE_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            JSONObject json = new JSONObject(response.toString());
            JSONObject data = json.getJSONObject("data");
            return new BigDecimal(data.getString("amount"));
        }
    }

    private static BigDecimal getPriceFromCoingecko() throws Exception {
        logURL(COINGECKO_API_URL);
        URL url = new URL(COINGECKO_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            JSONObject json = new JSONObject(in.readLine());
            return new BigDecimal(json.getJSONObject("bitcoin").getString("usd"));
        }
    }

    private static BigDecimal getPriceFromCryptocompare() throws Exception {
        logURL(CRYPTOCOMPARE_API_URL);
        URL url = new URL(CRYPTOCOMPARE_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            JSONObject json = new JSONObject(response.toString());
            return new BigDecimal(json.getDouble("USD"));
        }
    }

    private static BigDecimal getPriceFromGemini() throws Exception {
        logURL(GEMINI_API_URL);
        URL url = new URL(GEMINI_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            JSONObject json = new JSONObject(in.readLine());
            return new BigDecimal(json.getString("last"));
        }
    }

    private static BigDecimal getPriceFromKraken() throws Exception {
        logURL(KRAKEN_API_URL);
        URL url = new URL(KRAKEN_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            JSONObject json = new JSONObject(in.readLine());
            JSONObject result = json.getJSONObject("result");
            String pair = result.keys().next();
            return new BigDecimal(result.getJSONObject(pair).getJSONArray("c").getString(0));
        }
    }

    // Helper method to log the URL that is being called
    private static void logURL(String url) {
        System.out.println("Calling API URL: " + url);
    }

    /**
     * Returns a list of supported market data sources by inspecting the available getPriceFrom* methods.
     *
     * @return A List of strings representing the names of the market data sources.
     */
    public static List<String> getConfiguredMarketDataSources() {
        List<String> sources = new ArrayList<>();
        try {
            // Get all declared methods in BitcoinPriceWrapper
            Method[] methods = BitcoinPriceWrapper.class.getDeclaredMethods();
            for (Method method : methods) {
                // Look for methods starting with "getPriceFrom"
                if (method.getName().startsWith("getPriceFrom")) {
                    // Extract the exchange name (remove "getPriceFrom" prefix)
                    String exchange = method.getName().substring("getPriceFrom".length());
                    // Convert to lowercase to match getPrice switch cases
                    sources.add(exchange.toLowerCase());
                }
            }
        } catch (Exception e) {
            System.err.println("Error inspecting BitcoinPriceWrapper methods: " + e.getMessage());
            e.printStackTrace();
        }
        // Return sorted list
        return sources.stream().sorted().collect(Collectors.toList());
    }

    public static void main(String[] args) {
        try {
            List<String> exchanges = getConfiguredMarketDataSources();

            for (String exchange : exchanges) {
                String price = getPrice(exchange);
                System.out.println(exchange + " Price: " + price);
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}