package com.projects.metalscrypto.holding.Retrofit;


import com.projects.metalscrypto.holding.PriceModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiWebservices
{
    @GET("latest")
    Call<PriceModel> getPriceRes(@Query("api_key") String apiKey, @Query("base") String base, @Query("currencies") String currencies);
}