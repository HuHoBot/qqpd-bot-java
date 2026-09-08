package io.github.kloping.qqbot.utils;

import io.github.kloping.qqbot.api.SendAble;
import io.github.kloping.qqbot.entities.ex.Audio;
import io.github.kloping.qqbot.entities.ex.FileMsg;
import io.github.kloping.qqbot.entities.ex.Image;
import io.github.kloping.qqbot.entities.ex.Video;
import io.github.kloping.qqbot.entities.ex.msg.MessageChain;
import io.github.kloping.qqbot.entities.qqpd.message.MessageAttachment;
import io.github.kloping.qqbot.entities.qqpd.message.MessageReference;
import io.github.kloping.qqbot.entities.qqpd.message.RawMessage;
import io.github.kloping.qqbot.entities.qqpd.message.RawPreMessage;
import io.github.kloping.qqbot.impl.MessagePacket;
import io.github.kloping.spt.util.Judge;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author github.kloping
 */
public class BaseUtils {

    /**
     * 类型转换
     *
     * @param packet
     * @param msg
     */
    public static void packet2pre(MessagePacket packet, RawPreMessage msg) {
        if (Judge.isNotEmpty(packet.getContent())) {
            msg.setContent(packet.getContent());
        }
        if (Judge.isNotEmpty(packet.getImage())) {
            msg.setImage(packet.getImage());
        }
        if (Judge.isNotEmpty(packet.getReplyId())) {
            msg.setMessageReference(new MessageReference(packet.getReplyId()));
        }
    }

    public static <T, K1, K2> T tryGet(Map<K1, Map<K2, T>> tmap, K1 k1, K2 k2) {
        Map<K2, T> map = tmap.get(k1);
        if (map == null) return null;
        else return map.get(k2);
    }

    public static final Pattern AT_PATTERN = Pattern.compile("<@![0-9]+>");
    public static final Pattern AT_ALL = Pattern.compile("@everyone");
    public static final Pattern AT_CHANNEL = Pattern.compile("<#[0-9]+>");
    public static final Pattern EMOJI = Pattern.compile("<emoji:[0-9]+>");
    public static final Pattern EMOJI_V2 = Pattern.compile("<faceType=.*?>");

    public static MessageChain parseToMessageChain(RawMessage rawMessage) {
        return parseToMessageChain(rawMessage, null);
    }

    public static MessageChain parseToMessageChain(RawMessage rawMessage, Class<?>[] filter) {
        String content = rawMessage.getContent();
        MessageChain chain = new MessageChain();
        dispose(content, chain);
        if (rawMessage.getAttachments() != null && rawMessage.getAttachments().length > 0) {
            for (MessageAttachment attachment : rawMessage.getAttachments()) {
                String contentType = attachment.getContent_type();
                //	附件内容类型（MIME 类型）: voice=语音消息 image/jpeg=JPEG 图片 image/png=PNG 图片 image/gif=GIF 图片 video/mp4=MP4 视频 file=群文件
                if (contentType.startsWith("image")) {
                    chain.append(new Image(attachment.getUrl()));
                    continue;
                } else if (contentType.startsWith("video")) {
                    chain.append(new Video(attachment.getUrl(), attachment.getFilename()));
                    continue;
                } else if (contentType.startsWith("voice")) {
                    chain.append(new Audio(attachment.getUrl(), attachment.getFilename()));
                    continue;
                } else {
                    FileMsg fileMsg = null;
                    fileMsg = new FileMsg(1, attachment.getContent_type(), attachment.getUrl(), null, attachment.getFilename()) {};
                    chain.append(fileMsg);
                }
            }
        }
        if (filter != null && filter.length > 0) {
            List<Class> list = new ArrayList<>(Arrays.asList(filter));
            chain.reSet(chain.stream().filter(s -> s != null && !list.contains(s.getClass())).collect(Collectors.toList()));
        }
        return chain;
    }

    private static void dispose(String content, MessageChain chain) {
        if (Judge.isEmpty(content)) return;
        List<SendAble> sendAbles = PdCode.deserializePdCode(content);
        if (sendAbles == null || sendAbles.isEmpty()) return;
        for (SendAble sendAble : sendAbles) {
            chain.append(sendAble);
        }
    }

    public static Map<String, Object> parseAngleBracketsEmoji(String s) {
        Map<String, Object> map = new HashMap<>();
        String[] split = s.substring(1, s.length() - 1).split(",");
        for (String s1 : split) {
            Integer i0 = s1.indexOf("=");
            String key = s1.substring(0, i0);
            String value = s1.substring(i0 + 1, s1.length());
            if (value.startsWith("\"") && value.endsWith("\"")) {
                map.put(key, value.substring(1, value.length() - 1));
            } else map.put(key, value);
        }
        return map;
    }
}
