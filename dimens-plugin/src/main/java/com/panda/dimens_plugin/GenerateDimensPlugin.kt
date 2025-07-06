package com.panda.dimens_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.text.DecimalFormat

open class DimensConfig {
    var designWidthDp: Float = 360f
}

class GenerateDimensPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val config = project.extensions.create("dimensConfig", DimensConfig::class.java)

        val generateDimens = project.tasks.register("generateDimens") {
            group = "prebuild"
            description = "Generate dimens.xml based on design width"

            doLast {
                val designWidthDp = config.designWidthDp

                println("🔧 Thiết kế chiều rộng dp: $designWidthDp")

                val adbOutput = "adb shell wm size".runCommand() ?: error("⚠️ Không lấy được kích thước từ adb")
                val match = Regex("""Physical size: (\d+)x(\d+)""").find(adbOutput)
                    ?: error("⚠️ Không parse được kích thước màn hình")

                val screenWidthPx = match.groupValues[1].toFloat()
                val density = 3.0f
                val scaledDensity = 3.0f
                val scaleFactor = (screenWidthPx / density) / designWidthDp
                val adjustmentFactor = if (designWidthDp > 360f) 800f / 360f else 1f
                val fontScale = scaledDensity / density

                val sdpValues = listOf(1, 2, 4, 8, 10, 12, 14, 16, 20, 24, 32, 40, 48, 56, 64)
                val sspValues = listOf(10, 12, 14, 16, 18, 20, 24, 40)
                val df = DecimalFormat("#.####")

                val xmlContent = buildString {
                    appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                    appendLine("<resources>")
                    sdpValues.forEach { value ->
                        appendLine("""    <dimen name="_${value}sdp">${df.format(value * scaleFactor * adjustmentFactor)}dp</dimen>""")
                    }
                    sspValues.forEach { value ->
                        appendLine("""    <dimen name="_${value}ssp">${df.format((value * scaleFactor * adjustmentFactor) / fontScale)}dp</dimen>""")
                    }
                    appendLine("</resources>")
                }

                val outputFile = File("${project.projectDir}/src/main/res/values/dimens.xml")
                outputFile.parentFile.mkdirs()
                outputFile.writeText(xmlContent)

                println("✅ dimens.xml đã được sinh tại: ${outputFile.absolutePath}")
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
        println("❌ Lỗi khi chạy lệnh: $this")
        null
    }
}
