package com.rainy.barcodelabeling

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Config
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.ux.ArFragment

class MainActivity : AppCompatActivity() {

    private val arFragment by lazy { supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment }

    private lateinit var arViewModel: ArViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ArViewModel(ModelProvider(this@MainActivity.applicationContext)) as T
            }
        }).get()

        setupAr()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            arViewModel.messageEventFlow.collect {
                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        lifecycleScope.launchWhenStarted {
            arViewModel.placeOverlayEventFlow.collect { node ->
                node.parent = arFragment.arSceneView.scene
                arFragment.arSceneView.scene.addChild(node)

            }
        }
    }

    private fun setupAr() {
        arFragment.setOnSessionConfigurationListener { session, config ->
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            config.planeFindingMode =
                Config.PlaneFindingMode.VERTICAL // Don't bother with horizontal planes since we want to scan only walls
            config.focusMode = Config.FocusMode.AUTO

            // Fixes autofocus
            session.apply {
                resume()
                pause()
                resume()
            }
        }

        arFragment.setOnViewCreatedListener { arSceneView ->
            configureArScene(arSceneView)

            arSceneView.scene.addOnUpdateListener {
                arSceneView?.session?.also { session ->
                    val rotation =
                        RotationUtil.getRotationCompensation(session.cameraConfig.cameraId, this)
                    arViewModel.onNewFrame(session, rotation)
                }
            }

        }
    }

    private fun configureArScene(arSceneView: ArSceneView) {
        arSceneView.planeRenderer.isVisible = false
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
    }
}
