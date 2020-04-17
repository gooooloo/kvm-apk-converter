package eggfly.kvm.converter

import com.googlecode.dex2jar.tools.Dex2jarCmd
import eggfly.kvm.converter.JavaAssistInsertImpl.replaceMethodCode
import javassist.ClassPool
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets


class Main {
    companion object {
        private const val Dex2JarCache = false

        @JvmStatic
        fun main(args: Array<String>) {
            val srcApk = args[0]
            val targetApk = args[1]
            runMain(File(srcApk), File(targetApk))
        }

        @Suppress("SpellCheckingInspection")
        private fun runMain(srcApk: File, outputApk: File) {
            val backupApk = File(srcApk.name)
            stepPrintln("copyTo: $backupApk")
            srcApk.copyTo(backupApk, true)
            srcApk.copyTo(outputApk, true)

            val extractedApkDir = File("extracted_apk")
            if (extractedApkDir.exists()) {
                FileUtils.deleteDirectory(extractedApkDir)
            }
            extractedApkDir.mkdirs()
            stepPrintln("解压缩apk到: $extractedApkDir")
            ZipUtils.unzip(backupApk.absolutePath, extractedApkDir.absolutePath)

            val smaliOutDir = "smali"

            stepPrintln("baksmali反编译origin.apk到smali目录: $smaliOutDir")
            org.jf.baksmali.Main.main(arrayOf("disassemble", "--output", smaliOutDir, backupApk.absolutePath))

            val jarPath = backupApk.name + ".jar"
            val jarFile = File(jarPath)
            stepPrintln("dex2jar反编译origin.apk到classes.jar: $jarFile")
            if (!Dex2JarCache || !jarFile.exists()) {
                Dex2jarCmd.main(backupApk.absolutePath, "--force", "-o", jarPath)
            }

            val classNames = JarUtils.getAllClasses(jarFile.name)
            val pool = ClassPool.getDefault()
            pool.insertClassPath(jarFile.name)
            pool.insertClassPath("android-28/android.jar")
            val classes = classNames.map { pool.get(it) }
            val proxyJar = File("proxy_classes.jar")
            if (proxyJar.exists()) {
                proxyJar.delete()
            }

            stepPrintln("用javassist从classes.jar中查找函数,将实现删除并替换成VMProxy.invoke(...): $proxyJar")
            val modifiedClassesAndMethods = replaceMethodCode(classes, proxyJar)

            val proxyDex = "proxy_classes.dex"
            stepPrintln("用dx将proxy_classes.jar编译成proxy_classes.dex: $proxyDex")
            com.android.dx.command.Main.main(arrayOf("--dex", "--output=$proxyDex", proxyJar.path))

            val proxySmaliDir = "proxy_smali"
            stepPrintln("baksmali把proxy_classes.dex反编译到proxy_smali目录: $proxySmaliDir")
            org.jf.baksmali.Main.main(arrayOf("disassemble", "--output", proxySmaliDir, proxyDex))

            stepPrintln("替换带有proxy的类的smali文件,合并到之前的smali目录: $smaliOutDir")
            modifiedClassesAndMethods.forEach { (className, methods) ->
                val classSmaliPath = className.replace('.', '/') + ".smali"
                val oldSmaliFile = File(smaliOutDir, classSmaliPath)
                val newSmaliFile = File(proxySmaliDir, classSmaliPath)
                if (oldSmaliFile.exists() && newSmaliFile.exists()) {
                    // println("replace $oldSmaliFile with $newSmaliFile")
                    val newSmaliText = findAndReplaceMethods(oldSmaliFile, newSmaliFile, methods)
                    FileUtils.writeStringToFile(oldSmaliFile, newSmaliText, StandardCharsets.UTF_8)
                } else {
                    throw IllegalStateException("not exist?")
                }
            }

            val newClassesDex = "new_classes.dex"
            stepPrintln("用smali将smali目录重新编译到classes.dex: $newClassesDex")
            org.jf.smali.Main.main(arrayOf("assemble", "--output", newClassesDex, smaliOutDir))

            val dexPath = File(newClassesDex).absolutePath
            stepPrintln("把new_classes.dex复制到apk中，作为classes.dex: $outputApk")
            ZipUtils.replaceSingleFileIntoZip(outputApk.absolutePath, dexPath, "classes.dex")

            val codeFileInAsset = "assets/code.dex"
            stepPrintln("把code拷贝到apk的assets中: $codeFileInAsset")
            val originClasses = File(extractedApkDir, "classes.dex")
            ZipUtils.replaceSingleFileIntoZip(outputApk.absolutePath, originClasses.absolutePath, codeFileInAsset)

            stepPrintln("去掉apk中的签名: $outputApk (size: ${outputApk.length()})")
            ZipUtils.removeSignature(outputApk.absolutePath)
            stepPrintln("用~/.android/debug.keystore重新签名: $outputApk (size: ${outputApk.length()})")
            @Suppress("SpellCheckingInspection")
            Runtime.getRuntime().exec("jarsigner -keystore /Users/eggfly/.android/debug.keystore -storepass android ${outputApk.name} androiddebugkey")

            println("All progress done, the apk $srcApk was repackaged to $outputApk (origin size: ${backupApk.length()}, output size: ${outputApk.length()})")
        }

        @Suppress("SpellCheckingInspection")
        private fun findAndReplaceMethods(oldSmaliFile: File, newSmaliFile: File, methods: List<String>): String {
            var oldSmaliText = FileUtils.readFileToString(oldSmaliFile, StandardCharsets.UTF_8)
            val newSmaliText = FileUtils.readFileToString(newSmaliFile, StandardCharsets.UTF_8)
            methods.forEach { method ->
                val oldMethodSection = findMethodSection(oldSmaliText, method)
                val newMethodSection = findMethodSection(newSmaliText, method)
                if (oldMethodSection.isEmpty() || newMethodSection.isEmpty()) {
                    throw RuntimeException("parse error")
                } else {
                    oldSmaliText = oldSmaliText.replaceFirst(oldMethodSection, newMethodSection)
                }
            }
            return oldSmaliText
        }

        @Suppress("SpellCheckingInspection")
        private fun findMethodSection(smali: String, method: String): String {
            val startOffset = smali.indexOf(".method $method")
            if (startOffset < 0) {
                throw RuntimeException("parse error")
            }
            val leftPart = smali.substring(startOffset)
            val endMethodStr = ".end method"
            val endOffset = leftPart.indexOf(endMethodStr)
            return leftPart.substring(0, endOffset + endMethodStr.length)
        }

        private var stepCount = 0
        private fun stepPrintln(step: String) {
            stepCount++
            println("第${stepCount}步 -> $step")
        }
    }
}