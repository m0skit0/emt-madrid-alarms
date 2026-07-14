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
            PlayStoreShowcaseScreen(scene = PlayStoreShowcaseScene.fromValue(sceneValue))
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"

        internal fun intent(context: Context, scene: PlayStoreShowcaseScene): Intent =
            Intent(context, PlayStoreShowcaseActivity::class.java)
                .putExtra(EXTRA_SCENE, scene.value)
    }
}
