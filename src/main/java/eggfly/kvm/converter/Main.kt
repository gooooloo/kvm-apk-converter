package eggfly.kvm.converter

import com.googlecode.dex2jar.tools.Dex2jarCmd
import eggfly.kvm.converter.JavaAssistInsertImpl.replaceMethodCode
import javassist.ClassPool
import org.apache.commons.io.FileUtils
import org.jf.dexlib2.DexFileFactory
import java.io.File

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val srcApk = args[0]
            val targetApk = args[1]
            runMain(File(srcApk), File(targetApk))
        }

        private fun runMain(srcApk: File, targetApk: File) {
            val backupApk = File(srcApk.name)
            val outputApk = File("output.apk")
            srcApk.copyTo(backupApk, true)
            srcApk.copyTo(outputApk, true)
            val temp = File("temp")
            if (temp.exists()) {
                FileUtils.deleteDirectory(temp)
            }
            temp.mkdirs()

            @Suppress("SpellCheckingInspection")
            val smaliOutDir = "smali_out"
            org.jf.baksmali.Main.main(arrayOf("disassemble", "--output", smaliOutDir, backupApk.absolutePath))
            org.jf.smali.Main.main(arrayOf("assemble", "--output", "combine.dex", smaliOutDir))

            val jarPath = backupApk.name + ".jar"
            val jarFile = File(jarPath)
            if (!jarFile.exists()) {
                Dex2jarCmd.main(backupApk.absolutePath, "--force", "-o", jarPath)
            }

            val classNames = JarUtils.getAllClasses(jarFile.name)
            val pool = ClassPool.getDefault()
            pool.insertClassPath(jarFile.name)
            pool.insertClassPath("android-28/android.jar")
            val classes = classNames.map { pool.get(it) }
            val modOutput = File("output_mod.jar")
            if (modOutput.exists()) {
                modOutput.delete()
            }
            replaceMethodCode(classes, modOutput)
            // ZipArchiveUtils.compress("output.zip", *extracted.listFiles())
            // val params = "output2.apk -v -u -d -z app-debug.zip".trim().split(" ").toTypedArray()
            // ApkBuilderMain.main(params)

            ZipUtils.removeSignature(outputApk.absolutePath)

            val dexPath = File("classes.dex").absolutePath
            ZipUtils.replaceSingleFileIntoZip(outputApk.absolutePath, dexPath)

//            @Suppress("SpellCheckingInspection")
//            Runtime.getRuntime().exec("jarsigner -keystore ~/.android/debug.keystore -storepass android ${outputApk.name} androiddebugkey")
            com.android.dx.command.Main.main(arrayOf("--dex", "--output=classes.dex", "output_mod.jar"))
            // com.android.dx.command.dexer.Main.run(com.android.dx.command.dexer.Main.Arguments())
            // com.android.dx.command.Main.main(arrayOf())
            println("repackage apk done")
        }
    }
}