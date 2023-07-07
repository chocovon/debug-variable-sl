package util.file;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static config.Config.DEFAULT_PATH_ABSOLUTE;
import static config.Config.META_NAME;

public class FileUtil {
    public static void saveFile(String str, String path) throws IOException {
        try (FileWriter fw = new FileWriter(path);
             BufferedWriter bw = new BufferedWriter(fw)){
            bw.write(str);
        }
    }

    public static void saveFile(byte[] bytes, String path) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
        }
    }

    public static void appendToFile(String str, String path) throws IOException {
        try (FileWriter fw = new FileWriter(path, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(str);
            bw.newLine();
        }
    }

    public static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public static String readFileOrEmpty(String path) {
        String ret = "";
        try {
            ret = readFile(path);
        } catch (Exception ignored) {
        }
        return ret;
    }

    public static String readBytesFileAsISOString(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.ISO_8859_1);
    }

    public static String readBytesAsISOString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = inputStream.read(buffer); len != -1; len = inputStream.read(buffer)) {
            os.write(buffer, 0, len);
        }
        byte[] bytes = os.toByteArray();
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    public static void removeLine(String path, String matcher) throws IOException {
        File oldFile = new File(path);
        File tmpFile = new File(path + ".tmp");
        try (FileReader reader = new FileReader(oldFile);
             BufferedReader bufferedReader = new BufferedReader(reader);
             PrintWriter output = new PrintWriter(tmpFile)){
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.contains(matcher)) {
                    output.println(line);
                }
            }
        }
        Files.move(tmpFile.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
