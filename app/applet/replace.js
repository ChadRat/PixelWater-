const fs = require('fs');
const file = './app/src/main/java/com/pixelwater/app/ui/WaterTrackerScreen.kt';
let code = fs.readFileSync(file, 'utf8');

code = code.replace(/androidx\.compose\.foundation\.BorderStroke\(\s*1\.dp\s*,\s*(.*?)\)/g, 'appBorder($1)');
code = code.replace(/androidx\.compose\.foundation\.BorderStroke\(\s*2\.dp\s*,\s*(.*?)\)/g, 'appBorder($1)');
code = code.replace(/BorderStroke\(\s*1\.dp\s*,\s*(.*?)\)/g, 'appBorder($1)');
code = code.replace(/BorderStroke\(\s*2\.dp\s*,\s*(.*?)\)/g, 'appBorder($1)');
code = code.replace(/Modifier\.border\(\s*width\s*=\s*1\.dp\s*,\s*color\s*=\s*(.*?),\s*shape\s*=\s*(.*?)\s*\)/gs, 'Modifier.appBorder($1, $2)');
code = code.replace(/Modifier\.border\(\s*1\.dp\s*,\s*(.*?),\s*(.*?)\s*\)/gs, 'Modifier.appBorder($1, $2)');

fs.writeFileSync(file, code);
