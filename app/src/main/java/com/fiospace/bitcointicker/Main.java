package com.fiospace.bitcointicker;

public class Main {
    private double temp;
    private int pressure;
    private int humidity;
    private double temp_min;
    private double temp_max;

    // Getters and setters
    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public int getPressure() {
        return pressure;
    }

    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getTempMin() {
        return temp_min;
    }

    public void setTempMin(double temp_min) {
        this.temp_min = temp_min;
    }

    public double getTempMax() {
        return temp_max;
    }

    public void setTempMax(double temp_max) {
        this.temp_max = temp_max;
    }
}
