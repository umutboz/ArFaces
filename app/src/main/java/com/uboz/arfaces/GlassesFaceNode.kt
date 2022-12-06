package com.uboz.arfaces


import android.opengl.Matrix.rotateM
import android.os.Build
import android.util.Log
import com.google.ar.core.AugmentedFace
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.AugmentedFaceNode



class GlassesFaceNode(augmentedFace: AugmentedFace?,
val context: MainActivity, scene: Scene, val selectedModel:String
): AugmentedFaceNode(augmentedFace) {

    var node: Node? = null
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

    override fun onActivate() {
        super.onActivate()
        node = Node()
        node?.setParent(this)
        node?.isEnabled = true

        var modelResourceId = 0
        when(selectedModel){
            "Read" -> modelResourceId = R.raw.read_glasses
            //"Cat" -> modelResourceId = R.raw.cat
            "Filter" -> modelResourceId = R.raw.sunglasses
            //"Frog" -> modelResourceId = R.raw.frog
            //"Zebra" -> modelResourceId = R.raw.zebra
            //"Monkey" -> modelResourceId = R.raw.monkey
            "Glasses" -> modelResourceId = R.raw.sunglasses
            "Yellow_Glasses" -> modelResourceId = R.raw.yellow_sunglasses
        }


        ModelRenderable.builder()
            .setSource(this.context, modelResourceId)
            .build()
            .thenAccept { renderable: ModelRenderable ->
                renderable.isShadowCaster = false
                renderable.isShadowReceiver = false
                node?.renderable = renderable
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
            //val uvs = face.meshVertices

            if (selectedModel == "Read"){
                node?.localRotation =  Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), 180f)
                //animation
                //val q1: Quaternion = node?.localRotation!!
                //val q2 = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -2f)
                //var rotationAnimation = Quaternion.multiply(q1, q2)
                //node?.localRotation = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), -5.0f)
                val glassesPositionVector = Vector3(
                    (rightForehead.tx() - 0.1f),
                    (rightForehead.ty()) - 0.022f, center.tz() - 0.01f)
                val glassesScaleVector = Vector3(0.74f, 0.66f, 0.20f)
                if (isAndroidEmulator()) {
                    node?.worldScale = glassesScaleVector
                    node?.worldPosition = glassesPositionVector
                }
            }else{
                val glassesPositionVector = Vector3(
                    (rightForehead.tx() - 0.1f),
                    (rightForehead.ty()) - 0.0222f, center.tz() - 0.01f)
                val glassesScaleVector = Vector3(0.74f, 0.66f, 0.20f)
                if (isAndroidEmulator()) {
                    node?.worldScale = glassesScaleVector
                    node?.worldPosition = glassesPositionVector
                } else {
                    //device
                    node?.worldPosition = Vector3(
                        (leftForehead.tx() + rightForehead.tx()) / 2,
                        (leftForehead.ty() + rightForehead.ty()) / 2 + 0.05f, center.tz()
                    )
                }
            }
        }
    }


    private fun isAndroidEmulator(): Boolean {
        val model = Build.MODEL
        Log.d(ARSessionSupport.PermissionFragment.TAG, "model=$model")
        val product = Build.PRODUCT
        Log.d(ARSessionSupport.PermissionFragment.TAG, "product=$product")
        var isEmulator = false
        if (product != null) {
            isEmulator = product == "sdk" || product.contains("_sdk") || product.contains("sdk_")
        }
        Log.d(ARSessionSupport.PermissionFragment.TAG, "isEmulator=$isEmulator")
        return isEmulator
    }
}