package eggfly.kvm.converter.util

object ClassUtils {
    fun convertSignatureToCanonicalName(signature: String): String {
        when (signature[0]) {
            'L' -> return signature.trimStart('L').trimEnd(';').replace('/', '.')
            else -> throw NotImplementedError(signature)
        }
    }

    fun convertCanonicalNameToSignature(name: String): String = "L" + name.replace('.', '/') + ";"
}