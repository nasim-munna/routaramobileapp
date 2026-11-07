package com.routara.field.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.routara.field.BuildConfig
import com.routara.field.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** One fresh fix for a visit, or null if GPS can't get one in time. */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    if (!hasLocationPermission(context)) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    val location = withTimeoutOrNull(15_000) {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
    } ?: client.lastLocation.await()
    return location?.let { it.latitude to it.longitude }
}

/**
 * Shares live location while a rep is checked in, and only then: it starts
 * at check-in and stops at check-out or day end. The ongoing notification
 * keeps that visible to them the whole time.
 */
class TrackingService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var socket: WebSocket? = null
    private var loop: Job? = null
    private var callback: LocationCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        // Starting a location service without the permission crashes the
        // app on Android 14 and later.
        if (!hasLocationPermission(this)) {
            stopSelf()
            return
        }

        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        connect()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateIntervalMillis(INTERVAL_MS / 2)
            .build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                socket?.send("""{"type":"location.update","lat":${location.latitude},"lng":${location.longitude}}""")
            }
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(request, callback!!, mainLooper)

        loop = scope.launch {
            while (isActive) {
                if (socket == null) connect()
                socket?.send("""{"type":"heartbeat"}""")
                delay(HEARTBEAT_MS)
            }
        }
    }

    private fun connect() {
        val token = Session(this).access ?: return
        val request = Request.Builder().url("${BuildConfig.WS_URL}?token=$token").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = forget(webSocket)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = forget(webSocket)
        })
    }

    private fun forget(closed: WebSocket) {
        if (socket === closed) socket = null
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Live location", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Routara")
            .setContentText("Sharing your location while you're checked in")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        loop?.cancel()
        callback?.let { LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it) }
        socket?.close(1000, null)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 7
        private const val CHANNEL = "live-location"
        private const val INTERVAL_MS = 15_000L
        private const val HEARTBEAT_MS = 60_000L

        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        fun stop(context: Context) = context.stopService(Intent(context, TrackingService::class.java))
    }
}
