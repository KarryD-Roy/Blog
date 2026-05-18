package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("skills")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String title;

    private String description;

    /**
     * 是否置顶，true 表示置顶
     */
    private Boolean pinned;

    /**
     * 上级技能 ID
     */
    private Long parentId;

    /**
     * 固化后的图谱坐标
     */
    @com.fasterxml.jackson.annotation.JsonProperty("xAxis")
    private Double xAxis;

    @com.fasterxml.jackson.annotation.JsonProperty("yAxis")
    private Double yAxis;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;
}
