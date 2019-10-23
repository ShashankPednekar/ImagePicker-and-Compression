package com.shashankpednekar.imagepickercompression

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

private const val REQ_CAPTURE = 100

class MainActivity : ParentActivity(R.layout.activity_main) {
    val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btn_capture.setOnClickListener {
            if (isPermissionsAllowed(permissions, true, REQ_CAPTURE)) {
                chooseImage()
            }
        }
    }

    private fun chooseImage() {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_CAPTURE -> {
                if (isAllPermissionsGranted(grantResults)) {
                    chooseImage()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.permission_not_granted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
