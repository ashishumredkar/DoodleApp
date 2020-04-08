package sample.app.doodleappassignment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.DriveScopes
import sample.app.doodleappassignment.utils.DriveServiceHelper
import java.io.File
import java.util.*


/**
 * Created by Ashish on 4/6/2020.
 */
class ImageUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private var mDriveServiceHelper: DriveServiceHelper? = null

    override fun doWork(): Result {
        val taskData = inputData

        val imagePath = taskData.getString(EXTRA_TEXT)
        Log.e("imagePath", "" + imagePath!!)

        val sentValue =  uploadFile(imagePath);

        val taskDataString = taskData.getString(MainActivity.MESSAGE_STATUS)
        showNotification("WorkManager", taskDataString ?: sentValue)
        val outputData = Data.Builder().putString(WORK_RESULT, ""+sentValue).build()
        return Result.success(outputData)
    }

    private fun uploadFile(imagePath: String) :String {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        var returnValue = "File Sending"
        if (account == null) {
            //requestUserSignIn()
        } else {

            val credential = GoogleAccountCredential.usingOAuth2(
                    applicationContext, Collections.singleton(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = account.account
            val googleDriveService = com.google.api.services.drive.Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential)
                    .setApplicationName("AppName")
                    .build()
            mDriveServiceHelper = DriveServiceHelper(googleDriveService)

            //upload file
           //get the file from path
            val file = File(imagePath)

                mDriveServiceHelper!!.uploadFile(file, "image/jpeg", null)
                        .addOnSuccessListener({ fileId ->
                            Log.e("fileId", "" + fileId.getName())
                            returnValue= "File saved";

                            showNotification("WorkManager", returnValue ?: returnValue)

                            // Toast.makeText(applicationContext, "File saved", Toast.LENGTH_LONG).show()
                        })
                        .addOnFailureListener({ exception ->{
                            showNotification("WorkManager", "File saved")
                            returnValue= "File saved";
                            Log.e("ASHISH", "Unable to save file via REST.", exception)
                        }
                        })



        }
       return returnValue;

    }

    private fun showNotification(task: String, desc: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_channel"
        val channelName = "task_name"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle(task)
                .setContentText(desc)
                .setSmallIcon(R.mipmap.ic_launcher)
        manager.notify(1, builder.build())
    }

    companion object {
        private val WORK_RESULT = "work_result"
        val EXTRA_TEXT = "text"
    }


}