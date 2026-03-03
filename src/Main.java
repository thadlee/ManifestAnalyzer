import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 主类，用于解析AndroidManifest.xml文件并分析APK的activity和权限信息
 */
public class Main {
    // 定义常量字符串，用于XML标签和属性名
    private static final String APPLICATION = "application";
    private static final String RESIZEABLE_ACTIVITY = "resizeableActivity";
    private static final String ACTIVITY = "activity";
    private static final String NAME = "name";
    private static final String USES_PERMISSION = "uses-permission";
    private static final String PERMISSION = "permission";

    // 定义屏幕方向相关的常量
    private static final String SCREEN_ORIENTATION = "screenOrientation";
    private static final String PORTRAIT = "portrait";
    private static final String LANDSCAPE = "landscape";

    // 定义布尔值常量
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    // 定义包名相关常量
    private static final String PACKAGE = "package";

    // 定义文件路径和名称常量
    private static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
    private static final String XML_PATH = "xml";
    private static final String APKS_PATH = "apks";
    private static final String SUFFIX = ".apk";

    // 定义apkanalyzer工具的命令路径
    private static final String APKANALYZER_CMD = "D:\\Android\\cmdline-tools\\latest\\bin" +
            "\\apkanalyzer manifest print ";

    /**
     * 程序入口点
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 创建APK文件目录对象
        File apksFile = new File(APKS_PATH);
        // 如果APK目录不存在，直接返回
        if (!apksFile.exists()) {
            return;
        }
        ProcessBuilder builder;

        try {
            // 遍历APK目录中的所有文件
            for (File file : Objects.requireNonNull(apksFile.listFiles())) {
                // 处理APK文件
                if (file.getPath().endsWith(SUFFIX)) {
                    // 获取APK文件名前缀
                    String prefix = file.getName().split("\\.")[0];
                    // 创建XML输出目录
                    File xmlApkPath = new File("xml/" + prefix);
                    // 构建apkanalyzer命令
                    String command = APKANALYZER_CMD
                            + file.getPath() + ">xml/" + prefix + "/" + MANIFEST_FILE_NAME;

                    // 创建目录
                    xmlApkPath.mkdir();
                    // 如果目录创建失败，跳过当前文件
                    if (!xmlApkPath.exists()) {
                        continue;
                    }
                    // 执行命令
                    builder = new ProcessBuilder("cmd", "/c", command);
                    Map<String, String> env = builder.environment();
                    env.put("SKIP_JDK_VERSION_CHECK", "true");
                    builder.redirectErrorStream(true);
                    builder.start();
                }
            }
            // 等待7秒，确保所有APK文件处理完成
            Thread.sleep(7000);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        // 创建XML文件目录对象
        File xmlFile = new File(XML_PATH);
        // 如果XML目录不存在，输出提示信息并返回
        if (!xmlFile.exists()) {
            System.out.println("xml dir not exists.");
            return;
        }

        // 遍历XML目录中的所有文件
        for (File manifestFile : Objects.requireNonNull(xmlFile.listFiles())) {
            // 处理清单文件目录
            if (manifestFile.isDirectory()) {
                // 打印分隔线
                System.out.println("------------------------------------------------------------"
                        + "---------------------------------------------------");
                // 解析清单文件
                parseManifest(manifestFile.getPath() + "\\" + MANIFEST_FILE_NAME);
                System.out.println("\n");
            }
        }
    }

    /**
     * 解析AndroidManifest.xml文件
     * @param path 清单文件路径
     */
    private static void parseManifest(String path) {
        System.out.println(path);

        SAXReader reader = new SAXReader();
        Document doc;

        try {
            File xmlFile = new File(path);
            // 如果清单文件不存在，输出提示信息并返回
            if (!xmlFile.exists()) {
                System.out.println("Manifest file not exists");
                return;
            }

            // 读取XML文件
            doc = reader.read(new FileInputStream(path));
            Element root = doc.getRootElement();
            // 获取包名
            String packageName = getPackageName(root);
            if (packageName == null) {
                return;
            }
            System.out.println("package: " + packageName);

            // 如果没有application标签，返回
            if (root.elements(APPLICATION).isEmpty()) {
                return;
            }

            // 检查application的resizeableActivity属性
            boolean notResizeableActivityApp
                    = FALSE.equals(root.elements(APPLICATION).get(0).attributeValue(RESIZEABLE_ACTIVITY));

            if (notResizeableActivityApp) {
                System.out.println("application resizeableActivity=false.");
            }

            // 遍历所有activity元素
            for (Element activity : root.elements(APPLICATION).get(0).elements(ACTIVITY)) {
                // 获取activity的属性
                String resizeableActivity = activity.attributeValue(RESIZEABLE_ACTIVITY);
                String activityName = activity.attributeValue(NAME).startsWith(".")
                        ? packageName + activity.attributeValue(NAME)
                        : activity.attributeValue(NAME);

                String orientation = activity.attributeValue(SCREEN_ORIENTATION);
                // 检查是否为固定方向
                boolean isFixedOrientation = "0".equalsIgnoreCase(orientation)
                        || "1".equalsIgnoreCase(orientation)
                        || PORTRAIT.equalsIgnoreCase(orientation)
                        || LANDSCAPE.equalsIgnoreCase(orientation);

                // 根据不同条件输出activity信息
                if (notResizeableActivityApp) {
                    if (!TRUE.equals(resizeableActivity) && isFixedOrientation) {
                        System.out.println("resizeableActivity="
                                + resizeableActivity + ", screenOrientation="
                                + orientation + ", " + activityName);
                    }
                } else {
                    if (FALSE.equals(resizeableActivity) && isFixedOrientation) {
                        System.out.println("resizeableActivity="
                                + resizeableActivity + ", screenOrientation="
                                + orientation + ", " + activityName);
                    }
                }
            }
        } catch (DocumentException | FileNotFoundException e) {
            System.out.println("exception=" + e);
        }
    }

    /**
     * 获取包名
     * @param root XML根元素
     * @return 包名字符串
     */
    private static String getPackageName(Element root) {
        if (root == null) {
            return null;
        }
        return root.attributeValue(PACKAGE);
    }

    /**
     * 打印uses-permission信息
     * @param root XML根元素
     */
    private static void printUsesPermissions(Element root) {
        if (root == null) {
            return;
        }
        // 遍历所有uses-permission元素
        for (Element usesPermission : root.elements(USES_PERMISSION)) {
            System.out.println("usesPermission Name: " + usesPermission.attributeValue(NAME));
        }
    }

    /**
     * 打印permission信息
     * @param root XML根元素
     */
    private static void printPermissions(Element root) {
        if (root == null) {
            return;
        }
        // 遍历所有permission元素
        for (Element permission : root.elements(PERMISSION)) {
            System.out.println("permission Name: " + permission.attributeValue(NAME));
        }
    }
}