package org.m0skit0.android.emtmadridalarms.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class PlayStoreShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sceneValue = intent.getStringExtra(EXTRA_SCENE).orEmpty()
            val modeValue = intent.getStringExtra(EXTRA_MODE).orEmpty()
            PlayStoreShowcaseScreen(
                scene = PlayStoreShowcaseScene.fromValue(sceneValue),
                mode = PlayStoreShowcaseMode.fromValue(modeValue),
            )
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
        const val EXTRA_MODE = "mode"
    }
}
