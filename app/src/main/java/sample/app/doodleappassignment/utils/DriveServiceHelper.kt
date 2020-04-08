package sample.app.doodleappassignment.utils

import android.content.Intent
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Created by Ashish on 4/7/2020.
 */
/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
class DriveServiceHelper(private val mDriveService: Drive) {
    private val mExecutor = Executors.newSingleThreadExecutor()



    /**
     * Returns an [Intent] for opening the Storage Access Framework file picker.
     */
    fun createFilePickerIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"

        return intent
    }


    fun uploadFile(localFile: java.io.File, mimeType: String, folderId: String?): Task<GoogleDriveFileHolder> {
        return Tasks.call(mExecutor, Callable {
            // Retrieve the metadata as a File object.

            val root: List<String>
            if (folderId == null) {
                root = listOf("root")
            } else {

                root = listOf(folderId)
            }

            val metadata = File()
                    .setParents(root)
                    .setMimeType(mimeType)
                    .setName(localFile.name)

            val fileContent = FileContent(mimeType, localFile)

            val fileMeta = mDriveService.files().create(metadata, fileContent).execute()
            val googleDriveFileHolder = GoogleDriveFileHolder()
            googleDriveFileHolder.id = fileMeta.id
            googleDriveFileHolder.name = fileMeta.name
            googleDriveFileHolder
        })
    }
}