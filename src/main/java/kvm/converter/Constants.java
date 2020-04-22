package kvm.converter;

import java.util.Arrays;
import java.util.List;

public class Constants {

    public static final String CLASSES_DEX_NAME = "classes.dex";
    public static final String PATACH_JAR_NAME = "patch.jar";
    public static final String PATCH_CONTROL_SUFFIX = "Control";
    public static final String INLINE_PATCH_SUFFIX = "InLinePatch";

    public static Class ModifyAnnotationClass = null;
    public static Class AddAnnotationClass = null;

    public final static String PRIMITIVE_TYPE = "ZCBSIJFDV";
    public final static String ARRAY_TYPE = "[";
    public final static char OBJECT_TYPE = 'L';
    public static final String PACKNAME_START = String.valueOf(OBJECT_TYPE);
    public static final Boolean OBSCURE = true;
    //    public static final Boolean OBSCURE = false;
    //    public static final Boolean isLogging = false;
    public static boolean isLogging = true;


    public static final String CONSTRUCTOR = "Constructor";
    public static final String LANG_VOID = "java.lang.Void";
    public static final String VOID = "void";
    public static final String LANG_BOOLEAN = "java.lang.Boolean";
    public static final String BOOLEAN = "boolean";
    public static final String LANG_INT = "java.lang.Integer";
    public static final String INT = "int";
    public static final String LANG_LONG = "java.lang.Long";
    public static final String LONG = "long";
    public static final String LANG_DOUBLE = "java.lang.Double";
    public static final String DOUBLE = "double";
    public static final String LANG_FLOAT = "java.lang.Float";
    public static final String FLOAT = "float";
    public static final String LANG_SHORT = "java.lang.Short";
    public static final String SHORT = "short";
    public static final String LANG_BYTE = "java.lang.Byte";
    public static final String BYTE = "byte";
    public static final String LANG_CHARACTER = "Character";
    public static final String CHAR = "char";

    public static final List<String> NO_NEED_REFLECT_CLASS = Arrays.asList("android.os.Bundle", "android.os.BaseBundle");
}
