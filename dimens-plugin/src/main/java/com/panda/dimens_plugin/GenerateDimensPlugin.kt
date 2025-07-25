package com.panda.dimens_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.text.DecimalFormat
import kotlin.math.abs

open class DimensConfig {
    var designWidthDp: Float = 360f
    var fallbackScreenWidthPx: Float? = 1080f
    var fallbackDensity: Float = 3.0f
    var fallbackScaledDensity: Float = 3.0f

    /**
     * Tùy chỉnh scale theo phần trăm so với thực tế. Ví dụ: 80f = 80% = scale nhỏ hơn 20%
     */
    var manualScalePercent: Float=1.0f
}

class GenerateDimensPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val config = project.extensions.create("dimensConfig", DimensConfig::class.java)

        val generateDimens = project.tasks.register("generateDimens") {
            group = "prebuild"
            description = "Generate dimens.xml based on design width"

            doLast {
                val designWidthDp = config.designWidthDp
                println("🔧 Design width in dp: $designWidthDp")

                // 1. Lấy kích thước màn hình thật qua ADB
                val adbSizeOutput = "adb shell wm size".runCommand()
                println("📱 adb wm size: $adbSizeOutput")

                val screenWidthPx = Regex("""(?:Override|Physical) size:\s*(\d+)x(\d+)""")
                    .find(adbSizeOutput ?: "")
                    ?.groupValues?.get(1)
                    ?.toFloatOrNull()
                    ?: config.fallbackScreenWidthPx
                    ?: error("⚠️ Cannot detect screen width and no fallbackScreenWidthPx provided.")

                println("📏 Screen width (px): $screenWidthPx")

                // 2. Lấy mật độ thực tế qua adb
                val adbDensityOutput = "adb shell wm density".runCommand()
                println("🔍 adb density: $adbDensityOutput")

                val densityDpi = Regex("""Physical density:\s*(\d+)""")
                    .find(adbDensityOutput ?: "")
                    ?.groupValues?.get(1)
                    ?.toFloatOrNull()

                val density = densityDpi?.div(160f) ?: config.fallbackDensity
                val scaledDensity = config.fallbackScaledDensity

                println("🧪 density: $density")
                println("🧪 scaledDensity: $scaledDensity")

                val actualWidthDp = screenWidthPx / density
                val actualScaleFactor = actualWidthDp / designWidthDp

                val scaleFactor = config.manualScalePercent.let {
                    val percentScale = it / 100f
                    println("📐 Adjusting actual scale $actualScaleFactor by ${it}% → final: ${actualScaleFactor * percentScale}")
                    actualScaleFactor * percentScale
                } ?: actualScaleFactor

                val fontScale = scaledDensity / density

                println("🔬 actualWidthDp: $actualWidthDp")
                println("🔬 scaleFactor: $scaleFactor")
                println("🔬 fontScale: $fontScale")

                // 4. Sinh file dimens.xml
                val sdpValues = (-500..500).filter { it != 0 }
                val sspValues = (-500..500).filter { it != 0 }

                val df = DecimalFormat("#.####")

                val xmlContent = buildString {
                    appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                    appendLine("<resources>")

                    sdpValues.forEach { value ->
                        val scaledDp = value * scaleFactor
                        val name = if (value < 0) "_m${abs(value)}sdp" else "_${value}sdp"
                        appendLine("""    <dimen name="$name">${df.format(scaledDp)}dp</dimen>""")
                    }

                    sspValues.forEach { value ->
                        val scaledSp = (value * scaleFactor) / fontScale
                        val name = if (value < 0) "_m${abs(value)}ssp" else "_${value}ssp"
                        appendLine("""    <dimen name="$name">${df.format(scaledSp)}sp</dimen>""")
                    }

                    appendLine("</resources>")
                }

                val outputFile = File("${project.projectDir}/src/main/res/values/dimens.xml")
                outputFile.parentFile.mkdirs()
                outputFile.writeText(xmlContent)

                println("✅ dimens.xml has been generated at: ${outputFile.absolutePath}")
            }
        }

        project.tasks.matching { it.name == "preBuild" }.configureEach {
            dependsOn(generateDimens)
        }
    }

    private fun String.runCommand(): String? = try {
        ProcessBuilder(*split(" ").toTypedArray())
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
    } catch (e: Exception) {
        println("❌ Error running command: $this\n${e.message}")
        null
    }
}
