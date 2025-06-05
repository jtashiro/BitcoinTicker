package com.fiospace.bitcointicker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;

public class BitcoinPriceWrapper {

    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";
    private static final String BITFINEX_API_URL = "https://api-pub.bitfinex.com/v2/tickers?symbols=tBTCUSD";
    private static final String BITSTAMP_API_URL = "https://www.bitstamp.net/api/v2/ticker/btcusd";
    private static final String COINDESK_API_URL = "https://api.coindesk.com/v1/bpi/currentprice.json";
    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2/prices/spot?currency=USD";
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd";
    private static final String CRYPTOCOMPARE_API_URL = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD";
    private static final String GEMINI_API_URL = "https://api.gemini.com/v1/pubticker/btcusd";
    private static final String KRAKEN_API_URL = "https://api.kraken.com/0/public/Ticker?pair=XXBTZUSD";

    public static String getPrice(String exchange) throws Exception {
        BigDecimal price = null;

        switch (exchange.toLowerCase()) {
            case "binance":
                price = getPriceFromBinance();
                break;
            case "bitfinex":
                price = getPriceFromBitfinex();
                break;
            case "bitstamp":
                price = getPriceFromBitstamp();
                break;
            case "coindesk":
                price = getPriceFromCoinDesk();
                break;
            case "coinbase":
                price = getPriceFromCoinbase();
                break;
            case "coingecko":
                price = getPriceFromCoinGecko();
                break;
            case "cryptocompare":
                price = getPriceFromCryptoCompare();
                break;
            case "gemini":
                price = getPriceFromGemini();
                break;
            case "kraken":
                price = getPriceFromKraken();
                break;

            default:
                throw new IllegalArgumentException("Unsupported exchange: " + exchange);
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

    private static BigDecimal getPriceFromCoinDesk() throws Exception {
        logURL(COINDESK_API_URL);
        URL url = new URL(COINDESK_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            JSONObject json = new JSONObject(response.toString());
            return new BigDecimal(json.getJSONObject("bpi").getJSONObject("USD").getString("rate_float"));
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

    private static BigDecimal getPriceFromCoinGecko() throws Exception {
        logURL(COINGECKO_API_URL);
        URL url = new URL(COINGECKO_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            JSONObject json = new JSONObject(in.readLine());
            return new BigDecimal(json.getJSONObject("bitcoin").getString("usd"));
        }
    }

    private static BigDecimal getPriceFromCryptoCompare() throws Exception {
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
     * Returns a list of supported market data sources for Bitcoin price fetching.
     *
     * @return A List of strings representing the names of the market data sources.
     */
    public static List<String> getConfiguredMarketDataSources() {
        return Arrays.asList(
                        "binance", "bitfinex", "bitstamp", "coindesk", "coinbase", "coingecko",
                        "cryptocompare", "gemini", "kraken"
                ).stream()
                .sorted()
                .collect(Collectors.toList());
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