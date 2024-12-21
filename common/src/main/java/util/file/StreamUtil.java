package util.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// does not auto-create DEFAULT_PATH_ABSOLUTE
public class StreamUtil {

    public static String readBytesAsISOString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = inputStream.read(buffer); len != -1; len = inputStream.read(buffer)) {
            os.write(buffer, 0, len);
        }
        byte[] bytes = os.toByteArray();
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
