package com.ILoveU.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期和时间处理的工具类。
 */
public class DateUtil {

    // 定义一个标准的ISO8601 UTC格式化器，例如：2025-05-15T08:30:00Z
    // DateTimeFormatter.ISO_INSTANT 会自动处理为UTC 'Z' 后缀
    private static final DateTimeFormatter ISO_UTC_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * 私有构造函数，防止实例化工具类。
     */
    private DateUtil() {}

    /**
     * 将 java.sql.Timestamp 对象格式化为 ISO8601 UTC 标准字符串。
     * 例如："2025-05-15T08:30:00Z"
     *
     * @param timestamp 要格式化的 Timestamp 对象。如果为 null，则返回 null。
     * @return ISO8601 UTC 格式的日期时间字符串，或 null。
     */
    public static String formatTimestampToISOString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        // 将 java.sql.Timestamp 转换为 java.time.Instant (Instant 本身就是UTC的)
        Instant instant = timestamp.toInstant();
        // 使用 DateTimeFormatter.ISO_INSTANT 进行格式化
        return ISO_UTC_FORMATTER.format(instant);
    }

    /**
     * 将 java.time.LocalDateTime 对象（假设它代表UTC时间）格式化为 ISO8601 UTC 标准字符串。
     *
     * @param localDateTime 要格式化的 LocalDateTime 对象。如果为 null，则返回 null。
     * @return ISO8601 UTC 格式的日期时间字符串，或 null。
     */
    public static String formatLocalDateTimeToISOStringUTC(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        // 将 LocalDateTime 视为UTC时间，转换为 Instant
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        return ISO_UTC_FORMATTER.format(instant);
    }

    /**
     * 将 java.time.OffsetDateTime 对象格式化为 ISO8601 标准字符串。
     * OffsetDateTime 本身包含时区偏移信息，ISO_OFFSET_DATE_TIME 会保留它。
     * 如果想强制转为UTC 'Z' 格式，需要先转换。
     *
     * @param offsetDateTime 要格式化的 OffsetDateTime 对象。如果为 null，则返回 null。
     * @return ISO8601 格式的日期时间字符串，或 null。
     */
    public static String formatOffsetDateTimeToISOString(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        // 直接使用 DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
    }

    /**
     * 获取当前时间的ISO8601 UTC字符串表示。
     * @return 当前时间的ISO8601 UTC字符串。
     */
    public static String nowAsISOStringUTC() {
        return ISO_UTC_FORMATTER.format(Instant.now());
    }

    /**
     * (可选) 将ISO8601格式的字符串解析为 OffsetDateTime 对象。
     * 用于处理从前端接收到的日期时间字符串。
     *
     * @param isoDateTimeString ISO8601格式的日期时间字符串。
     * @return 解析后的 OffsetDateTime 对象；如果输入为null或解析失败，则返回null。
     */
    public static OffsetDateTime parseISOStringToOffsetDateTime(String isoDateTimeString) {
        if (isoDateTimeString == null || isoDateTimeString.trim().isEmpty()) {
            return null;
        }
        try {
            // DateTimeFormatter.ISO_OFFSET_DATE_TIME 可以解析多种包含偏移量的ISO格式
            return OffsetDateTime.parse(isoDateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.err.println("无法解析日期时间字符串: " + isoDateTimeString + " - " + e.getMessage());
            // 在实际应用中，这里应该使用logger记录错误
            // logger.error("无法解析日期时间字符串: {}", isoDateTimeString, e);
            return null;
        }
    }

    /**
     * (可选) 将ISO8601格式的字符串解析为 Timestamp 对象 (转换为系统默认时区)。
     * 注意：这种转换涉及默认时区，可能不如直接使用OffsetDateTime或Instant精确。
     *
     * @param isoDateTimeString ISO8601格式的日期时间字符串。
     * @return 解析后的 Timestamp 对象；如果输入为null或解析失败，则返回null。
     */
    public static Timestamp parseISOStringToTimestamp(String isoDateTimeString) {
        OffsetDateTime offsetDateTime = parseISOStringToOffsetDateTime(isoDateTimeString);
        if (offsetDateTime == null) {
            return null;
        }
        // OffsetDateTime.toInstant() 总是返回一个UTC的Instant
        // Timestamp.from(Instant) 会创建一个Timestamp
        return Timestamp.from(offsetDateTime.toInstant());
    }
}
