package eggfly.kvm.converter;

import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.expr.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JavaAssistInsertImpl {

    private static String[] exceptPackageList = {"android", "androidx", "kotlin"};
    private static boolean isForceInsertLambda = false;
    private static boolean isExceptMethodLevel = false;
    private static String[] exceptMethodList = {};
    private static boolean isHotfixMethodLevel = false;
    private static String[] hotfixMethodList = {"test"};
    private static AtomicInteger insertMethodCount = new AtomicInteger(0);
    @SuppressWarnings("SpellCheckingInspection")
    private static String[] hotfixPackageList = {"eggfly.kvm.demo"};

    protected static boolean isNeedInsertClass(String className) {
        //这样可以在需要埋点的剔除指定的类
        for (String exceptName : exceptPackageList) {
            if (className.startsWith(exceptName)) {
                return false;
            }
        }
//        return true;
        for (String name : hotfixPackageList) {
            if (className.startsWith(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCallMethod = false;

    private static boolean isMethodWithExpression(CtMethod ctMethod) throws CannotCompileException {
        isCallMethod = false;
        if (ctMethod == null) {
            return false;
        }

        ctMethod.instrument(new ExprEditor() {
            /**
             * Edits a <tt>new</tt> expression (overridable).
             * The default implementation performs nothing.
             *
             * @param e the <tt>new</tt> expression creating an object.
             */
//            public void edit(NewExpr e) throws CannotCompileException { isCallMethod = true; }

            /**
             * Edits an expression for array creation (overridable).
             * The default implementation performs nothing.
             *
             * @param a the <tt>new</tt> expression for creating an array.
             * @throws CannotCompileException
             */
            public void edit(NewArray a) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits a method call (overridable).
             *
             * The default implementation performs nothing.
             */
            public void edit(MethodCall m) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits a constructor call (overridable).
             * The constructor call is either
             * <code>super()</code> or <code>this()</code>
             * included in a constructor body.
             *
             * The default implementation performs nothing.
             *
             */
            public void edit(ConstructorCall c) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits an instanceof expression (overridable).
             * The default implementation performs nothing.
             */
            public void edit(Instanceof i) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits an expression for explicit type casting (overridable).
             * The default implementation performs nothing.
             */
            public void edit(Cast c) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits a catch clause (overridable).
             * The default implementation performs nothing.
             */
            public void edit(Handler h) throws CannotCompileException {
                isCallMethod = true;
            }
        });
        return isCallMethod;
    }

    private static boolean isQualifiedMethod(CtBehavior it) throws CannotCompileException {
        if (it.getMethodInfo().isStaticInitializer()) {
            return false;
        }

        // synthetic 方法暂时不aop 比如AsyncTask 会生成一些同名 synthetic方法,对synthetic 以及private的方法也插入的代码，主要是针对lambda表达式
        if (!isForceInsertLambda && (it.getModifiers() & AccessFlag.SYNTHETIC) != 0 && !AccessFlag.isPrivate(it.getModifiers())) {
            return false;
        }
        if (it.getMethodInfo().isConstructor()) {
            return false;
        }

        if ((it.getModifiers() & AccessFlag.ABSTRACT) != 0) {
            return false;
        }
        if ((it.getModifiers() & AccessFlag.NATIVE) != 0) {
            return false;
        }
        if ((it.getModifiers() & AccessFlag.INTERFACE) != 0) {
            return false;
        }

        if (it.getMethodInfo().isMethod()) {
            if (AccessFlag.isPackage(it.getModifiers())) {
                it.setModifiers(AccessFlag.setPublic(it.getModifiers()));
            }
            boolean flag = isMethodWithExpression((CtMethod) it);
            if (!flag) {
                return false;
            }
        }
        //方法过滤
        if (isExceptMethodLevel && exceptMethodList != null) {
            for (String exceptMethod : exceptMethodList) {
                if (it.getName().matches(exceptMethod)) {
                    return false;
                }
            }
        }

        if (isHotfixMethodLevel && hotfixMethodList != null) {
            for (String name : hotfixMethodList) {
                if (it.getName().matches(name)) {
                    return true;
                }
            }
        }
        return !isHotfixMethodLevel;
    }

    protected static void zipFile(byte[] classBytesArray, ZipOutputStream zos, String entryName) {
        try {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(classBytesArray, 0, classBytesArray.length);
            zos.closeEntry();
            zos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> replaceMethodCode(List<CtClass> box, File jarFile) throws CannotCompileException, IOException, NotFoundException {
        List<String> modifiedClasses = new ArrayList<>();
        ZipOutputStream outStream = new JarOutputStream(new FileOutputStream(jarFile));
//        new ForkJoinPool().submit {
        for (CtClass ctClass : box) {
            boolean classModified = false;
            if (isNeedInsertClass(ctClass.getName())) {
                //change class modifier
                ctClass.setModifiers(AccessFlag.setPublic(ctClass.getModifiers()));
                if (ctClass.isInterface() || ctClass.getDeclaredMethods().length < 1) {
                    //skip the unsatisfied class
                    zipFile(ctClass.toBytecode(), outStream, ctClass.getName().replaceAll("\\.", "/") + ".class");
                    continue;
                }

                boolean addIncrementalChange = false;
                for (CtBehavior ctBehavior : ctClass.getDeclaredBehaviors()) {
//                    if (!addIncrementalChange) {
//                        //insert the field
//                        addIncrementalChange = true;
//                        ClassPool classPool = ctBehavior.getDeclaringClass().getClassPool();
//                        CtClass type = classPool.getOrNull(Constants.INTERFACE_NAME);
//                        CtField ctField = new CtField(type, Constants.INSERT_FIELD_NAME, ctClass);
//                        ctField.setModifiers(AccessFlag.PUBLIC | AccessFlag.STATIC);
//                        ctClass.addField(ctField);
//                    }
                    if (!isQualifiedMethod(ctBehavior)) {
                        continue;
                    }
                    //here comes the method will be inserted code
                    String body = "";
                    try {
                        if (ctBehavior.getMethodInfo().isMethod()) {
                            CtMethod ctMethod = (CtMethod) ctBehavior;
                            boolean isStatic = (ctMethod.getModifiers() & AccessFlag.STATIC) != 0;
                            CtClass returnType = ctMethod.getReturnType();
                            String returnTypeString = returnType.getName();
                            // construct the code will be inserted in string format
                            body = "";
                            body += "Object argThis = null;\n";
                            if (!isStatic) {
                                body += "argThis = $0;\n";
                            }
                            String parametersClassType = getParametersClassType(ctMethod);
////                                body += "if (com.meituan.robust.PatchProxy.isSupport(\$args, argThis, ${Constants.INSERT_FIELD_NAME}, $isStatic, " + methodMap.get(ctBehavior.longName) + ",${parametersClassType},${returnTypeString}.class)) {"
//                            body += "if (com.meituan.robust.PatchProxy.isSupport($args, argThis, " + Constants.INSERT_FIELD_NAME + ", " + isStatic +
//                                    ", " + methodMap.get(ctBehavior.getLongName()) + "," + parametersClassType + "," + returnTypeString + ".class)) {";
                            body += JavaAssistCodeGen.INSTANCE.getReturnStatement(returnTypeString, ctClass.getName(), isStatic, ctMethod.getName(), parametersClassType);
                            //finish the insert-code body ,let`s insert it
                            body = "{\n" + body + "\n}";
                            ctBehavior.setBody(body);
                            classModified = true;
                        }
                    } catch (Exception t) {
                        //here we ignore the error
                        t.printStackTrace();
                        System.err.println(body);
                        System.out.println("ctClass: " + ctClass.getName() + " error: " + t.getMessage());
                    }
                }
            }
            // zip the inserted-classes into output file
            zipFile(ctClass.toBytecode(), outStream, ctClass.getName().replaceAll("\\.", "/") + ".class");
            if (classModified) {
                modifiedClasses.add(ctClass.getName());
            }
        }
//        }.get()
        outStream.close();
        return modifiedClasses;
    }

    private static String getParametersClassType(CtMethod method) throws NotFoundException {
        if (method.getParameterTypes().length == 0) {
            return " null ";
        }
        StringBuilder parameterType = new StringBuilder();
        parameterType.append("new Class[]{");
        for (CtClass paramterClass : method.getParameterTypes()) {
            parameterType.append(paramterClass.getName()).append(".class,");
        }
        //remove last ','
        if (',' == parameterType.charAt(parameterType.length() - 1))
            parameterType.deleteCharAt(parameterType.length() - 1);
        parameterType.append("}");
        return parameterType.toString();
    }

}
