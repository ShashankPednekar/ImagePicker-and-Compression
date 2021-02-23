package com.shashankpednekar.imagepickercompression.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.shashankpednekar.imagepickercompression.BuildConfig
import com.shashankpednekar.imagepickercompression.R
import com.shashankpednekar.imagepickercompression.databinding.ActivityMainBinding
import com.shashankpednekar.imagepickercompression.utils.compressImageFile
import kotlinx.coroutines.*
import java.io.File
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private var queryImageUrl: String = ""
    private var imgPath: String = ""
    private var imageUri: Uri? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnCapture.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    chooseImage()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showRationale()
                else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }


    private fun chooseImage() {
        launchIntent.launch(getPickImageIntent())
    }

    private fun getPickImageIntent(): Intent? {
        var chooserIntent: Intent? = null

        var intentList: MutableList<Intent> = ArrayList()

        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri())

        intentList = addIntentsToList(this, intentList, pickIntent)
        intentList = addIntentsToList(this, intentList, takePhotoIntent)

        if (intentList.size > 0) {
            chooserIntent = Intent.createChooser(
                intentList.removeAt(intentList.size - 1),
                getString(R.string.select_capture_image)
            )
            chooserIntent!!.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                intentList.toTypedArray<Parcelable>()
            )
        }

        return chooserIntent
    }

    private fun setImageUri(): Uri {
        val folder = File("${getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
        folder.mkdirs()

        val file = File(folder, "Image_Tmp.jpg")
        if (file.exists())
            file.delete()
        file.createNewFile()
        imageUri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + getString(R.string.file_provider_name),
            file
        )
        imgPath = file.absolutePath
        return imageUri!!
    }

    private fun addIntentsToList(
        context: Context,
        list: MutableList<Intent>,
        intent: Intent
    ): MutableList<Intent> {
        val resInfo = context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resInfo) {
            val packageName = resolveInfo.activityInfo.packageName
            val targetedIntent = Intent(intent)
            targetedIntent.setPackage(packageName)
            list.add(targetedIntent)
        }
        return list
    }

    private fun handleImageRequest(data: Intent?) {
        val exceptionHandler = CoroutineExceptionHandler { _, t ->
            t.printStackTrace()
            binding.progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                t.localizedMessage ?: getString(R.string.some_err),
                Toast.LENGTH_SHORT
            ).show()
        }

        GlobalScope.launch(Dispatchers.Main + exceptionHandler) {
            binding.progressBar.visibility = View.VISIBLE

            if (data?.data != null) {     //Photo from gallery
                imageUri = data.data
                queryImageUrl = imageUri?.path!!
                queryImageUrl = compressImageFile(queryImageUrl, false, imageUri!!)
            } else {
                queryImageUrl = imgPath
                compressImageFile(queryImageUrl, uri = imageUri!!)
            }
            imageUri = Uri.fromFile(File(queryImageUrl))

            if (queryImageUrl.isNotEmpty()) {

                Glide.with(this@MainActivity)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .load(queryImageUrl)
                    .into(binding.ivImg)
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showRationale() {
        AlertDialog.Builder(this)
                .setMessage("Permission required for Camera")
                .setPositiveButton(
                        "Allow"
                ) { dialog, button ->
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
                .setNegativeButton(
                        "Deny"
                ) { dialog, button -> }
                .show()
    }

    private val requestCameraPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    chooseImage()
                }
            }

    private val launchIntent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    handleImageRequest(result.data)
                }
            }

}
