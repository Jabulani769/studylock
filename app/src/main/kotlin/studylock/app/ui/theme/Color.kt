package studylock.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import studylock.app.R

@Composable
fun studyLockDarkColorScheme() = darkColorScheme(
    primary = colorResource(R.color.colorPrimary),
    onPrimary = colorResource(R.color.colorOnPrimary),
    primaryContainer = colorResource(R.color.colorPrimaryContainer),
    onPrimaryContainer = colorResource(R.color.colorOnPrimaryContainer),
    inversePrimary = colorResource(R.color.colorInversePrimary),
    secondary = colorResource(R.color.colorSecondary),
    onSecondary = colorResource(R.color.colorOnSecondary),
    secondaryContainer = colorResource(R.color.colorSecondaryContainer),
    onSecondaryContainer = colorResource(R.color.colorOnSecondaryContainer),
    tertiary = colorResource(R.color.colorTertiary),
    onTertiary = colorResource(R.color.colorOnTertiary),
    tertiaryContainer = colorResource(R.color.colorTertiaryContainer),
    onTertiaryContainer = colorResource(R.color.colorOnTertiaryContainer),
    background = colorResource(R.color.colorBackground),
    onBackground = colorResource(R.color.colorOnBackground),
    surface = colorResource(R.color.colorSurface),
    onSurface = colorResource(R.color.colorOnSurface),
    surfaceVariant = colorResource(R.color.colorSurfaceVariant),
    onSurfaceVariant = colorResource(R.color.colorOnSurfaceVariant),
    surfaceTint = colorResource(R.color.colorPrimary),
    inverseSurface = colorResource(R.color.colorInverseSurface),
    inverseOnSurface = colorResource(R.color.colorInverseOnSurface),
    error = colorResource(R.color.colorError),
    onError = colorResource(R.color.colorOnError),
    errorContainer = colorResource(R.color.colorErrorContainer),
    onErrorContainer = colorResource(R.color.colorOnErrorContainer),
    outline = colorResource(R.color.colorOutline),
    outlineVariant = colorResource(R.color.colorOutlineVariant),
    scrim = Color.Black
)

@Composable
fun studyLockLightColorScheme() = lightColorScheme(
    primary = colorResource(R.color.colorPrimary),
    onPrimary = colorResource(R.color.colorOnPrimary),
    primaryContainer = colorResource(R.color.colorPrimaryContainer),
    onPrimaryContainer = colorResource(R.color.colorOnPrimaryContainer),
    inversePrimary = colorResource(R.color.colorInversePrimary),
    secondary = colorResource(R.color.colorSecondary),
    onSecondary = colorResource(R.color.colorOnSecondary),
    secondaryContainer = colorResource(R.color.colorSecondaryContainer),
    onSecondaryContainer = colorResource(R.color.colorOnSecondaryContainer),
    tertiary = colorResource(R.color.colorTertiary),
    onTertiary = colorResource(R.color.colorOnTertiary),
    tertiaryContainer = colorResource(R.color.colorTertiaryContainer),
    onTertiaryContainer = colorResource(R.color.colorOnTertiaryContainer),
    background = colorResource(R.color.colorBackground),
    onBackground = colorResource(R.color.colorOnBackground),
    surface = colorResource(R.color.colorSurface),
    onSurface = colorResource(R.color.colorOnSurface),
    surfaceVariant = colorResource(R.color.colorSurfaceVariant),
    onSurfaceVariant = colorResource(R.color.colorOnSurfaceVariant),
    surfaceTint = colorResource(R.color.colorPrimary),
    inverseSurface = colorResource(R.color.colorInverseSurface),
    inverseOnSurface = colorResource(R.color.colorInverseOnSurface),
    error = colorResource(R.color.colorError),
    onError = colorResource(R.color.colorOnError),
    errorContainer = colorResource(R.color.colorErrorContainer),
    onErrorContainer = colorResource(R.color.colorOnErrorContainer),
    outline = colorResource(R.color.colorOutline),
    outlineVariant = colorResource(R.color.colorOutlineVariant),
    scrim = Color.Black
)
