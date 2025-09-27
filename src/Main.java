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

public class Main {
    private static final String APPLICATION = "application";
    private static final String RESIZEABLE_ACTIVITY = "resizeableActivity";
    private static final String ACTIVITY = "activity";
    private static final String NAME = "name";
    private static final String USES_PERMISSION = "uses-permission";
    private static final String PERMISSION = "permission";

    private static final String SCREEN_ORIENTATION = "screenOrientation";
    private static final String PORTRAIT = "portrait";
    private static final String LANDSCAPE = "landscape";

    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private static final String PACKAGE = "package";

    private static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
    private static final String XML_PATH = "xml";
    private static final String APKS_PATH = "apks";
    private static final String SUFFIX = ".apk";

    private static final String APKANALYZER_CMD = "C:\\Users\\Liguoning\\AppData\\Local\\Android\\Sdk\\" +
            "cmdline-tools\\latest\\bin\\apkanalyzer manifest print ";

    public static void main(String[] args) {
        File apksFile = new File(APKS_PATH);
        if (!apksFile.exists()) {
            return;
        }
        ProcessBuilder builder;

        try {
            for (File file : Objects.requireNonNull(apksFile.listFiles())) {
                if (file.getPath().endsWith(SUFFIX)) {
                    String prefix = file.getName().split("\\.")[0];
                    File xmlApkPath = new File("xml/" + prefix);
                    String command = APKANALYZER_CMD
                            + file.getPath() + ">xml/" + prefix + "/" + MANIFEST_FILE_NAME;

                    xmlApkPath.mkdir();
                    if (!xmlApkPath.exists()) {
                        continue;
                    }
                    builder = new ProcessBuilder("cmd", "/c", command);
                    Map<String, String> env = builder.environment();
                    env.put("SKIP_JDK_VERSION_CHECK", "true");
                    builder.redirectErrorStream(true);
                    builder.start();
                }
            }
            Thread.sleep(7000);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        File xmlFile = new File(XML_PATH);
        if (!xmlFile.exists()) {
            System.out.println("xml dir not exists.");
            return;
        }

        for (File manifestFile : Objects.requireNonNull(xmlFile.listFiles())) {
            if (manifestFile.isDirectory()) {
                System.out.println("------------------------------------------------------------"
                        + "---------------------------------------------------");
                parseManifest(manifestFile.getPath() + "\\" + MANIFEST_FILE_NAME);
                System.out.println("\n");
            }
        }
    }

    private static void parseManifest(String path) {
        System.out.println(path);

        SAXReader reader = new SAXReader();
        Document doc;

        try {
            File xmlFile = new File(path);
            if (!xmlFile.exists()) {
                System.out.println("Manifest file not exists");
                return;
            }

            doc = reader.read(new FileInputStream(path));
            Element root = doc.getRootElement();
            String packageName = getPackageName(root);
            if (packageName == null) {
                return;
            }
            System.out.println("package: " + packageName);

            if (root.elements(APPLICATION).isEmpty()) {
                return;
            }

            boolean notResizeableActivityApp
                    = FALSE.equals(root.elements(APPLICATION).get(0).attributeValue(RESIZEABLE_ACTIVITY));

            if (notResizeableActivityApp) {
                System.out.println("application resizeableActivity=false.");
            }

            for (Element activity : root.elements(APPLICATION).get(0).elements(ACTIVITY)) {
                String resizeableActivity = activity.attributeValue(RESIZEABLE_ACTIVITY);
                String activityName = activity.attributeValue(NAME).startsWith(".")
                        ? packageName + activity.attributeValue(NAME)
                        : activity.attributeValue(NAME);

                String orientation = activity.attributeValue(SCREEN_ORIENTATION);
                boolean isFixedOrientation = "0".equalsIgnoreCase(orientation)
                        || "1".equalsIgnoreCase(orientation)
                        || PORTRAIT.equalsIgnoreCase(orientation)
                        || LANDSCAPE.equalsIgnoreCase(orientation);

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

    private static String getPackageName(Element root) {
        if (root == null) {
            return null;
        }
        return root.attributeValue(PACKAGE);
    }

    private static void printUsesPermissions(Element root) {
        if (root == null) {
            return;
        }
        for (Element usesPermission : root.elements(USES_PERMISSION)) {
            System.out.println("usesPermission Name: " + usesPermission.attributeValue(NAME));
        }
    }

    private static void printPermissions(Element root) {
        if (root == null) {
            return;
        }
        for (Element permission : root.elements(PERMISSION)) {
            System.out.println("permission Name: " + permission.attributeValue(NAME));
        }
    }
}