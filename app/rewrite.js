const fs = require('fs');
const data = fs.readFileSync('app/src/main/java/com/pixelwater/app/ui/WaterTrackerScreen.kt', 'utf8');

const startIndex = data.indexOf('fun androidx.compose.foundation.lazy.LazyListScope.renderAppearanceSettingsSection(');
const searchEnd = '        }\n    }\n}\n';
const chunkAfter = data.substring(startIndex);
const endRelIndex = chunkAfter.indexOf(searchEnd) + searchEnd.length;
const actualEnd = startIndex + endRelIndex;

const toReplace = data.substring(startIndex, actualEnd);

const replacement = `fun androidx.compose.foundation.lazy.LazyListScope.renderAppearanceSettingsSection(
    viewModel: WaterViewModel,
    themeMode: String,
    appTheme: String,
    isFrostedGlassEnabled: Boolean,
    frostedGlassTransparency: Float,
    transparentComponentsEnabled: Boolean,
    componentsTransparency: Float,
    oledModeEnabled: Boolean,
    lightModeDarkTextEnabled: Boolean,
    tabTransitionMode: Int,
    isSwipeTabNavEnabled: Boolean,
    bounceStiffness: Float,
    bounceDamping: Float,
    fadeStiffness: Float,
    fadeDamping: Float,
    slideStiffness: Float,
    slideDamping: Float,
    appLanguage: String,
    isDeveloper: Boolean,
    isEmailDeveloper: Boolean,
    onNavigateToCornerRadius: () -> Unit,
    onScrollLockChange: (Boolean) -> Unit = {}
) {
    com.pixelwater.app.ui.renderRedesignedAppearanceSettingsSection(
        this, viewModel, themeMode, appTheme, isFrostedGlassEnabled, frostedGlassTransparency,
        transparentComponentsEnabled, componentsTransparency, oledModeEnabled,
        lightModeDarkTextEnabled, tabTransitionMode, isSwipeTabNavEnabled,
        bounceStiffness, bounceDamping, fadeStiffness, fadeDamping,
        slideStiffness, slideDamping, appLanguage, isDeveloper, isEmailDeveloper,
        onNavigateToCornerRadius, onScrollLockChange
    )
}
`;

fs.writeFileSync('app/src/main/java/com/pixelwater/app/ui/WaterTrackerScreen.kt', data.replace(toReplace, replacement), 'utf8');
console.log("Successfully replaced appearance section.");
