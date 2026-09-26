package com.projects.metalscrypto.holding.Retrofit;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CryptoApiWebservices {
    @GET("api/v3/simple/price")
    Call<JsonObject> getCryptoPrices(@Query("ids") String ids, @Query("vs_currencies") String vsCurrencies);
}

