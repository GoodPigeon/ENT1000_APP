package com.example.intoleranser

import com.example.intoleranser.DTO.EngFoodDTO
import com.example.intoleranser.DTO.FoodDTO
import retrofit2.Call
import retrofit2.http.GET

interface GetCalls {
    @GET("GoodPigeon/FODMAP/master/fodmap_repo_NO.json")
    fun getAllFoods() : Call<List<FoodDTO>>

    @GET("oseparovic/fodmap_list/master/fodmap_repo.json")
    fun getAllFoodsEng() : Call<List<EngFoodDTO>>
}