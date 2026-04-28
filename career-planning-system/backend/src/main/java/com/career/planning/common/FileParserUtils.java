package com.career.planning.common;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FileParserUtils {

    private static final Logger log = LoggerFactory.getLogger(FileParserUtils.class);

    /**
     * 去掉 ?query、#fragment 后再判断扩展名（OSS 签名 URL 常带查询串）。
     */
    public static String pathForExtensionDetection(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        String p = filePath.trim();
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        int h = p.indexOf('#');
        if (h >= 0) {
            p = p.substring(0, h);
        }
        return p;
    }

    public static String parseFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        try {
            String pathForExt = pathForExtensionDetection(filePath);
            String lowerPath = pathForExt.toLowerCase();

            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                return parseFromUrl(filePath);
            } else {
                return parseFromLocal(filePath, lowerPath);
            }
        } catch (Exception e) {
            log.error("文件解析失败: {}", filePath, e);
            return "文件解析失败";
        }
    }

    private static String parseFromUrl(String fileUrl) throws IOException {
        log.info("从URL下载文件: {}", fileUrl);

        byte[] fileData = downloadFile(fileUrl);
        if (fileData == null || fileData.length == 0) {
            log.error("文件下载失败或为空: {}", fileUrl);
            return "";  // 返回空，让调用方决定如何处理
        }

        String fromBytes = parseBytes(fileData, fileUrl);
        if (fromBytes != null && !fromBytes.isEmpty()) {
            return fromBytes;
        }

        return "";  // 格式不支持，返回空
    }

    /**
     * 从已下载的字节解析简历（供 OSS SDK 下载后使用）；按扩展名提示 + 文件魔数兜底。
     */
    public static String parseBytes(byte[] fileData, String pathOrUrlHint) {
        if (fileData == null || fileData.length == 0) {
            return "";
        }
        String extPath = pathForExtensionDetection(pathOrUrlHint != null ? pathOrUrlHint : "").toLowerCase();
        try {
            if (extPath.endsWith(".pdf") || isPdfMagic(fileData)) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
                    return parsePdfFromStream(bais);
                }
            }
            if (extPath.endsWith(".docx") || isZipMagic(fileData)) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
                    return parseWordFromStream(bais);
                }
            }
            if (extPath.endsWith(".txt")) {
                return new String(fileData, StandardCharsets.UTF_8);
            }
            if (isPdfMagic(fileData)) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
                    return parsePdfFromStream(bais);
                }
            }
            if (isZipMagic(fileData)) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
                    return parseWordFromStream(bais);
                }
            }
        } catch (Exception e) {
            log.error("字节流解析简历失败", e);
            return "";
        }
        log.warn("无法识别简历格式（非 PDF/DOCX/TXT），hint={}", pathOrUrlHint);
        return "";
    }

    private static boolean isPdfMagic(byte[] data) {
        return data.length >= 5
                && data[0] == '%'
                && data[1] == 'P'
                && data[2] == 'D'
                && data[3] == 'F';
    }

    private static boolean isZipMagic(byte[] data) {
        return data.length >= 4 && data[0] == 0x50 && data[1] == 0x4B && (data[2] == 0x03 || data[2] == 0x05 || data[2] == 0x07);
    }

    private static String parseFromLocal(String filePath, String lowerPath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("本地文件不存在: {}", filePath);
            return "";
        }

        if (lowerPath.endsWith(".pdf")) {
            return parsePdf(file);
        } else if (lowerPath.endsWith(".docx")) {
            return parseWord(file);
        } else if (lowerPath.endsWith(".txt")) {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }

        return "不支持的文件格式";
    }

    private static byte[] downloadFile(String fileUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 CareerPlanningBackend/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("下载文件失败, HTTP响应码: {}", responseCode);
                return null;
            }

            try (InputStream is = connection.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.error("下载文件异常: {}", fileUrl, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String parsePdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String parsePdfFromStream(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String parseWord(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String parseWordFromStream(InputStream is) throws IOException {
        try (XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
