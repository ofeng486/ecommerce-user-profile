package com.oufeng.ecommerceuserprofile.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("sys_notification")
public class SysNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private String refType;
    private Long refId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public SysNotification() {}
    public SysNotification(Long userId, String type, String title, String content, String refType, Long refId) {
        this.userId = userId; this.type = type; this.title = title;
        this.content = content; this.refType = refType; this.refId = refId;
        this.isRead = false; this.createdAt = LocalDateTime.now();
    }
    // getters/setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public Boolean getIsRead() { return isRead; } public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public String getRefType() { return refType; } public void setRefType(String refType) { this.refType = refType; }
    public Long getRefId() { return refId; } public void setRefId(Long refId) { this.refId = refId; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
