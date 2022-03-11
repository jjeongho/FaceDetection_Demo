package com.facetag.test.facedetection_demo.Activity


import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facetag.test.facedetection_demo.camerax.CameraManager
import com.facetag.test.facedetection_demo.databinding.ActivityMainBinding
import com.facetag.test.facedetection_demo.util.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        createCameraManager()
        checkForPermission()
        CameraSwitchClicks()
        takeCapture()
    }
    //권한 설정 체크
    private fun checkForPermission() {
        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun CameraSwitchClicks() {
        binding.cameraSwitch.setOnClickListener {
            cameraManager.changeCameraSelector()
        }
    }

    private fun takeCapture() {
        binding.imageCaptureButton.setOnClickListener {
            Toast.makeText(this, "take a picture!", Toast.LENGTH_SHORT).show()
            setOrientationEvent()

            cameraManager.imageCapture.takePicture(
                cameraManager.cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    @SuppressLint("UnsafeOptInUsageError")
                    override fun onCaptureSuccess(image: ImageProxy) {
                        image.image?.let {
                            imageToBitmapSaveGallery(it)
                        }
                        super.onCaptureSuccess(image)
                    }
                })
        }
    }

    //이미지 사진과 얼굴 랜드마크 까지 같이 저장
    private fun imageToBitmapSaveGallery(image: Image) {
        image.imageToBitmap()
            ?.rotateFlipImage(
                cameraManager.rotation,
                cameraManager.isFrontMode()
            )
            ?.scaleImage(
                binding.previewViewFinder,
                cameraManager.isHorizontalMode()
            )
            ?.let { bitmap ->
                binding.graphicOverlayFinder.processCanvas.drawBitmap(
                    bitmap,
                    0f,
                    bitmap.getBaseYByView(
                        binding.graphicOverlayFinder,
                        cameraManager.isHorizontalMode()
                    ),
                    Paint().apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                    })
                binding.graphicOverlayFinder.processBitmap.saveToGallery(this@MainActivity)
            }
    }

    //랜드마크가 좌우반전 때문에 좌우반전 보정
    private fun setOrientationEvent() {
        val orientationEventListener = object : OrientationEventListener(this as Context) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation: Float = when (orientation) {
                    in 45..134 -> 270f
                    in 135..224 -> 180f
                    in 225..314 -> 90f
                    else -> 0f
                }
                cameraManager.rotation = rotation
            }
        }
        orientationEventListener.enable()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraManager.startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun createCameraManager() {
        cameraManager = CameraManager(
            this,
            binding.previewViewFinder,
            this,
            binding.graphicOverlayFinder
        )
    }
    //모든 권한이 사용 부여 되었는지 확인
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }

}