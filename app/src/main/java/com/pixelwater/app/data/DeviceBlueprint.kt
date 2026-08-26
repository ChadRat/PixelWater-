package com.pixelwater.app.data

data class DeviceBlueprint(
    val specsDescription: String = "",
    val backPanelColor: String = "#1E1E1E",
    val bumpX: Float = 0f,
    val bumpY: Float = 0f,
    val bumpWidth: Float = 0f,
    val bumpHeight: Float = 0f,
    val bumpCornerRadius: Float = 0f,
    val lenses: List<Lens> = emptyList(),
    val flashX: Float = 0f,
    val flashY: Float = 0f,
    val isHorizontalVisor: Boolean = false, // For Pixel style
    val detectedBrand: String = "",
    val detectedModel: String = ""
) {
    data class Lens(val x: Float, val y: Float, val radius: Float)
}
