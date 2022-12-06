package com.uboz.arfaces

import android.os.Bundle
import android.view.View
import com.google.ar.core.*
import com.google.ar.sceneform.ux.ArFragment
import java.util.*


class FaceArFragment : ArFragment() {

    var onReadySession: (Session?) -> Unit = {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        /*
        val a = floatArrayOf(10f,0f,0f,90f)
        val cameraView = FloatArray(16)
        cameraView.set(2, 1.3f)
        arSceneView.arFrame?.camera?.pose?.inverse()?.toMatrix(cameraView, 2)
        this.arSceneView.arFrame?.camera?.displayOrientedPose?.extractRotation()?.toMatrix(a,0)
        */
    }

    fun getSession(): Session? {
        return this.arSceneView.session
    }


    override fun getSessionFeatures(): MutableSet<Session.Feature> {
        return EnumSet.of(Session.Feature.FRONT_CAMERA)
    }


    override fun getSessionConfiguration(session: Session?): Config {
        val config = Config(session)
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        this.onReadySession?.invoke(session)
        return config
    }


}