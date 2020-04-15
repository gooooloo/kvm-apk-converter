package eggfly.kvm.converter

import com.googlecode.dex2jar.tools.Dex2jarCmd
import org.apache.commons.io.FileUtils
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.base.reference.BaseMethodReference
import org.jf.dexlib2.base.reference.BaseStringReference
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.*
import org.jf.dexlib2.dexbacked.DexBackedMethod
import org.jf.dexlib2.dexbacked.DexBackedMethodImplementation
import org.jf.dexlib2.iface.ExceptionHandler
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.MethodImplementation
import org.jf.dexlib2.iface.TryBlock
import org.jf.dexlib2.iface.debug.DebugItem
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.rewriter.DexRewriter
import org.jf.dexlib2.rewriter.Rewriter
import org.jf.dexlib2.rewriter.RewriterModule
import org.jf.dexlib2.rewriter.Rewriters
import java.io.File

class OldMain {
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
            // ZipArchiveUtils.decompress(backupApk.absolutePath, extracted)

            val apk = DexFileFactory.loadDexContainer(srcApk, Opcodes.getDefault())
            val dexClasses = apk.dexEntryNames.map { dex ->
                val entry = apk.getEntry(dex)
                entry!!.classes
            }.flatten()
            DumpDexClasses().dump(dexClasses)
            println("dump done!")
            val dexFiles = apk.dexEntryNames.map { dex -> dex to apk.getEntry(dex) }
            val mutator = DexRewriter(object : RewriterModule() {
                override fun getMethodRewriter(rewriters: Rewriters): Rewriter<Method> {
                    return Rewriter { m ->
                        val method = m as DexBackedMethod
                        var impl: MethodImplementation? = method.implementation

                        if (impl != null) {
                            val dexImpl = impl as DexBackedMethodImplementation
                            val newImpl = MutableMethodImplementation(impl)
                            val methodFlag = method.getAccessFlags()
                            val hasThisInParam = !AccessFlags.STATIC.isSet(methodFlag)
                            val realParamCount = if (hasThisInParam) method.parameters.size + 1 else method.parameters.size
                            if (newImpl.registerCount - realParamCount >= 2) {
                                // ha ha remove all is dangerous
//                            while (newImpl.instructions.isNotEmpty()) {
//                                newImpl.removeInstruction(0)
//                            }
                                val ref1 = object : BaseStringReference() {
                                    override fun getString(): String {
                                        return "MyTag"
                                    }
                                }
                                val ref2 = object : BaseStringReference() {
                                    override fun getString(): String {
                                        return method.name
                                    }
                                }
                                var offset = -1
//                            newImpl.addInstruction(++offset, BuilderInstruction32x(Opcode.MOVE_OBJECT_16, newImpl.registerCount + 3, 0));
//                            newImpl.addInstruction(++offset, BuilderInstruction32x(Opcode.MOVE_OBJECT_16, newImpl.registerCount + 1, 1));
                                newImpl.addInstruction(++offset, BuilderInstruction21c(Opcode.CONST_STRING, 0, ref1))
                                newImpl.addInstruction(++offset, BuilderInstruction21c(Opcode.CONST_STRING, 1, ref2))
                                val logMethodRef = object : BaseMethodReference() {
                                    override fun getName(): String {
                                        return "d"
                                    }

                                    override fun getReturnType(): String {
                                        return "I"
                                    }

                                    override fun getParameterTypes(): MutableList<out CharSequence> {
                                        return mutableListOf("Ljava/lang/String;", "Ljava/lang/String;")
                                    }

                                    override fun getDefiningClass(): String {
                                        return "Landroid/util/Log;"
                                    }
                                }
                                newImpl.addInstruction(++offset, BuilderInstruction35c(Opcode.INVOKE_STATIC,
                                        2, 0, 1,
                                        0, 0, 0, logMethodRef))
                                //newImpl.addInstruction(++offset, BuilderInstruction32x(Opcode.MOVE_OBJECT_16, 0, newImpl.registerCount));
                                //newImpl.addInstruction(++offset, BuilderInstruction32x(Opcode.MOVE_OBJECT_16, 1, newImpl.registerCount + 1));
                            }
                            for (i in 0..4) {
                                newImpl.addInstruction(0, BuilderInstruction10x(Opcode.NOP))
                            }
                            val i = object : MethodImplementation {
                                override fun getRegisterCount(): Int {
                                    return newImpl.registerCount
                                }

                                override fun getTryBlocks(): MutableList<out TryBlock<out ExceptionHandler>> {
                                    return newImpl.tryBlocks
                                }

                                override fun getInstructions(): MutableIterable<Instruction> {
                                    return newImpl.instructions
                                }

                                override fun getDebugItems(): MutableIterable<DebugItem> {
                                    return newImpl.debugItems
                                }
                            }
                            // TODO: try blocks
                            impl = i
                        }
                        val newMethod = ImmutableMethod(method.definingClass, method.name, method.parameters,
                                method.returnType, method.accessFlags, method.annotations, impl
                        )
                        newMethod
                    }
                }
            })
            val newDexFiles = dexFiles.map { item -> item.first to mutator.rewriteDexFile(item.second!!) }
            println(newDexFiles)
            if (temp.exists() && !temp.isDirectory) {
                throw IllegalStateException("$temp is not a directory")
            }
            temp.mkdirs()
            newDexFiles.forEach {
                val dexPath = File(temp, it.first).absolutePath
                DexFileFactory.writeDexFile(dexPath, it.second)
                ZipUtils.replaceSingleFileIntoZip(outputApk.absolutePath, dexPath)
            }
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