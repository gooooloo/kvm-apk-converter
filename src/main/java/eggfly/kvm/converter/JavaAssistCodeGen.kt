package eggfly.kvm.converter

object JavaAssistCodeGen {

    @Suppress("SpellCheckingInspection")
    private const val INVOKE = "eggfly.kvm.core.VMProxy.invoke"

    fun getInvokeAndReturnStatement(returnTypeString: String, className: String, isStatic: Boolean, methodName: String, parametersClassType: String): String? {
        return when (returnTypeString) {
            Constants.CONSTRUCTOR -> "$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.LANG_VOID -> "$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args); return null;"
            Constants.VOID -> "$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args); return;"
            Constants.BOOLEAN -> "return ((java.lang.Boolean)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).booleanValue();"
            Constants.LANG_BOOLEAN -> "return (java.lang.Boolean)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.INT -> "return ((java.lang.Integer)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).intValue();"
            Constants.LANG_INT -> "return (java.lang.Integer)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.LONG -> "return ((java.lang.Long)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).longValue();"
            Constants.LANG_LONG -> "return (java.lang.Long)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.DOUBLE -> "return ((java.lang.Double)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).doubleValue();"
            Constants.LANG_DOUBLE -> "return (java.lang.Double)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.FLOAT -> "return ((java.lang.Float)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).floatValue();"
            Constants.LANG_FLOAT -> "return (java.lang.Float)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.SHORT -> "return ((java.lang.Short)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).shortValue();"
            Constants.LANG_SHORT -> "return (java.lang.Short)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.BYTE -> "return ((java.lang.Byte)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).byteValue();"
            Constants.LANG_BYTE -> "return (java.lang.Byte)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            Constants.CHAR -> "return ((java.lang.Character)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args)).charValue();"
            Constants.LANG_CHARACTER -> "return (java.lang.Character)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
            else -> "return ($returnTypeString)$INVOKE(\"$className\", \"$methodName\", $parametersClassType, $isStatic, argThis, \$args);"
        }
    }

}