# kvm-apk-converter

注意修改一下JavaAssistInsertImpl中的需要做代码替换的类或者函数，然后：
```
./gradlew jar
java -jar build/libs/kvm-apk-converter.jar input.apk output.apk

```

```flow
st=>start: 输入origin.apk
extract_origin_dex=>operation: 解压缩提取原始classes.dex保存为origin_classes.dex
baksmali=>operation: baksmali反编译origin.apk到smali目录
(为方便后续原样恢复dex)
dex2jar=>operation: dex2jar反编译origin.apk到classes.jar
(为下一步的javassist插桩做准备)
javassist=>operation: 用javassist从classes.jar中查找带annotation的函数体
将实现删除并替换成VMProxy.invoke(...), 保存proxy_classes.jar
dx=>operation: 用dx将proxy_classes.jar编译成proxy_classes.dex
baksmali2=>operation: baksmali把proxy_classes.dex反编译到proxy_smali目录
replace_smali=>operation: 替换带有proxy的类的smali文件,合并到之前的smali目录
smali=>operation: 用smali将smali目录重新编译到new_classes.dex
replace_dex=>operation: 把new_classes.dex复制到apk中，作为classes.dex
encrypt_code=>operation: 把origin_classes.dex加密或者转换到私有的指令集，保存到把encrypted_code.data
(可能要做指令集随机化等)
copy_asset=>operation: 把encrypted_code.data拷贝到apk的assets中
(VM会读取、解密并解释执行这个文件作为实际运行逻辑)
remove_sign=>operation: 去掉apk中的签名
resign=>operation: 重新为apk签名(只能有自己的证书)
e=>end: 输出output.apk
st->extract_origin_dex->baksmali->dex2jar
dex2jar->javassist->dx->baksmali2->replace_smali->smali
smali->replace_dex->encrypt_code->copy_asset->remove_sign->resign->e
```

