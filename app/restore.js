const { execSync } = require('child_process');
try {
  execSync('git checkout app/src/main/java/com/pixelwater/app/ui/WaterTrackerScreen.kt');
  console.log('Restored');
} catch (e) {
  console.log(e.toString());
}
