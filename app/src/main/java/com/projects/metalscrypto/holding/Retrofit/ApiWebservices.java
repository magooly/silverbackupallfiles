package com.projects.metalscrypto.holding.Retrofit;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface ApiWebservices {
    @GET("v1/prices")
    Call<JsonObject> getPriceRes(@Header("Authorization") String authorization);

    @GET("v1/currency/")
    Call<JsonObject> getCurrencyRates(@Header("Authorization") String authorization);
}