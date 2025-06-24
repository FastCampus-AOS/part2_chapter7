package fastcampus.aos.part2.part2_chapter7

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {

    @GET("1360000/VilageFcstInfoService_2.0/getVilageFcst?dataType=JSON")
    fun getVillageForecast(
        @Query("serviceKey", encoded = true) serviceKey: String,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int
    ): Call<WeatherEntity>
}