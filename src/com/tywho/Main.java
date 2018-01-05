package com.tywho;

public class Main {
    // write your code here
    // apk的名字
    private static String apkName = "app-release.apk";
    //签名文件
    private static String keystoreName = "xxxxx.jks";
    // 初始密码
    private static String storepass = "xxxxx";
    // 名字
    private static String keyName = "xxxxx";
    // 密码
    private static String keypass = "xxxxx";
    // 打包的地址 去jdk 中找
    private static String zipalignPath = "D:\\Android\\android-sdk\\build-tools\\25.0.3\\zipalign";

    private static String aaptPath = "D:\\Android\\android-sdk\\build-tools\\25.0.3\\aapt.exe";

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("apkName=")) {
                apkName = arg.trim().toLowerCase().split("=")[1];
            } else if (arg.startsWith("keystoreName=")) {
                keystoreName = arg.trim().toLowerCase().split("=")[1];
            } else if (arg.startsWith("storepass=")) {
                storepass = arg.trim().toLowerCase().split("=")[1];
            } else if (arg.startsWith("keyName=")) {
                keyName = arg.trim().toLowerCase().split("=")[1];
            } else if (arg.startsWith("keypass=")) {
                keypass = arg.trim().toLowerCase().split("=")[1];
            } else if (arg.startsWith("zipalignPath=")) {
                zipalignPath = arg.trim().toLowerCase().split("=")[1];
            } else if (arg.startsWith("aaptPath=")) {
                aaptPath = arg.trim().toLowerCase().split("=")[1];
            }
        }
        // 开始打包
        new SplitApk(apkName, keystoreName, storepass, keyName, keypass, zipalignPath, aaptPath).mySplit();

    }
}
