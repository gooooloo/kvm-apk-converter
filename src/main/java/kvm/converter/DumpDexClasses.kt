package kvm.converter

import kvm.converter.util.ClassUtils
import javassist.ClassPool
import javassist.CtClass
import javassist.CtNewMethod
import org.apache.commons.io.FileUtils
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.dexbacked.DexBackedClassDef
import org.jf.dexlib2.dexbacked.DexBackedMethod
import org.jf.dexlib2.iface.MethodParameter
import java.io.File

class DumpDexClasses {
    companion object {
        const val DIR = "output_classes"
    }

    fun dump(dexClasses: List<DexBackedClassDef>) {

        val temp = File(DIR)
        if (temp.exists()) {
            FileUtils.deleteDirectory(temp)
        }
        val pool = ClassPool.getDefault()
        pool.find("")
        dexClasses.forEach { dexClass ->
            val name = ClassUtils.convertSignatureToCanonicalName(dexClass.type)
            val newClass = pool.makeClass(name)
            val directMethods = dexClass.directMethods
            val virtualMethods = dexClass.virtualMethods
            val isInterface = AccessFlags.INTERFACE.isSet(dexClass.accessFlags)
            if (isInterface) {
                val newInterface = pool.makeInterface(name)
                directMethods.forEach {
                }
            } else {
                directMethods.forEach { directMethod ->
                    try {
                        newClass.getDeclaredMethod(directMethod.name)
                    } catch (e: javassist.NotFoundException) {
                        @Suppress("SpellCheckingInspection")
                        if (directMethod.name != "<init>" && directMethod.name != "<clinit>") {
                            val returnTypeStr = directMethod.returnType
                            val returnType = getReturnTypeFromString(returnTypeStr)
                            if (returnType != null) {
                                // val parameters: Array<CtClass> = getParametersArray(directMethod.parameters)
                                // val exceptions :Array<CtClass> = getExceptionsArray(directMethod)
                                val newMethod = CtNewMethod.make(directMethod.accessFlags,
                                        returnType, directMethod.name,
                                        arrayOf(), arrayOf(), null, newClass)
                                // val newMethod = CtNewMethod.make("", newClass)

                                newClass.addMethod(newMethod)
                            }
                        }
                    }
                }
            }
            newClass.writeFile(DIR)
        }
        println("dump done!")
    }

    private fun getExceptionsArray(directMethod: DexBackedMethod?): Array<CtClass> {
        TODO("Not yet implemented")
    }

    private fun getParametersArray(parameters: List<MethodParameter>): Array<CtClass> {
        TODO("Not yet implemented")
    }

    private fun getReturnTypeFromString(returnTypeStr: String): CtClass? {
        return when (returnTypeStr[0]) {
            'V' -> CtClass.voidType
            else -> object : CtClass(ClassUtils.convertSignatureToCanonicalName(returnTypeStr)) {}
        }
    }
}