package com.tywho;

import com.tywho.apkutils.entity.ApkInfo;
import com.tywho.apkutils.utils.ApkUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by limit on 2017/6/23/0023.
 */
public class SplitApk {
    HashMap<String, String> qudao = new HashMap<String, String>();// 渠道号，渠道名

    ArrayList<String> channelList = new ArrayList<>();

    String basePath;// 当前文件夹路径

    // apk 名
    private String apkName;

    // 秘钥文件名
    private String keystoreName;

    //仓库密码
    private String storepass;

    // 口令密码
    private String keypass;

    // 口令名
    private String keyName;

    // 签名工具地址（全路径）
    private String zipalignPath;
    //aapt路径
    private String aaptPath;

    public SplitApk(String apkName, String keystoreName, String storepass,
                    String keyName, String keypass, String zipalignPath, String aaptPath) {
        this.apkName = apkName;
        this.keystoreName = keystoreName;
        this.storepass = storepass;
        this.keypass = keypass;
        this.keyName = keyName;
        this.zipalignPath = zipalignPath;
        this.aaptPath = aaptPath;

        this.basePath = new File("").getAbsolutePath();
    }

    public void mySplit() {
        getCannelFile();// 获得自定义的渠道号

        modifyChannel(); // 开始打包

    }

    /**
     * 修改渠道字段
     */
    public void modifyChannel() {
        System.out.println("*********创建文件夹 ***********");
        File apkPath = new File(basePath + File.separator + "apk" + File.separator);
        if (!apkPath.exists()) {
            apkPath.mkdir();
        }
        File outputPath = new File(basePath + File.separator + "output" + File.separator);
        if (!outputPath.exists()) {
            outputPath.mkdir();
        }

        // 1， 将该App 反编译
        String cmdUnpack = "java -jar apktool.jar d -f -s " + apkName;
        runCmd(cmdUnpack);

        System.out.println("*********反编译Apk 成功 ***********");

        // 2, 移动清单文件，作为备份
        // 获取编译后后的目录名 和目录文件
        String decodeDir = apkName.split(".apk")[0];
        //
        File decodeDirFile = new File(decodeDir);

        // 获取清单文件
        String maniPath = decodeDirFile.getAbsolutePath() + File.separator + "AndroidManifest.xml";

        // 获取备份清单文件目录 工程根目录
        String maniPathSave = outputPath + File.separator + "AndroidManifest_back.xml";

        // 备份清单文件
        new File(maniPath).renameTo(new File(maniPathSave));
        System.out.println("*********备份清单文件 ***********");

        for (int i = 0; i < channelList.size(); i++) {

            System.out.println("*********开始搞----" + channelList.get(i) + " ***********");
            // 获取备份文件的内容，修改渠道值，并保存到maniPath 中
            updateChannel(maniPathSave, maniPath, channelList.get(i));
            System.out.println("*********修改清单文件，替换清单文件成功 ***********");

            // 重新打包
            String cmdPack = String.format("java -jar apktool.jar b %s %s", decodeDir, apkName);

            runCmd(cmdPack);

            System.out.println("*********4,打包成功，开始重新签名 ***********");

            // 签名文件地址
            String keyStorePath = basePath + File.separator + keystoreName;

            // 未签名的apk 地址
            String unsign_apk_path = decodeDir + File.separator + "dist" + File.separator + apkName;

            String apkname = channelList.get(i) + ".apk";

            // 签名后的apk
            String sign_apk_path = apkPath + File.separator + apkname;

            String signCmd = "jarsigner -digestalg SHA1 -sigalg MD5withRSA -verbose -keystore "
                    + keyStorePath + " -storepass " + storepass + " -keypass "
                    + keypass + " -signedjar " + sign_apk_path + " "
                    + unsign_apk_path + " " + keyName;

            runCmd(signCmd);

            System.out.println("*********5,签名成功，开始压缩对齐 ***********");

            String zipapkname = "zip_sign_" + apkname;
            try {
                if (aaptPath != null) {
                    ApkInfo appInfo = new ApkUtil(aaptPath).getApkInfo(sign_apk_path);
                    System.out.println(appInfo.getVersionName() + "  " + appInfo.getVersionCode());
                    zipapkname = "zip_sign_v" + appInfo.getVersionName() + "_n" + appInfo.getVersionCode() + "_" + apkname;
                }
            } catch (Exception e) {
                System.out.println("*********找不到aaptPath无法解析apk信息***********");
            }
            if (zipalignPath != null) {
                String zipalignCmd = zipalignPath + " -v 4 " + apkPath + File.separator + apkname + " " + apkPath + File.separator + zipapkname;
                runCmd(zipalignCmd);

                deleteFile(sign_apk_path);
            } else {
                System.out.println("*********找不到zipalignPath无法进行压缩校对***********");
                return;
            }

            System.out.println("*********6," + channelList.get(i) + "打包成功***********");
        }
    }

    /**
     * 修改渠道值
     *
     * @param sourcePath 备份清单文件地址
     * @param targetPath 目标清单文件地址
     * @param channelStr 要求该的渠道值
     */
    public void updateChannel(String sourcePath, String targetPath,
                              String channelStr) {

        BufferedReader br = null;
        FileReader fr = null;
        FileWriter fw = null;
        try {
            // 从备份中读取内容
            fr = new FileReader(sourcePath);
            br = new BufferedReader(fr);
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                // 如果某一行存在UMENG_CHANNEL 则替换该值
                if (line.contains("UMENG_CHANNEL")) {
                    line = line.replaceAll("android:value=\"(.*?)\"", "android:value=\"" + channelStr + "\"");
                }
                if (line.contains("JPUSH_CHANNEL")) {
                    line = line.replaceAll("android:value=\"(.*?)\"", "android:value=\"" + channelStr + "\"");
                }
                sb.append(line + "\n");
            }
            // 写到目标清单文件
            fw = new FileWriter(targetPath);
            fw.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
                if (br != null) {
                    br.close();
                }
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 获取渠道字段
     */
    private void getCannelFile() {
        File file = new File("channel.txt");
        // 如果文件不存在，则提示
        if (file.exists() && file.isFile()) {
            BufferedReader br = null;
            FileReader fr = null;
            try {
                // 获取到渠道的输入流
                br = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = br.readLine()) != null) {
                    // 获取到渠道
                    channelList.add(line.trim());
                }
                System.out.println(channelList);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fr != null) fr.close();
                    if (br != null) br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("*********获取渠道成功 ***********");
        } else {
            System.err.println("*********error: channel.txt文件不存在，请添加渠道文件***********");
        }
    }

    /**
     * 执行控制台指令
     *
     * @param cmd
     */
    public void runCmd(String cmd) {
        System.out.println(cmd);
        Runtime rt = Runtime.getRuntime();
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
            // 执行
            Process p = rt.exec(cmd);
            // 获取对应流，一遍打印控制台输出的信息
            isr = new InputStreamReader(p.getInputStream(), Charset.forName("UTF-8"));
            br = new BufferedReader(isr);
            String msg = null;
            while ((msg = br.readLine()) != null) {
                System.out.println(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (isr != null) isr.close();
                if (br != null) br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 删除单个文件
     *
     * @param fileName 被删除文件的文件名
     * @return 单个文件删除成功返回true, 否则返回false
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.isFile() && file.exists()) {
            file.delete();
            System.out.println("删除单个文件" + fileName + "成功！");
            return true;
        } else {
            System.out.println("删除单个文件" + fileName + "失败！");
            return false;
        }
    }
}
