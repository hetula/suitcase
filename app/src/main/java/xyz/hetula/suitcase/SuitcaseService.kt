/*
 * MIT License
 *
 * Copyright (c) 2017 Tuomo Heino
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package xyz.hetula.suitcase

import android.annotation.TargetApi
import android.app.*
import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SuitcaseService : Service() {
    private val suitcaseNotificationId = 44
    private val suitcaseNotificationChannelId = "suitcase_channel_id"

    private val mExecutor = Executors.newSingleThreadExecutor()
    private val mBinder = LocalBinder(this)

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mDb: WorkDatabase

    override fun onBind(intent: Intent?) = mBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onCreate() {
        super.onCreate()
        mDb = Room.databaseBuilder(applicationContext, WorkDatabase::class.java, "workday_db")
                .build()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        createService()
    }

    override fun onDestroy() {
        super.onDestroy()
        mExecutor.shutdown()
        mExecutor.awaitTermination(5, TimeUnit.SECONDS)
        if (!mExecutor.isShutdown) {
            mExecutor.shutdownNow()
        }
        stopForeground(true)
        mNotificationManager.cancelAll()
        mDb.close()
    }

    fun workdayDbSync(daoCallback: (WorkDayDao) -> Unit) {
        daoCallback(mDb.workDayDao())
    }

    fun workdayDb(daoCallback: (WorkDayDao) -> Unit) {
        mExecutor.submit {
            Log.d("Dao Thread", "Accessing Dao!")
            daoCallback(mDb.workDayDao())
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(suitcaseNotificationChannelId,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT)
        channel.setSound(null, null)
        channel.vibrationPattern = null
        mNotificationManager.createNotificationChannel(channel)
    }

    private fun createService() {
        val contentIntent = PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)
        val foreground = NotificationCompat.Builder(this, suitcaseNotificationChannelId)
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setSound(null)
                .setVibrate(null)
                .setUsesChronometer(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setSmallIcon(R.drawable.ic_workday)
                .setContentTitle(getString(R.string.working))
                .setContentIntent(contentIntent)
                .build()
        startForeground(suitcaseNotificationId, foreground)
    }

    class LocalBinder(val service: SuitcaseService) : Binder()
}
