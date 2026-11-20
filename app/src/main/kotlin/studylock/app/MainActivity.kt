package studylock.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studylock.app.settings.ThemePreference
import studylock.app.ui.StudyLockApp
import studylock.app.ui.theme.StudyLockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as StudyLockApplication).container
        setContent {
            // The preference is read live so switching appearance takes effect
            // immediately, and it survives process death via DataStore.
            val preference by container.settingsRepository.themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val darkTheme = preference.resolvesToDark(isSystemInDarkTheme())

            // Keep the system bars consistent with the app's resolved theme,
            // not just with the device-wide setting.
            val activity = LocalContext.current as? ComponentActivity
            LaunchedEffect(darkTheme) {
                val host = activity ?: return@LaunchedEffect
                val bars = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                host.enableEdgeToEdge(
                    statusBarStyle = bars,
                    navigationBarStyle = bars
                )
            }

            // The palettes are Android colour resources with values-night
            // variants. Override the resource configuration as well as the
            // Compose colour scheme, otherwise manual selection would still
            // read the device's night colours.
            val baseContext = LocalContext.current
            val themedContext = remember(darkTheme) {
                val config = Configuration(baseContext.resources.configuration)
                config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (darkTheme) Configuration.UI_MODE_NIGHT_YES
                    else Configuration.UI_MODE_NIGHT_NO
                baseContext.createConfigurationContext(config)
            }

            // The themed context replaces LocalContext so colour resources
            // follow the chosen theme. Launchers still need the Activity as
            // their result-registry owner, so provide it explicitly.
            CompositionLocalProvider(
                LocalContext provides themedContext,
                LocalActivityResultRegistryOwner provides this
            ) {
                StudyLockTheme(darkTheme = darkTheme) {
                    StudyLockApp(container = container)
                }
            }
        }
    }
}
