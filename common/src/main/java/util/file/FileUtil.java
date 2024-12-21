package util.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static config.Config.DEFAULT_PATH_ABSOLUTE;

public class FileUtil {
    static {
        try {
            Files.createDirectories(Paths.get(DEFAULT_PATH_ABSOLUTE));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create working directory", e);
        }
    }

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
