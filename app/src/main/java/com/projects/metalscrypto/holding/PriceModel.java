package com.projects.metalscrypto.holding;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public  class PriceModel {


    @Expose
    @SerializedName("rates")
    private RatesEntity rates;
    @Expose
    @SerializedName("base")
    private String base;
    @Expose
    @SerializedName("timestamp")
    private int timestamp;
    @Expose
    @SerializedName("success")
    private boolean success;
    @Expose
    @SerializedName("error")
    private ErrorEntity error;

    public ErrorEntity getError() {
        return error;
    }

    public void setError(ErrorEntity error) {
        this.error = error;
    }

    public RatesEntity getRates() {
        return rates;
    }

    public void setRates(RatesEntity rates) {
        this.rates = rates;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static class ErrorEntity {
        @Expose
        @SerializedName("statusCode")
        private int statusCode;
        @Expose
        @SerializedName("message")
        private String message;

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class RatesEntity {
        @Expose
        @SerializedName("AUDXAG")
        private double audxag;
        @Expose
        @SerializedName("AUDXAU")
        private double audxau;
        @Expose
        @SerializedName("AUDXCU")
        private double audxcu;
        @Expose
        @SerializedName("AUDXPT")
        private double audxpt;
        @Expose
        @SerializedName("AUDXPD")
        private double audxpd;
        @Expose
        @SerializedName("AUDXLD")
        private double audxld;
        @Expose
        @SerializedName("AUDXAL")
        private double audxal;
        @Expose
        @SerializedName("AUDXNI")
        private double audxni;
        @Expose
        @SerializedName("AUDXZN")
        private double audxzn;
        @Expose
        @SerializedName("BTC")
        private double btc;
        @Expose
        @SerializedName("ETH")
        private double eth;
        @Expose
        @SerializedName("XAG")
        private double xag;
        @Expose
        @SerializedName("XAU")
        private double xau;
        @Expose
        @SerializedName("XCU")
        private double xcu;
        @Expose
        @SerializedName("XPT")
        private double xpt;
        @Expose
        @SerializedName("XPD")
        private double xpd;
        @Expose
        @SerializedName("XLD")
        private double xld;
        @Expose
        @SerializedName("XAL")
        private double xal;
        @Expose
        @SerializedName("XNI")
        private double xni;
        @Expose
        @SerializedName("XZN")
        private double xzn;

        public double getAudxag() {
            return audxag;
        }

        public void setAudxag(double audxag) {
            this.audxag = audxag;
        }

        public double getAudxau() {
            return audxau;
        }

        public void setAudxau(double audxau) {
            this.audxau = audxau;
        }

        public double getAudxcu() {
            return audxcu;
        }

        public void setAudxcu(double audxcu) {
            this.audxcu = audxcu;
        }

        public double getXag() {
            return xag;
        }

        public void setXag(double xag) {
            this.xag = xag;
        }

        public double getXau() {
            return xau;
        }

        public void setXau(double xau) {
            this.xau = xau;
        }

        public double getXcu() {
            return xcu;
        }

        public void setXcu(double xcu) {
            this.xcu = xcu;
        }

        public double getAudxpt() {
            return audxpt;
        }

        public void setAudxpt(double audxpt) {
            this.audxpt = audxpt;
        }

        public double getAudxpd() {
            return audxpd;
        }

        public void setAudxpd(double audxpd) {
            this.audxpd = audxpd;
        }

        public double getAudxld() {
            return audxld;
        }

        public void setAudxld(double audxld) {
            this.audxld = audxld;
        }

        public double getAudxal() {
            return audxal;
        }

        public void setAudxal(double audxal) {
            this.audxal = audxal;
        }

        public double getAudxni() {
            return audxni;
        }

        public void setAudxni(double audxni) {
            this.audxni = audxni;
        }

        public double getAudxzn() {
            return audxzn;
        }

        public void setAudxzn(double audxzn) {
            this.audxzn = audxzn;
        }

        public double getBtc() {
            return btc;
        }

        public void setBtc(double btc) {
            this.btc = btc;
        }

        public double getEth() {
            return eth;
        }

        public void setEth(double eth) {
            this.eth = eth;
        }

        public double getXpt() {
            return xpt;
        }

        public void setXpt(double xpt) {
            this.xpt = xpt;
        }

        public double getXpd() {
            return xpd;
        }

        public void setXpd(double xpd) {
            this.xpd = xpd;
        }

        public double getXld() {
            return xld;
        }

        public void setXld(double xld) {
            this.xld = xld;
        }

        public double getXal() {
            return xal;
        }

        public void setXal(double xal) {
            this.xal = xal;
        }

        public double getXni() {
            return xni;
        }

        public void setXni(double xni) {
            this.xni = xni;
        }

        public double getXzn() {
            return xzn;
        }

        public void setXzn(double xzn) {
            this.xzn = xzn;
        }
    }
}
