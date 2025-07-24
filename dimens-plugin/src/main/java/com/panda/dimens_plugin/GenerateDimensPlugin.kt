package com.panda.dimens_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.text.DecimalFormat
import kotlin.math.abs

open class DimensConfig {
    var designWidthDp: Float = 360f
    var fallbackScreenWidthPx: Float? = 1080f
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

                val adbOutput = "adb shell wm size".runCommand()
                    ?: error("⚠️ Failed to get screen size from adb")


                val match = Regex("""(?:Override|Physical) size:\s*(\d+)x(\d+)""").find(adbOutput)
                    ?: error("⚠️ Unable to parse screen size from adb output:\n$adbOutput")

                val screenWidthPx = try {
                    val adbOutput = "adb shell wm size".runCommand()
                        ?: throw Exception("ADB failed")
                    val match = Regex("""(?:Override|Physical) size:\s*(\d+)x(\d+)""")
                        .find(adbOutput)
                        ?: throw Exception("Regex failed")
                    match.groupValues[1].toFloat()
                } catch (e: Exception) {
                    config.fallbackScreenWidthPx
                        ?: error("⚠️ Cannot detect screen width via ADB and no fallback provided. Error: ${e.message}")
                }

                val density = 3.0f
                val scaledDensity = 3.0f
                val scaleFactor = (screenWidthPx / density) / designWidthDp
                val adjustmentFactor = if (designWidthDp > 360f) 800f / 360f else 1f
                val fontScale = scaledDensity / density

                val sdpValues = (-500..500).filter { it != 0 }
                val sspValues = (-500..500).filter { it != 0 }

                val df = DecimalFormat("#.####")

                val xmlContent = buildString {
                    appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                    appendLine("<resources>")
                    sdpValues.forEach { value ->
                        if(value<0){
                            appendLine("""    <dimen name="_m${abs(value)}sdp">${df.format(value * scaleFactor * adjustmentFactor)}dp</dimen>""")
                        }else{
                            appendLine("""    <dimen name="_${value}sdp">${df.format(value * scaleFactor * adjustmentFactor)}dp</dimen>""")
                        }
                    }
                    sspValues.forEach { value ->
                        val formattedValue = df.format((value * scaleFactor * adjustmentFactor) / fontScale)
                        if(value<0){
                            appendLine("""    <dimen name="_m${abs(value)}ssp">$formattedValue${if (value > 0) "sp" else "sp"}</dimen>""")
                        }else{
                            appendLine("""    <dimen name="_${value}ssp">$formattedValue${if (value > 0) "sp" else "sp"}</dimen>""")
                        }
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
        println("❌ Error running command: $this")
        null
    }
}
