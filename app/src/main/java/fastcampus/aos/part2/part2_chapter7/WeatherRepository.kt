package fastcampus.aos.part2.part2_chapter7

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.collections.forEach
import kotlin.collections.orEmpty

object WeatherRepository {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://apis.data.go.kr/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(WeatherService::class.java)

    fun getVillageForecast(
        latitude: Double,
        longitude: Double,
        successCallback: (List<Forecast>) -> Unit,
        failureCallback: (Throwable) -> Unit
    ) {
        val baseDateTime = BaseDateTime.getBaseDateTime()
        val converter = GeoPointConverter()
        val point = converter.convert(latitude, longitude)
        Log.e("hyunsu point", point.toString())
        service.getVillageForecast(
            serviceKey = DEC_KEY,
            baseDate = baseDateTime.baseDate,
            baseTime = baseDateTime.baseTime,
            nx = point.nx,
            ny = point.ny
        ).enqueue(object : Callback<WeatherEntity> {
            override fun onResponse(
                call: Call<WeatherEntity?>,
                response: Response<WeatherEntity?>
            ) {

                val forecastDateTimeMap = mutableMapOf<String, Forecast>()
                val forecastList = response.body()?.response?.body?.items?.forecastEntities.orEmpty()
                forecastList.forEach { forecast ->
                    if (forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] == null) {
                        forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] =
                            Forecast(
                                forecastDate = forecast.forecastDate,
                                forecastTime = forecast.forecastTime
                            )
                    }

                    forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"]?.apply {
                        when (Category.from(forecast.category)) {
                            Category.POP -> precipitation = forecast.forecastValue.toInt()
                            Category.PTY -> precipitationType = transformRainType(forecast)
                            Category.SKY -> sky = transformSky(forecast)
                            Category.TMP -> temperature = forecast.forecastValue.toDouble()
                            else -> {}
                        }
                    }
                }

                val list = forecastDateTimeMap.values.toMutableList()
                list.sortWith { f1, f2 ->
                    val f1DataTime = "${f1.forecastDate}${f1.forecastTime}"
                    val f2DateTime = "${f2.forecastDate}${f2.forecastTime}"

                    return@sortWith f1DataTime.compareTo(f2DateTime)
                }

                if (list.isEmpty()) {
                    failureCallback(NullPointerException())
                } else {
                    successCallback(list)
                }
            }

            override fun onFailure(call: Call<WeatherEntity?>, t: Throwable) {
                failureCallback(t)
            }
        })
    }

    private fun transformRainType(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            0 -> "없음"
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            4 -> "소나기"
            else -> ""
        }
    }

    private fun transformSky(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            1 -> "맑음"
            3 -> "구름 많음"
            4 -> "흐림"
            else -> ""
        }
    }

    const val DEC_KEY =
        "dNMWWuDj2dDf%2BaUjZqDW6CDkSuj9eHpUek9kD7QFQXw9VQdmfBH2%2BpLpv4d4xQaRl%2FrRH7hI8hAVZXWvTrmZkg%3D%3D"
}