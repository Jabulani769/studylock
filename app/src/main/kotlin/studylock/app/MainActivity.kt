package studylock.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import studylock.app.ui.StudyLockApp
import studylock.app.ui.theme.StudyLockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Follow the device theme automatically; system bars adapt with it.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val container = (application as StudyLockApplication).container
        setContent {
            StudyLockTheme(darkTheme = isSystemInDarkTheme()) {
                StudyLockApp(container = container)
            }
        }
    }
}
