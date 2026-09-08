import re

file_path = "app/src/main/java/com/pixelwater/app/ui/WaterTrackerScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. For the Chart box (line 13322):
content = content.replace(
    "color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF0F2F5),",
    "color = LocalBaseCardColor.current ?: MaterialTheme.colorScheme.surfaceVariant,"
)

# 2. For the History cards:
replacement = """} else {
                            LocalBaseCardColor.current ?: MaterialTheme.colorScheme.surfaceVariant
                        }"""
content = content.replace(
"""} else if (isDark) {
                            Color(0xFF1C1C1E)
                        } else {
                            Color(0xFFF0F2F5)
                        }""",
replacement
)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

