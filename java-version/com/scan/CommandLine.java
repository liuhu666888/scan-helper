package com.scan;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 1. 扫描图片
 * <p>
 * 2. ABBYY - 编辑图像
 * 2.1 歪斜校正 - (所有页面) - (歪斜校正)
 * 2.2 等级 - (输入级别: 69 1.00 223) - (输出级别: 0 255) - (所有页面) - (应用)
 * <p>
 * 3. 文件 - 将页面保存为图像 - (保存类型: jpeg 彩色)
 * <p>
 * 4. 执行 CommandLine 去除exif并统一宽度
 * <p>
 * 5. Adobe Acrobat Pro DC - 创建PDF
 * 5.1 工具 - 合并文件 - 添加文件 - 选项(文件大小: 默认大小, 其它选项: 取消所有勾选) - 合并
 * <p>
 * 6. PDF阅读器 - 网格视图 - 检查每页图像
 */
public class CommandLine {
    private final static String PATH = "/Users/osx/Desktop/ttt/新建文件夹";// 处理目录【修改】
    private final static String SUFFIX = "jpg";// "处理目录"中的指定图片后缀【修改】
    private final static String CONVERT = "/usr/local/opt/imagemagick@6/bin/convert";// imagemagick路径【修改】
    private final static long WIDTH = 2048; // 输出宽度（像素）【修改】
    private final static int MAX_THREAD = 5;// 最大线程数【修改】
    private final static File OUT_DIR = new File(PATH, "_resize");// 输出目录
    private final static boolean IS_BLACK_WHITE = true;// 是否转成黑白图像【修改】
    private final static int BLACK_WHITE_WIDTH = 3072;// 黑白图像宽度（像素）【修改】
    // 不转黑白图像只更改大小的封面图片路径，在"处理目录"下，会生成在"输出目录中"【修改】
    private final static String BLACK_WHITE_COVER = new File(PATH, "cover").getAbsolutePath();

    static {
        if (!deleteFile(OUT_DIR))
            throw new RuntimeException("删除输出文件夹失败！");

        if (!OUT_DIR.exists())
            OUT_DIR.mkdirs();

        System.out.println("supported file types: ");
        for (String suffix : ImageIO.getReaderFileSuffixes()) {
            System.out.println("*." + suffix);
        }
        System.out.println("----------");
    }

    public static void main(String[] args) {
        List<File> fileList = getFileWithSuffixDepth1(PATH, SUFFIX);

        final ThreadPoolExecutor fixedThreadPool = (ThreadPoolExecutor)
                Executors.newFixedThreadPool(MAX_THREAD);
        int i = 0;
        for (final File file : fileList) {
            i++;
            fixedThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        // remove exif
                        BufferedImage image = ImageIO.read(file);
                        File outFile = new File(OUT_DIR, file.getName()
                                .replace(" ", "")
                                .replace("\"", "")
                                .replace("'", "")
                        );// 去除空格等特殊符号
                        ImageIO.write(image, getSuffix(file.getAbsolutePath()), outFile);
                        // ImageMagick command line processing
                        StringBuffer commandLine = new StringBuffer();
                        // 命令可自定义
                        // ----- jpg
                        if (!IS_BLACK_WHITE)
                            commandLine.append(CONVERT).append(" ")
                                    .append("-resize").append(" ").append(WIDTH).append(" ")
                                    .append("-gravity").append(" ").append("center").append(" ")
                                    .append("-extent").append(" ").append(WIDTH).append(" ")
                                    .append(outFile.getAbsolutePath()).append(" ")
                                    .append(outFile.getAbsolutePath()).append(" ")
                                    .append(" ");
                        // ----- Black & White png
                        if (IS_BLACK_WHITE)
                            commandLine.append(CONVERT).append(" ")
                                    .append("-resize").append(" ").append(BLACK_WHITE_WIDTH).append(" ")
                                    .append("-gravity").append(" ").append("center").append(" ")
                                    .append("-extent").append(" ").append(BLACK_WHITE_WIDTH).append(" ")
                                    .append("-monochrome").append(" ")
                                    .append(outFile.getAbsolutePath()).append(" ")
                                    .append(getPrefix(outFile.getAbsolutePath())).append(".png").append(" ")
                                    .append(" ");

//                        System.out.println(commandLine);
                        exec(commandLine.toString());
                        if (IS_BLACK_WHITE)
                            deleteFile(outFile);// 生成黑白png后删除jpg
                    } catch (Exception e) {
                        System.out.println("exception:" + file.getName());
                        e.printStackTrace();
                    }
                }
            });
        }
        boolean flag = true;
        while (flag) {
            System.out.println(fixedThreadPool.getCompletedTaskCount() + "|thread sum: " + i);
            if (fixedThreadPool.getCompletedTaskCount() == i) {
                fixedThreadPool.shutdown();
                System.out.println(fixedThreadPool.isShutdown());
                if (fixedThreadPool.isShutdown()) {
                    flag = false;
                }
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (IS_BLACK_WHITE && new File(BLACK_WHITE_COVER).exists()) {
            System.out.println("开始处理封面图片...");
            List<File> coverList = getFileWithSuffixDepth1(BLACK_WHITE_COVER, SUFFIX);
            for (File file : coverList) {
                try {
                    // remove exif
                    BufferedImage image = ImageIO.read(file);
                    File outFile = new File(OUT_DIR, file.getName()
                            .replace(" ", "")
                            .replace("\"", "")
                            .replace("'", "")
                    );// 去除空格等特殊符号
                    ImageIO.write(image, getSuffix(file.getAbsolutePath()), outFile);
                    // ImageMagick command line processing
                    StringBuffer commandLine = new StringBuffer();
                    // 命令可自定义
                    // ----- jpg
                    commandLine.append(CONVERT).append(" ")
                            .append("-resize").append(" ").append(BLACK_WHITE_WIDTH).append(" ")
                            .append("-gravity").append(" ").append("center").append(" ")
                            .append("-extent").append(" ").append(BLACK_WHITE_WIDTH).append(" ")
                            .append(outFile.getAbsolutePath()).append(" ")
                            .append(outFile.getAbsolutePath()).append(" ")
                            .append(" ");
                    exec(commandLine.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("ok");
    }

    /**
     * 执行命令行
     */
    private static String exec(String cmd) {
        StringBuffer outPut = new StringBuffer();
        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        BufferedReader bufferedReader = null;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            inputStream = process.getInputStream();
            bufferedInputStream = new BufferedInputStream(inputStream);
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
                outPut.append(line + "\n");
            int exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            outPut.append(e.getMessage());
        } finally {
            try {
                if (bufferedReader != null)
                    bufferedReader.close();
                if (bufferedInputStream != null)
                    bufferedInputStream.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                outPut.append(e.getMessage());
            }
        }
        return outPut.toString();
    }

    /**
     * 获取文件后缀
     *
     * @return abc.txt返回“txt”
     */
    private static String getSuffix(String fileName) {
        String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
        return prefix;
    }

    /**
     * 获取文件前缀
     *
     * @return abc.txt返回“abc”
     */
    private static String getPrefix(String fileName) {
        String prefix = fileName.substring(0, fileName.lastIndexOf("."));
        return prefix;
    }

    /**
     * 获取单层目录指定后缀文件
     *
     * @param contentBasePath 目录
     * @param suffix          文件后缀，如：html
     */
    private static List<File> getFileWithSuffixDepth1(String contentBasePath, String suffix) {
        File[] files = new File(contentBasePath).listFiles();
        List<File> suffixFiles = new ArrayList<File>();
        for (File file : files) {
            if (getSuffix(file.getAbsolutePath()).equals(suffix)) {
                suffixFiles.add(file);
            }
        }
        return suffixFiles;
    }

    /**
     * 删除文件夹、文件
     *
     * @return 删除成功或者文件夹、文件不存在的时候返回：true
     */
    private static boolean deleteFile(File dir) {
        if (!dir.exists())
            return true;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteFile(children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
