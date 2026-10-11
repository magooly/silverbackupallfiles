package com.projects.metalscrypto.holding.Retrofit;

import com.google.gson.JsonElement;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StockApiWebservices {
    @GET("api/real-time/{ticker}")
    Call<JsonElement> getLatestPrice(
            @Path("ticker") String ticker,
            @Query("api_token") String apiToken,
            @Query("fmt") String format);

    @GET("api/eod/{ticker}")
    Call<JsonElement> getEndOfDayPrice(
            @Path("ticker") String ticker,
            @Query("api_token") String apiToken,
            @Query("fmt") String format,
            @Query("order") String order);
}