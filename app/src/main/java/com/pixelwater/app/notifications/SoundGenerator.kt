package com.pixelwater.app.notifications

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import kotlin.math.PI
import java.nio.ByteBuffer
import java.nio.ByteOrder

object SoundGenerator {
    /**
     * Synthesizes a beautiful water droplet sound and writes it as a WAV file.
     * The trajectory is a quick upward pitch sweep from 600Hz to 1600Hz with fast volume decay.
     */
    fun generateWaterDropletWav(context: Context): File {
        val file = File(context.filesDir, "water_droplet.wav")
        // Always generate just to be sure it has a correct representation on current app launch
        val sampleRate = 44100
        val duration = 0.20 // 200 ms
        val numSamples = (sampleRate * duration).toInt()
        val data = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration
            // Rapid upward frequency sweep to sound like a droplet landing: 600 to 1400 Hz
            val freq = 550.0 + 950.0 * (progress * progress)
            val wave = sin(2.0 * PI * freq * t)
            
            // Speed curve envelope: immediate attack, exponential decay for splash sound
            val envelope = if (progress < 0.05) {
                progress / 0.05
            } else {
                Math.exp(-12.0 * (progress - 0.05))
            }
            
            val amplitude = 28000.0 * wave * envelope
            data[i] = amplitude.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        writeWavFile(file, sampleRate, data)
        return file
    }

    data class NoteDef(val frequency: Double, val durationMs: Int, val waveType: String)

    fun generateProceduralNotes(instruction: String?): List<NoteDef> {
        val text = (instruction ?: "").lowercase()
        val list = mutableListOf<NoteDef>()

        when {
            // 1. Water theme (drop, splash, drip, flow, ocean)
            text.contains("drop") || text.contains("drip") || text.contains("water") || text.contains("splash") || text.contains("flow") -> {
                list.add(NoteDef(600.0, 100, "sine"))
                list.add(NoteDef(0.0, 50, "sine")) // short pause
                list.add(NoteDef(800.0, 100, "sine"))
                list.add(NoteDef(0.0, 50, "sine"))
                list.add(NoteDef(1100.0, 150, "sine"))
            }
            // 2. Chime, bell, ring, glassy
            text.contains("chime") || text.contains("bell") || text.contains("ring") || text.contains("glass") || text.contains("crystal") -> {
                list.add(NoteDef(523.25, 150, "triangle")) // C5
                list.add(NoteDef(659.25, 150, "triangle")) // E5
                list.add(NoteDef(783.99, 150, "triangle")) // G5
                list.add(NoteDef(1046.50, 300, "sine"))    // C6
            }
            // 3. Retro, arcade, 8-bit, game, synth
            text.contains("retro") || text.contains("arcade") || text.contains("8bit") || text.contains("8-bit") || text.contains("game") || text.contains("synth") -> {
                list.add(NoteDef(261.63, 80, "square")) // C4
                list.add(NoteDef(329.63, 80, "square")) // E4
                list.add(NoteDef(392.00, 80, "square")) // G4
                list.add(NoteDef(523.25, 80, "square")) // C5
                list.add(NoteDef(659.25, 80, "square")) // E5
                list.add(NoteDef(783.99, 160, "square")) // G5
            }
            // 4. Alarm, urgent, beep, alert, siren, fast
            text.contains("alarm") || text.contains("urgent") || text.contains("beep") || text.contains("alert") || text.contains("siren") || text.contains("fast") -> {
                list.add(NoteDef(987.77, 100, "square")) // B5
                list.add(NoteDef(0.0, 50, "sine")) // pause
                list.add(NoteDef(987.77, 100, "square")) // B5
                list.add(NoteDef(0.0, 50, "sine")) // pause
                list.add(NoteDef(1318.51, 150, "square")) // E6
            }
            // 5. Calm, relax, slow, ambient, peaceful, zen
            text.contains("calm") || text.contains("relax") || text.contains("slow") || text.contains("ambient") || text.contains("peaceful") || text.contains("zen") -> {
                list.add(NoteDef(440.00, 250, "sine")) // A4
                list.add(NoteDef(554.37, 250, "sine")) // C#5
                list.add(NoteDef(659.25, 500, "sine")) // E5
            }
            // 6. Default: Melodious sweet water/musical chime
            else -> {
                list.add(NoteDef(587.33, 120, "sine")) // D5
                list.add(NoteDef(659.25, 120, "sine")) // E5
                list.add(NoteDef(880.00, 120, "sine")) // A5
                list.add(NoteDef(987.77, 250, "sine")) // B5
            }
        }
        return list
    }

    /**
     * Generates a custom WAV file based on frequency/duration pairs from AI Coach.
     */
    fun generateAiJingleWav(context: Context, notes: List<NoteDef>): File {
        val file = File(context.filesDir, "ai_notification.wav")
        val sampleRate = 44100
        
        // Calculate total duration
        val totalDurationMs = notes.sumOf { it.durationMs }
        val numSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
        val data = ShortArray(numSamples)
        
        var sampleIndex = 0
        for (note in notes) {
            val freq = note.frequency
            val noteDuration = note.durationMs / 1000.0
            val noteSamples = (sampleRate * noteDuration).toInt()
            
            for (i in 0 until noteSamples) {
                if (sampleIndex >= numSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / noteSamples
                
                // Oscillator
                val period = 1.0 / freq
                val phase = (t % period) / period
                
                var wave = 0.0
                when (note.waveType.lowercase()) {
                    "square" -> wave = if (phase < 0.5) 1.0 else -1.0
                    "triangle" -> wave = if (phase < 0.5) 4.0 * phase - 1.0 else 3.0 - 4.0 * phase
                    "sawtooth" -> wave = 2.0 * phase - 1.0
                    "noise" -> wave = (Math.random() * 2.0 - 1.0)
                    else -> wave = sin(2.0 * PI * freq * t) // default to sine
                }
                
                // Envelope (smooth attacks/releases)
                val envelope = if (progress < 0.1) {
                    progress / 0.1
                } else if (progress > 0.85) {
                    (1.0 - progress) / 0.15
                } else {
                    1.0
                }
                
                val amplitude = 18000.0 * wave * envelope
                data[sampleIndex++] = amplitude.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        
        // Pad remaining if any
        while (sampleIndex < numSamples) {
            data[sampleIndex++] = 0
        }
        
        writeWavFile(file, sampleRate, data)
        return file
    }

    private fun writeWavFile(file: File, sampleRate: Int, data: ShortArray) {
        try {
            val fos = FileOutputStream(file)
            val byteData = ByteBuffer.allocate(data.size * 2)
            byteData.order(ByteOrder.LITTLE_ENDIAN)
            for (sample in data) {
                byteData.putShort(sample)
            }
            
            val header = ByteArray(44)
            val totalDataLen = data.size * 2 + 36
            val totalAudioLen = data.size * 2
            
            header[0] = 'R'.toByte() // RIFF
            header[1] = 'I'.toByte()
            header[2] = 'F'.toByte()
            header[3] = 'F'.toByte()
            
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            
            header[8] = 'W'.toByte() // WAVE
            header[9] = 'A'.toByte()
            header[10] = 'V'.toByte()
            header[11] = 'E'.toByte()
            
            header[12] = 'f'.toByte() // fmt
            header[13] = 'm'.toByte()
            header[14] = 't'.toByte()
            header[15] = ' '.toByte()
            
            header[16] = 16 // Subchunk1Size
            header[17] = 0
            header[18] = 0
            header[19] = 0
            
            header[20] = 1 // AudioFormat = 1 (PCM)
            header[21] = 0
            
            header[22] = 1 // NumChannels = 1 (Mono)
            header[23] = 0
            
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            
            val byteRate = sampleRate * 2
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            
            header[32] = 2 // BlockAlign
            header[33] = 0
            
            header[34] = 16 // BitsPerSample
            header[35] = 0
            
            header[36] = 'd'.toByte() // data
            header[37] = 'a'.toByte()
            header[38] = 't'.toByte()
            header[39] = 'a'.toByte()
            
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
            
            fos.write(header)
            fos.write(byteData.array())
            fos.flush()
            fos.close()
            Log.d("SoundGenerator", "Successfully wrote WAV file to: ${file.absolutePath} (size: ${file.length()})")
        } catch (e: Exception) {
            Log.e("SoundGenerator", "Failed to write WAV file", e)
        }
    }
}
