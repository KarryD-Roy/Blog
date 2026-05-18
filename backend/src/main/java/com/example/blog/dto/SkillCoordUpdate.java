package com.example.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SkillCoordUpdate {

    private Long id;

    @JsonProperty("xAxis")
    private Double xAxis;

    @JsonProperty("yAxis")
    private Double yAxis;

    private Integer version;
}
