* 使用apktool反编译android apk
* 替换AndroidManifest.xml中的渠道信息
* jarsigner 重新签名打包
* zipalign 压缩校对

### 使用

java -jar ApkToolBuild.jar apkName= keystoreName= storepass= keyName= keypass= zipalignPath= aaptPath= 

具体看代码