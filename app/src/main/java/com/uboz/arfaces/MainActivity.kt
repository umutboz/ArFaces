package com.uboz.arfaces

import android.app.ActivityManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture


class MainActivity : AppCompatActivity(){
        companion object {
        private const val TAG = "ARFaces"
        const val MIN_OPENGL_VERSION = 3.0
    }
        var isAnimatingFinish : Boolean = false
        var onFinishAnimated: (String) -> Unit = {}

        private lateinit var arFragment: FaceArFragment
        var cameraManager: CameraManager? = null

        private var faceRenderable: ModelRenderable? = null
        private var faceTexture: Texture? = null
        private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

        var faceNodeFilter = HashMap<AugmentedFace, FilterFace>()
        var selectedModel : String = ""

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            if (!checkIsSupportedDeviceOrFinish()) {
                return
            }

            arFragment = fragment as FaceArFragment
            arFragment.onReadySession = { session ->
                if (session != null) {
                    val camConfig = session.cameraConfig
                }
            }

            arFragment.arSceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST
            arFragment.arSceneView.scene.addOnUpdateListener {
                /*if(faceRenderable != null && faceTexture != null) {
                    addTrackedFaces()
                    removeUntrackedFaces()
                }
                */
                if(isAnimatingFinish){
                    addTrackedFaces()
                    removeUntrackedFaces()
                }
                filterFaces()
            }
            arFragment.arSceneView.scene.setOnTouchListener { hitTestResult, motionEvent ->
                if (!faceNodeFilter.isEmpty()){
                    Log.d("hitted", "-------------------")
                }
                true
            }
            onFinishAnimated = { selectedModel ->
                Log.d("AR Faces","animation finish $selectedModel")
                //loadModel(selectedModel)
                this.selectedModel = selectedModel
                isAnimatingFinish = true
            }
        }
        private fun addTrackedFaces() {
            val scene = arFragment.arSceneView.scene
            val session = arFragment.arSceneView.session ?: return
            session.getAllTrackables(AugmentedFace::class.java).let {
                for(face in it) {
                    if (!faceNodeMap.containsKey(face)) {
                        val glassesFaceNode = GlassesFaceNode(face, this, scene,this.selectedModel)
                        glassesFaceNode.setParent(scene)
                        faceNodeMap.put(face, glassesFaceNode)
                    }
                }
            }
        }
        private fun removeUntrackedFaces() {
            val entries = faceNodeMap.entries
            for(entry in entries) {
                val face = entry.key
                if(face.trackingState == TrackingState.STOPPED) {
                    val faceNode = entry.value
                    faceNode.setParent(null)
                    entries.remove(entry)
                }
            }
        }

        private fun filterFaces() {
            val scene = arFragment.arSceneView.scene
            val session = arFragment.arSceneView.session ?: return
            session.getAllTrackables(AugmentedFace::class.java).let {
                for(face in it) {
                    if(!faceNodeFilter.containsKey(face)) {
                        val faceNode = FilterFace(face, this, scene)
                        faceNode.worldScale = Vector3(0.07f, 0.05f, 0.6f)
                        faceNode.setParent(arFragment.arSceneView.scene)
                        faceNodeFilter.put(face, faceNode)
                        startAnimation()
                    }
                }
                // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                val iterations = faceNodeFilter.entries.iterator()
                while (iterations.hasNext()) {
                    val entry = iterations.next()
                    val face = entry.key
                    if (face.trackingState == TrackingState.STOPPED) {
                        val faceNode = entry.value
                        faceNode.setParent(null)
                        iterations.remove()
                    }
                }
            }

        }
        private fun startAnimation() {
            for (face in faceNodeFilter.values) {
                face.animate()
            }
        }


        private fun checkIsSupportedDeviceOrFinish() : Boolean {
            if (ArCoreApk.getInstance().checkAvailability(this) == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
                Toast.makeText(this, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show()
                finish()
                return false
            }
            val openGlVersionString =  (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                ?.deviceConfigurationInfo
                ?.glEsVersion

            openGlVersionString?.let { s ->
                if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                    Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                        .show()
                    finish()
                    return false
                }
            }
            return true
        }

}