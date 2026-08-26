with open("app/src/main/java/com/pixelwater/app/ui/WaterTrackerScreen.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "requiredSize(52.dp)" in line:
        print(f"Found line {i+1}: {repr(line)}")
        for j in range(i, min(i + 25, len(lines))):
            print(f"  {j+1}: {repr(lines[j])}")
        break
