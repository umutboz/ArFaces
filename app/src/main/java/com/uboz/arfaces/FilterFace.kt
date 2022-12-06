package com.uboz.arfaces

import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.AugmentedFaceNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.uboz.arfaces.ARSessionSupport.PermissionFragment.Companion.TAG
import java.util.concurrent.CompletableFuture


class FilterFace(augmentedFace: AugmentedFace?,
                 val context: MainActivity, scene: Scene): AugmentedFaceNode(augmentedFace), Node.OnTapListener {

    var cardNode: Node? = null
    private var textView: TextView? = null
    private var linearLayout: LinearLayout? = null
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

    val models = arrayOf("Read", "Filter", "Glasses","Yellow_Glasses" )//  arrayOf("Dog", "Cat", "Filter","Glasses")
    val miliseconds = arrayOf(3800L ,3900L, 4000L, 4100L)// arrayOf(5000L, 5000L, 5000L, 5000L)
    override fun onActivate() {
        super.onActivate()
        cardNode = Node()
        cardNode?.setParent(this)
        cardNode?.isEnabled = true
        setOnTapListener(this)

        ViewRenderable.builder()
            .setView(context, R.layout.card_layout)
            .build()
            .thenAccept { uiRenderable: ViewRenderable ->
                uiRenderable.isShadowCaster = false
                uiRenderable.isShadowReceiver = false
                cardNode?.renderable = uiRenderable
                textView = uiRenderable.view.findViewById(R.id.title)
            }
            .exceptionally { throwable: Throwable? ->
                throw AssertionError(
                    "Could not create ui element",
                    throwable
                )
            }

    }

    override fun onUpdate(frameTime: FrameTime?) {
        super.onUpdate(frameTime)
        augmentedFace?.let { face ->
            val rightForehead = face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_RIGHT)
            val leftForehead = face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT)
            val center = face.centerPose

            if (isAndroidEmulator()) {
                cardNode?.worldPosition = Vector3(
                    (rightForehead.tx() - 0.11f),
                    (rightForehead.ty()) + 0.05f, center.tz() - 0.029f
                )
            } else {
                //device
                cardNode?.worldPosition = Vector3(
                    (leftForehead.tx() + rightForehead.tx()) / 2,
                    (leftForehead.ty() + rightForehead.ty()) / 2 + 0.05f, center.tz()
                )
            }
        }
    }

    fun animate() {
        val timer = object: CountDownTimer(2500, 100) {
            override fun onTick(millisUntilFinished: Long) {
                // do something
                textView?.text = models.random()
            }
            override fun onFinish() {
                // do something
                val selectedModel = models.random()
                textView?.text = selectedModel
                context.onFinishAnimated.invoke(selectedModel)
            }
        }
        timer.start()
    }

    fun refresh() {
        textView?.text = context.getText(R.string.select_title)
    }

    private fun isAndroidEmulator(): Boolean {
        val model = Build.MODEL
        Log.d(TAG, "model=$model")
        val product = Build.PRODUCT
        Log.d(TAG, "product=$product")
        var isEmulator = false
        if (product != null) {
            isEmulator = product == "sdk" || product.contains("_sdk") || product.contains("sdk_")
        }
        Log.d(TAG, "isEmulator=$isEmulator")
        return isEmulator
    }

    override fun onTap(p0: HitTestResult?, p1: MotionEvent?) {
        animate()
    }
}