package io.github.kloping.qqbot.http.data;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** 已创建的 QQ 指令面板记录。 */
@Data
public class PanelRecord {
    @SerializedName("panel_id")
    private String panelId;
}
