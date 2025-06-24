package fastcampus.aos.part2.part2_chapter7

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices

class UpdateWeatherService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createChannel()
        startForeground(1, createNotification())

        val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // todo 위젝을 권한 없음 상태로 표시하고, 클릭 했을 때 권한 팝업을 얻을 수 있도록 조정
            val pendingIntent: PendingIntent = Intent(this, SettingActivity::class.java).let {
                PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE)
            }

            RemoteViews(packageName, R.layout.widget_weather).apply {
                setTextViewText(R.id.temperatureTextView, "권한 없음")
                setOnClickPendingIntent(R.id.temperatureTextView, pendingIntent)
            }.also { remoteViews ->
                val appWidgetName = ComponentName(this, WeatherAppWidgetProvider::class.java)
                appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
            }

            return super.onStartCommand(intent, flags, startId)
        }

        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
            WeatherRepository.getVillageForecast(
                longitude = it.longitude,
                latitude = it.latitude,
                successCallback = { forecastList ->
                    val pendingServiceIntent: PendingIntent = Intent(this, UpdateWeatherService::class.java)
                        .let { intent ->
                            PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)

                        }

                    val currentForecast = forecastList.first()

                    RemoteViews(packageName, R.layout.widget_weather).apply {

                        setTextViewText(
                            R.id.temperatureTextView,
                            getString(
                                R.string.temperature_text,
                                currentForecast.temperature
                            )
                        )

                        setTextViewText(
                            R.id.weatherTextView,
                            currentForecast.weather
                        )
                        setOnClickPendingIntent(R.id.temperatureTextView, pendingServiceIntent)
                    }.also { remoteViews ->
                        val appWidgetName = ComponentName(this, WeatherAppWidgetProvider::class.java)
                        appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
                    }

                    stopSelf()
                },
                failureCallback = {
                    // todo 위젯을 에러 상태로 표시
                    val pendingServiceIntent: PendingIntent = Intent(this, UpdateWeatherService::class.java)
                        .let { intent ->
                            PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)

                        }

                    RemoteViews(packageName, R.layout.widget_weather).apply {

                        setTextViewText(
                            R.id.temperatureTextView,
                            "에라"
                        )
                        setTextViewText(
                            R.id.weatherTextView,
                            ""
                        )
                        setOnClickPendingIntent(R.id.temperatureTextView, pendingServiceIntent)
                    }.also { remoteViews ->
                        val appWidgetName = ComponentName(this, WeatherAppWidgetProvider::class.java)
                        appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
                    }

                    stopSelf()
                }
            )
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createChannel() {

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            "날씨 갱신 채널",
            NotificationManager.IMPORTANCE_LOW
        )

        channel.description = "위젯을 업데이트하는 채널"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("날씨")
            .setContentText("날씨 업데이트")
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "widget_refresh_channel"
    }
}