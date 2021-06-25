package app.ifnyas.prambors

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import co.mobiwise.library.RadioListener
import co.mobiwise.library.RadioManager
import com.google.android.material.button.MaterialButton
import androidx.media.app.NotificationCompat as MediaNotificationCompat


class MainActivity : AppCompatActivity() {

    private lateinit var btnPlayer: MaterialButton
    private lateinit var imgPlayer: AppCompatImageView
    private lateinit var imgAlbum: AppCompatImageView
    private lateinit var animatorSet: AnimatorSet
    private lateinit var radioManager: RadioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initFun()
    }

    override fun onStart() {
        super.onStart()
        radioManager.connect()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val act = intent?.extras?.getString("act")
        if (act == "stop") radioManager.stopRadio()
        finish()
    }

    override fun onDestroy() {
        radioManager.disconnect()
        super.onDestroy()
    }

    private fun initFun() {
        initRadio()
        initView()
        initNotify()
    }

    private fun initNotify() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    packageName,
                    getString(R.string.app_name),
                    IMPORTANCE_DEFAULT
                ).apply {
                    enableVibration(false)
                    setShowBadge(false)
                    setSound(null, null)
                }
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).putExtra("act", "stop"),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val remoteViews = RemoteViews(packageName, R.layout.notification_small).apply {
            setOnClickPendingIntent(R.id.btn_notification_stop, stopIntent)
        }

        notificationBuilder = NotificationCompat.Builder(this, packageName)
            .setStyle(MediaNotificationCompat.MediaStyle())
            .setSmallIcon(R.drawable.ic_outline_radio_24)
            .setVisibility(VISIBILITY_PUBLIC)
            .setCustomContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            .setOngoing(true)
            .setSound(null)
    }

    private fun initView() {
        // night
        setDefaultNightMode(MODE_NIGHT_YES)

        // find
        btnPlayer = findViewById(R.id.btn_player)
        imgPlayer = findViewById(R.id.img_player)
        imgAlbum = findViewById(R.id.img_album)

        // set
        animatorSet = AnimatorSet().apply { play(rotate(imgAlbum)) }
        btnPlayer.setOnClickListener { playerToggle() }
    }

    private fun initRadio() {
        radioManager = RadioManager.with(this).apply {
            enableNotification(false)
            registerListener(object : RadioListener {
                override fun onRadioConnected() {
                    //
                }

                override fun onRadioStarted() {
                    Handler(Looper.getMainLooper()).post {
                        notificationManager.notify(0, notificationBuilder.build())
                        if (animatorSet.isPaused) animatorSet.resume()
                        else animatorSet.start()
                    }
                }

                override fun onRadioStopped() {
                    Handler(Looper.getMainLooper()).post {
                        notificationManager.cancelAll()
                        animatorSet.pause()
                    }
                }

                override fun onMetaDataReceived(p0: String?, p1: String?) {
                    //
                }
            })
        }
    }

    private fun playerToggle() {
        if (radioManager.isPlaying) radioManager.stopRadio()
        else radioManager.startRadio(getString(R.string.stream_url))

        imgPlayer.setImageResource(
            if (radioManager.isPlaying) R.drawable.ic_round_play_arrow_24
            else R.drawable.ic_round_pause_24
        )
    }

    private fun rotate(view: View) = ObjectAnimator.ofFloat(
        view, "rotation", 0f, 360f
    ).apply {
        duration = 10000
        repeatCount = ObjectAnimator.INFINITE
        interpolator = LinearInterpolator()
    }
}