package eggfly.kvm.converter.smali;

import org.jf.dexlib2.writer.DexWriter;
import org.jf.dexlib2.writer.builder.BuilderClassDef;

import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class SmaliUtils {
    public static void main(String[] args) {
        try {
            innerMain();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void innerMain() throws Exception {
        List<BuilderClassDef> classes = SmaliParser.parse("out/eggfly/kvm/demo/MainActivity.smali");
        BuilderClassDef clazz = classes.get(0);

        System.out.println(clazz);
    }
}
