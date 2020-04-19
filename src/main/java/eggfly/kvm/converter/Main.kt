package eggfly.kvm.converter

import com.googlecode.dex2jar.tools.Dex2jarCmd
import eggfly.kvm.converter.JavaAssistInsertImpl.replaceMethodCode
import javassist.ClassPool
import org.apache.commons.io.FileUtils
import java.io.File
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths


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
            val outputTempDir = File("../kvm-output")
            if (outputTempDir.exists()) {
                FileUtils.deleteDirectory(outputTempDir)
            }

            val backupApk = File(outputTempDir, srcApk.name)
            stepPrintln("copyTo: $backupApk")
            srcApk.copyTo(backupApk, true)
            srcApk.copyTo(outputApk, true)

            val extractedApkDir = File(outputTempDir, "extracted_apk")

            extractedApkDir.mkdirs()
            stepPrintln("解压缩apk到: $extractedApkDir")
            ZipUtils.unzip(backupApk.path, extractedApkDir.path)

            val smaliOutDir = File(outputTempDir, "smali")
            stepPrintln("baksmali反编译origin.apk到smali目录: $smaliOutDir")
            org.jf.baksmali.Main.main(arrayOf("disassemble", "--output", smaliOutDir.path, backupApk.path))

            val jarFile = File(outputTempDir, backupApk.name + ".jar")
            stepPrintln("dex2jar反编译origin.apk到classes.jar: $jarFile")
            if (!Dex2JarCache || !jarFile.exists()) {
                Dex2jarCmd.main(backupApk.path, "--force", "-o", jarFile.path)
            }

            val classNames = JarUtils.getAllClasses(jarFile.path)
            val pool = ClassPool.getDefault()
            pool.insertClassPath(jarFile.path)
            pool.insertClassPath("android-28/android.jar")
            val classes = classNames.map { pool.get(it) }
            val proxyJar = File(outputTempDir, "proxy_classes.jar")

            stepPrintln("用javassist从classes.jar中查找函数,将实现删除并替换成VMProxy.invoke(...): $proxyJar")
            val modifiedClassesAndMethods = replaceMethodCode(classes, proxyJar)

            val proxyDex = File(outputTempDir, "proxy_classes.dex")
            stepPrintln("用dx将proxy_classes.jar编译成proxy_classes.dex: $proxyDex")
            com.android.dx.command.Main.main(arrayOf("--dex", "--output=${proxyDex.path}", proxyJar.path))

            val proxySmaliDir = File(outputTempDir, "proxy_smali")
            stepPrintln("baksmali把proxy_classes.dex反编译到proxy_smali目录: $proxySmaliDir")
            org.jf.baksmali.Main.main(arrayOf("disassemble", "--output", proxySmaliDir.path, proxyDex.path))

            val path = Paths.get(outputTempDir.path, "proxy_methods.txt")
            stepPrintln("替换带有proxy的类的smali文件,合并到之前的smali目录: $smaliOutDir, 类列表: $path")
            val builder = StringBuilder()
            modifiedClassesAndMethods.forEach { (className, methods) ->
                val classSmaliPath = className.replace('.', '/') + ".smali"
                val oldSmaliFile = File(smaliOutDir, classSmaliPath)
                val newSmaliFile = File(proxySmaliDir, classSmaliPath)
                if (oldSmaliFile.exists() && newSmaliFile.exists()) {
                    // println("replace $oldSmaliFile with $newSmaliFile")
                    val newSmaliText = findAndReplaceMethods(oldSmaliFile, newSmaliFile, methods)
                    FileUtils.writeStringToFile(oldSmaliFile, newSmaliText, StandardCharsets.UTF_8)
                    methods.forEach { method ->
                        builder.append(classSmaliPath).append("->").append(method).append('\n')
                    }
                } else {
                    throw IllegalStateException("not exist?")
                }
            }
            Files.write(path, builder.toString().toByteArray())

            val newClassesDex = File(outputTempDir, "new_classes.dex")
            stepPrintln("用smali将smali目录重新编译到classes.dex: $newClassesDex")
            org.jf.smali.Main.main(arrayOf("assemble", "--output", newClassesDex.path, smaliOutDir.path))

            stepPrintln("把new_classes.dex复制到apk中，作为classes.dex: $outputApk")
            ZipUtils.replaceSingleFileIntoZip(outputApk.path, newClassesDex.path, "classes.dex")

            val codeFileInAsset = "assets/code.dex"
            stepPrintln("把code拷贝到apk的assets中: $codeFileInAsset")
            val originClasses = File(extractedApkDir, "classes.dex")
            ZipUtils.replaceSingleFileIntoZip(outputApk.path, originClasses.path, codeFileInAsset)

            stepPrintln("去掉apk中的签名: $outputApk (size: ${outputApk.length()})")
            ZipUtils.removeSignature(outputApk.path)
            stepPrintln("用~/.android/debug.keystore重新签名: $outputApk (size: ${outputApk.length()})")
            @Suppress("SpellCheckingInspection")
            Runtime.getRuntime().exec("jarsigner -keystore /Users/eggfly/.android/debug.keystore -storepass android ${outputApk.path} androiddebugkey")

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