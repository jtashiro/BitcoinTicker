package com.fiospace.bitcointicker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

import org.json.JSONObject;

public class BitcoinPriceWrapper {

    private static final String COINDESK_API_URL = "https://api.coindesk.com/v1/bpi/currentprice.json";
    private static final String CRYPTOCOMPARE_API_URL = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD";
    private static final String COINCAP_API_URL = "https://api.coincap.io/v2/assets/bitcoin";

    public static String getPrice(String exchange) throws Exception {
        BigDecimal price = null;

        switch (exchange.toLowerCase()) {
            case "coindesk":
                price = getPriceFromCoinDesk();
                break;
            case "cryptocompare":
                price = getPriceFromCryptoCompare();
                break;
            case "coincap":
                price = getPriceFromCoinCap();
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

    private static BigDecimal getPriceFromCoinDesk() throws Exception {
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

    private static BigDecimal getPriceFromCryptoCompare() throws Exception {
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

    private static BigDecimal getPriceFromCoinCap() throws Exception {
        URL url = new URL(COINCAP_API_URL);
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
            return new BigDecimal(data.getString("priceUsd"));
        }
    }

    public static void main(String[] args) {
        try {
            String[] exchanges = {"coindesk", "cryptocompare", "coincap"};

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