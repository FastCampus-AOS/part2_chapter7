package fastcampus.aos.part2.part2_chapter7

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import fastcampus.aos.part2.part2_chapter7.databinding.ActivityMainBinding
import fastcampus.aos.part2.part2_chapter7.databinding.ItemForecastBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                updateLocation()
            }
            else -> {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))

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

    private fun updateLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {

            Thread {
                try {
                    val addressList = Geocoder(this, Locale.KOREA).getFromLocation(it.latitude, it.longitude, 1)
                    Log.e("hyunsu addressList", addressList.toString())

                    runOnUiThread {
                        binding.locationTextView.text = addressList?.get(0)?.locality.orEmpty()
                        Log.e("hyunsu addressList?.get(0)?.thoroughfare", addressList?.get(0)?.locality.orEmpty().toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            val retrofit = Retrofit.Builder()
                .baseUrl("http://apis.data.go.kr/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherService::class.java)
            val baseDateTime = BaseDateTime.getBaseDateTime()
            val converter = GeoPointConverter()
            val point = converter.convert(it.latitude, it.longitude)
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

                    val currentForecast = list.first()

                    Log.e("hyunsu raw response", response.body().toString())
                    Log.e("hyunsu forecast list size", list.size.toString())

                    binding.temperatureTextView.text = getString(R.string.temperature_text, currentForecast.temperature)
                    binding.skyTextView.text = currentForecast.weather
                    binding.precipitationTextView.text = getString(R.string.precipitation_text, currentForecast.precipitation)

                    binding.childForecastLayout.apply {
                        list.forEachIndexed { index, forecast ->
                            if (index == 0) { return@forEachIndexed }

                            val itemView = ItemForecastBinding.inflate(layoutInflater, this, false)
                            Log.e("hyunsu itemView test", itemView.toString())

                            itemView.timeTextView.text = forecast.forecastTime
                            itemView.weatherTextView.text = forecast.weather
                            itemView.temperatureTextView.text = getString(R.string.temperature_text, forecast.temperature)

                            addView(itemView.root)
                            Log.e("hyunsu addView", "아이템 추가")
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherEntity?>, t: Throwable) {}
            })
        }
    }

    companion object {
        const val DEC_KEY =
            "dNMWWuDj2dDf%2BaUjZqDW6CDkSuj9eHpUek9kD7QFQXw9VQdmfBH2%2BpLpv4d4xQaRl%2FrRH7hI8hAVZXWvTrmZkg%3D%3D"
    }
}