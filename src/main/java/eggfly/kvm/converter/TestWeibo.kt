package eggfly.kvm.converter

import java.io.File

class TestWeibo {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runMain()
        }

        private fun runMain() {
            val backupApk = File("com.sina.weibo.apk")
            val outputApk = File("weibo.output.apk")
            File(".").listFiles().forEach {
                if (it.endsWith(".dex") && it.isFile) {

                }
            }
            ZipUtils.replaceSingleFileIntoZip(backupApk.absolutePath, "classes")

            // ZipArchiveUtils.compress("output.zip", *extracted.listFiles())
            // val params = "output2.apk -v -u -d -z app-debug.zip".trim().split(" ").toTypedArray()
            // ApkBuilderMain.main(params)
            ZipUtils.removeSignature(outputApk.absolutePath)
            // @Suppress("SpellCheckingInspection")
            // Runtime.getRuntime().exec("jarsigner -keystore ~/.android/debug.keystore -storepass android ${outputApk.name} androiddebugkey")
            println("repackage apk done")
        }
    }
}