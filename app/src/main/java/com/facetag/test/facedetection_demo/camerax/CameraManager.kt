package com.facetag.test.facedetection_demo.camerax

import android.annotation.SuppressLint
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.ScaleGestureDetector
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facetag.test.facedetection_demo.face_detection.FaceContourDetectionProcessor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val finderView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val graphicOverlay: GraphicOverlay
) {

    private var preview: Preview? = null

    private var camera: Camera? = null
    lateinit var cameraExecutor: ExecutorService
    private var cameraSelectorOption = CameraSelector.LENS_FACING_FRONT
    private var cameraProvider: ProcessCameraProvider? = null


    lateinit var imageCapture: ImageCapture
    lateinit var metrics: DisplayMetrics
    var rotation: Float = 0f

    private var imageAnalyzer: ImageAnalysis? = null


    init {
        createNewExecutor()
    }

    private fun createNewExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    //손가락을 통한 화면 줌인앤아웃
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPinchToZoom() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1F
                val delta = detector.scaleFactor
                camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(context, listener)
        finderView.setOnTouchListener { _, event ->
            finderView.post {
                scaleGestureDetector.onTouchEvent(event)
            }
            return@setOnTouchListener true
        }
    }


    fun startCamera() {
        // 카메라의 생명 주기를 바인딩하는 데 사용
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            Runnable {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder()
                    .build()

                imageAnalyzer = ImageAnalysis.Builder()
                //카메라가 너무 빠르게 움직이는 경우 이미지 분석기가 일부 이미지 프레임을 건너 띔 해당 프레임을 건너 뛰지 않으면 앱이 지연되는 것처럼 보임 라이브 제공할 때 사용(STRATEGY_KEEP_ONLY_LATEST)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, selectAnalyzer())
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraSelectorOption)
                    .build()

                metrics =  DisplayMetrics().also { finderView.display.getRealMetrics(it) }

                imageCapture =
                    ImageCapture.Builder()
                        .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
                        .build()


                setUpPinchToZoom()
                setCameraConfig(cameraProvider, cameraSelector)

            }, ContextCompat.getMainExecutor(context)
        )
    }

    private fun selectAnalyzer(): ImageAnalysis.Analyzer {
        return FaceContourDetectionProcessor(graphicOverlay)
    }

    private fun setCameraConfig(
        cameraProvider: ProcessCameraProvider?,
        cameraSelector: CameraSelector
    ) {
        try {
            // 바인딩을 다시 사용하기전에 바인딩을 해제
            cameraProvider?.unbindAll()
            // 바인딩된 카메라 사용 카메라 사용시 화면전환이후 처리 방법
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer,
                imageCapture
            )
            // 미리보기 구현
            preview?.setSurfaceProvider(
                finderView.surfaceProvider
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    fun isHorizontalMode() : Boolean {
        return rotation == 90f || rotation == 270f
    }

    fun isFrontMode() : Boolean {
        return cameraSelectorOption == CameraSelector.LENS_FACING_FRONT
    }

    //안드로이드 기기 카메라 앞/뒤 전환
    fun changeCameraSelector() {
        cameraProvider?.unbindAll()
        cameraSelectorOption =
            if (cameraSelectorOption == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
            else CameraSelector.LENS_FACING_BACK
        graphicOverlay.toggleSelector()
        startCamera()
    }

    companion object {
        private const val TAG = "CameraXBasic"
    }

}