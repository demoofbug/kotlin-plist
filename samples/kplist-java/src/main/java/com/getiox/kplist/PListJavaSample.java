package com.getiox.kplist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class PListJavaSample {
    public static void main(String[] args) throws IOException {
        PListValue root = buildSamplePList();

        writeAndVerifyPList(root, "plist-bin.plist", PListFormat.BINARY);
        writeAndVerifyPList(root, "plist-xml.plist", PListFormat.XML);
    }

    static PListValue buildSamplePList() {
        return new PListDict(Map.of(
                "stringKey", new PListString("stringValue"),
                "intKey", new PListInt(123),
                "boolKey", new PListBool(true),
                "realKey", new PListReal(123.456),
                "dateKey", new PListDate(System.currentTimeMillis()),
                "dataKey", new PListData(new byte[]{1, 2, 3}),
                "arrayKey", new PListArray(List.of(
                        new PListInt(1),
                        new PListBool(false),
                        new PListReal(3.1415926)
                )),
                "dictKey", new PListDict(Map.of(
                        "stringKey", new PListString("stringValue"),
                        "intKey", new PListInt(123)
                ))
        ));
    }

    static void writeAndVerifyPList(PListValue root, String fileName, PListFormat format) throws IOException {
        Path dir = Paths.get("build/plists");
        System.out.println("📝 Writing " + dir.toAbsolutePath());
        Path path = dir.resolve(fileName);

        // create directory if not exists
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }

        // ✅ encode and write to file
        byte[] encoded = PList.encode(root, format);
        Files.write(path, encoded, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("✅ Wrote " + fileName);

        // ✅ read from file  and decode
        byte[] decodedBytes = Files.readAllBytes(path);
        PListValue decoded = PList.decode(decodedBytes);

        System.out.println("🔍 Decoded from " + fileName + ":\n" + decoded + "\n");
    }
}
