package com.qaassist.integration.jira;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AttachmentParser {

  private final Parser parser = new AutoDetectParser();
  private static final int MAX_CONTENT_LENGTH = 50_000; // Обрезаем слишком большие файлы

  /**
   * Извлекает текст из бинарного контента (PDF/DOCX/XLSX).
   * Возвращает null, если тип файла не поддерживается.
   */
  public ParsedAttachment parse(byte[] content, String filename, String mimeType) {
    try {
      if (!isSupportedType(mimeType)) {
        log.debug("⏭️ Unsupported attachment type: {} ({})", filename, mimeType);
        return null;
      }

      var handler = new BodyContentHandler(MAX_CONTENT_LENGTH);
      var metadata = new Metadata();
      metadata.set(Metadata.RESOURCE_NAME_KEY, filename);

      parser.parse(new ByteArrayInputStream(content), handler, metadata, new ParseContext());

      String text = handler.toString().trim();
      if (text.isBlank()) {
        log.warn("⚠️ Extracted empty text from {}", filename);
        return new ParsedAttachment(filename, mimeType, "", true);
      }

      log.debug("✅ Parsed {}: {} chars", filename, text.length());
      return new ParsedAttachment(filename, mimeType, truncateIfNeeded(text), false);

    } catch (IOException | SAXException | TikaException e) {
      log.error("❌ Failed to parse {}: {}", filename, e.getMessage());
      return new ParsedAttachment(filename, mimeType, "", true);
    }
  }

  private boolean isSupportedType(String mimeType) {
    return mimeType != null && (
        mimeType.equals("application/pdf") ||
            mimeType.startsWith("application/vnd.openxmlformats-officedocument") ||
            mimeType.equals("application/msword") ||
            mimeType.equals("application/vnd.ms-excel")
    );
  }

  private String truncateIfNeeded(String text) {
    return text.length() > MAX_CONTENT_LENGTH
        ? text.substring(0, MAX_CONTENT_LENGTH) + "...\n[TRUNCATED]"
        : text;
  }

  public record ParsedAttachment(
      String filename,
      String mimeType,
      String content,
      boolean error
  ) {
    public boolean isEmpty() { return content == null || content.isBlank(); }
    public String getPreview(int maxChars) {
      return content != null && content.length() > maxChars
          ? content.substring(0, maxChars) + "..."
          : content;
    }
  }
}