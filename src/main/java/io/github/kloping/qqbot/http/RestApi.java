package io.github.kloping.qqbot.http;

import io.github.kloping.qqbot.Start0;
import io.github.kloping.qqbot.Starter;
import io.github.kloping.spt.annotations.AutoStand;
import io.github.kloping.spt.annotations.Entity;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

/**
 * 注解代理无法覆盖的 REST 接口。
 *
 * <p>SpringTool 的 HTTP 代理在 0.6.x ~ 0.7.2-L1 只会按 GET/POST 发请求，
 * 用 {@code @RequestPath(method = ...)} 声明的 DELETE/PUT 实际会被发成 GET，
 * 因此撤回消息、响应互动事件这类接口在这里直接用 okhttp 调用。
 *
 * <p>调用方式：{@code starter.getBot().restApi.recallMessage(groupOpenId, messageId)}。
 *
 * @author github.kloping
 */
@Entity
@Slf4j
public class RestApi {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build();

    @AutoStand
    Starter starter;

    @AutoStand
    Start0 start0;

    /**
     * 撤回群聊消息
     * <table><thead><tr><th colspan="2">基本</th></tr> <tr><td>HTTP URL</td> <td>/v2/groups/{group_openid}/messages/{message_id}</td></tr> <tr><td>HTTP Method</td> <td>DELETE</td></tr></table>
     * <hr>
     * 发送超过 2 分钟的消息不可撤回。<br>
     * 机器人为群管理员时可以撤回自己以及普通群成员的消息；普通成员只能撤回自己发送的消息。<br>
     * 成功返回 HTTP 200，无响应体。
     *
     * @param groupOpenId 群的 openid
     * @param messageId   消息 ID（消息发送响应里的 id）
     * @return 是否撤回成功
     */
    public boolean recallMessage(String groupOpenId, String messageId) {
        if (isBlank(groupOpenId) || isBlank(messageId)) return false;
        return request("DELETE", String.format("/v2/groups/%s/messages/%s", groupOpenId, messageId), null);
    }

    /**
     * 响应互动事件（消息按钮 / 快捷菜单回调）
     * <table><thead><tr><th colspan="2">基本</th></tr> <tr><td>HTTP URL</td> <td>/interactions/{interaction_id}</td></tr> <tr><td>HTTP Method</td> <td>PUT</td></tr></table>
     * <hr>
     * 需要在收到互动事件后 5 秒内响应，否则客户端会提示操作失败。
     *
     * @param interactionId 互动事件 ID
     * @param code          0 成功，1 操作失败，2 操作频繁，3 重复操作，4 没有权限，5 仅管理员操作
     * @return 是否响应成功
     */
    public boolean respondInteraction(String interactionId, int code) {
        if (isBlank(interactionId)) return false;
        return request("PUT", String.format("/interactions/%s", interactionId),
                String.format("{\"code\": %s}", code));
    }

    /**
     * 以指定 HTTP 方法调用接口，自动带上机器人鉴权头与当前环境域名。
     *
     * @param method   HTTP 方法，如 DELETE、PUT、POST
     * @param path     接口路径，须以 / 开头
     * @param jsonBody JSON 请求体，无请求体时传 null
     * @return HTTP 2xx 视为成功
     */
    public boolean request(String method, String path, String jsonBody) {
        if (starter == null || start0 == null) return false;
        try {
            String host = starter.net.endsWith("/")
                    ? starter.net.substring(0, starter.net.length() - 1) : starter.net;
            Request.Builder builder = new Request.Builder().url(host + path);
            start0.getHeaders().forEach(builder::header);
            builder.method(method, jsonBody == null ? null : RequestBody.create(JSON, jsonBody));
            Response response = CLIENT.newCall(builder.build()).execute();
            try {
                if (response.code() < 200 || response.code() >= 400) {
                    log.error(String.format("%s %s failed: HTTP %s", method, path, response.code()));
                    return false;
                }
                return true;
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error(String.format("%s %s error: %s", method, path, e.getMessage()));
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
