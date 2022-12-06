package com.uboz.arfaces


/*
Copyright 2018 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

import android.content.Context
import android.util.Log
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.uboz.arfaces.utilities.CameraPermissionHelper.hasCameraPermission
import com.uboz.arfaces.utilities.CameraPermissionHelper.requestCameraPermission


/**
 * ARCore session creation is a complex flow of exception handling, permission requesting, and
 * possible downloading of the ARCore services APK.  This class encapsulates all this.
 *
 *
 * To use this class create an instance in onCreate().
 */
class ARSessionSupport(
    private val activity: FragmentActivity,
    lifecycle: Lifecycle?,
    listener: StatusChangeListener?
) :
    LifecycleObserver {
    @get:Nullable
    var session: Session? = null
        private set

    /**
     * Gets the current status of the ARCore Session.
     */
    var status: ARStatus? = null
        private set
    private var statusListener: StatusChangeListener?
    private var textureId: Int
    private var rotation: Int
    private var width: Int
    private var height: Int
    private var mUserRequestedInstall: Boolean

    init {
        setStatus(ARStatus.Uninitialized)
        lifecycle?.addObserver(this)

        // Handle graphics initialization during the ARCore startup.
        textureId = -1
        rotation = -1
        width = -1
        height = -1
        statusListener = listener
        mUserRequestedInstall = true
    }

    private fun setStatus(newStatus: ARStatus) {
        status = newStatus
        if (statusListener != null) {
            statusListener!!.onStatusChanged()
        }
    }

    private fun initializeARCore() {
        var exception: Exception? = null
        var message: String? = null
        val availability = ArCoreApk.getInstance().checkAvailability(activity)
        Log.d(TAG, "Availability is $availability")
        try {
            if (ArCoreApk.getInstance().requestInstall(activity, mUserRequestedInstall) ==
                ArCoreApk.InstallStatus.INSTALL_REQUESTED
            ) {
                // Ensures next invocation of requestInstall() will either return
                // INSTALLED or throw an exception.
                mUserRequestedInstall = false
                return
            }
            session = Session(activity)
        } catch (e: UnavailableArcoreNotInstalledException) {
            setStatus(ARStatus.ARCoreNotInstalled)
            message = "Please install ARCore"
            exception = e
        } catch (e: UnavailableApkTooOldException) {
            setStatus(ARStatus.ARCoreTooOld)
            message = "Please update ARCore"
            exception = e
        } catch (e: UnavailableSdkTooOldException) {
            message = "Please update this app"
            setStatus(ARStatus.SDKTooOld)
            exception = e
        } catch (e: UnavailableDeviceNotCompatibleException) {
            setStatus(ARStatus.DeviceNotSupported)
            message = "This device does not support AR"
            exception = e
        } catch (e: Exception) {
            setStatus(ARStatus.UnknownException)
            message = "This device does not support AR"
            exception = e
        }
        if (message != null) {
            Log.e(TAG, "Exception creating session", exception)
            return
        }

        // Set the graphics information if it was already passed in.
        if (textureId >= 0) {
            setCameraTextureName(textureId)
        }
        if (width > 0) {
            setDisplayGeometry(rotation, width, height)
        }
        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Exception resuming session", e)
            return
        }
        setStatus(ARStatus.Ready)
    }

    /**
     * Handle the onResume event.  This checks the permissions and
     * initializes ARCore.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    protected fun onResume() {
        if (hasCameraPermission(activity)) {
            if (session == null) {
                initializeARCore()
            } else {
                try {
                    session!!.resume()
                } catch (e: CameraNotAvailableException) {
                    Log.e(TAG, "Exception resuming session", e)
                }
            }
        } else {
            requestCameraPermission()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    protected fun onPause() {
        if (session != null) {
            session!!.pause()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected fun onStop() {
        statusListener = null
    }

    /**
     * Start a fragment to handle the permissions.  This is done in a fragment to
     * avoid entangling it with the base Activity.
     */
    private fun requestCameraPermission() {
        val fragment = PermissionFragment()
        fragment.setArSessionSupport(this)
        val mgr: FragmentManager = activity.supportFragmentManager
        val trans: FragmentTransaction = mgr.beginTransaction()
        trans.add(fragment, PermissionFragment.TAG)
        trans.commit()
    }

    /**
     * Handle setting the display geometry.  The values are cached
     * if they are set before the session is available.
     */
    fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        if (session != null) {
            session!!.setDisplayGeometry(rotation, width, height)
        } else {
            this.rotation = rotation
            this.width = width
            this.height = height
        }
    }

    /**
     * Handle setting the texture ID for the background image.  The value is cached
     * if it is called before the session is available.
     */
    fun setCameraTextureName(textureId: Int) {
        if (session != null) {
            session!!.setCameraTextureName(textureId)
            this.textureId = -1
        } else {
            this.textureId = textureId
        }
    }

    @Nullable
    fun update(): Frame? {
        if (session != null) {
            try {
                return session!!.update()
            } catch (e: CameraNotAvailableException) {
                Log.e(TAG, "Exception resuming session", e)
            }
        }
        return null
    }

    enum class ARStatus {
        ARCoreNotInstalled, ARCoreTooOld, SDKTooOld, UnknownException, NeedCameraPermission, Ready, DeviceNotSupported, Uninitialized
    }

    /**
     * Interface for listening for status changes. This can be used for showing error messages
     * to the user.
     */
    interface StatusChangeListener {
        fun onStatusChanged()
    }

    /**
     * PermissionFragment handles requesting the camera permission.
     */
    class PermissionFragment : Fragment() {
        private var arSessionSupport: ARSessionSupport? = null
        fun setArSessionSupport(arSessionSupport: ARSessionSupport?) {
            this.arSessionSupport = arSessionSupport
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            requestCameraPermission(this.activity)
        }


        override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String?>,
            results: IntArray
        ) {
            if (!hasCameraPermission(getActivity())) {
                arSessionSupport!!.setStatus(ARStatus.NeedCameraPermission)
            } else {
                arSessionSupport!!.initializeARCore()
            }
            val mgr: FragmentManager = requireFragmentManager()
            val trans: FragmentTransaction = mgr.beginTransaction()
            trans.remove(this)
            trans.commit()
        }

        companion object {
            const val TAG = "PermissionFragment"
        }
    }

    companion object {
        private const val TAG = "ARSessionSupport"
    }
}