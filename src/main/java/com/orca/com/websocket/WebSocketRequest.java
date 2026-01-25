package com.orca.com.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket请求格式
 */
public class WebSocketRequest {
    @JsonProperty("type")
    private int type;  // 请求类型 1-10
    
    @JsonProperty("requestId")
    private Long requestId;  // 可选，如果为空则由服务端生成
    
    @JsonProperty("responseTerminal")
    private Integer responseTerminal;
    
    @JsonProperty("aLongitude")
    private Double aLongitude;
    
    @JsonProperty("aLatitude")
    private Double aLatitude;
    
    @JsonProperty("bLongitude")
    private Double bLongitude;
    
    @JsonProperty("bLatitude")
    private Double bLatitude;
    
    @JsonProperty("dataSource")
    private Integer dataSource;  // 1:A 2:B 3:C
    
    public WebSocketRequest() {
    }
    
    // Getters and Setters
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public Long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    
    public Integer getResponseTerminal() {
        return responseTerminal;
    }
    
    public void setResponseTerminal(Integer responseTerminal) {
        this.responseTerminal = responseTerminal;
    }
    
    public Double getALongitude() {
        return aLongitude;
    }
    
    public void setALongitude(Double aLongitude) {
        this.aLongitude = aLongitude;
    }
    
    public Double getALatitude() {
        return aLatitude;
    }
    
    public void setALatitude(Double aLatitude) {
        this.aLatitude = aLatitude;
    }
    
    public Double getBLongitude() {
        return bLongitude;
    }
    
    public void setBLongitude(Double bLongitude) {
        this.bLongitude = bLongitude;
    }
    
    public Double getBLatitude() {
        return bLatitude;
    }
    
    public void setBLatitude(Double bLatitude) {
        this.bLatitude = bLatitude;
    }
    
    public Integer getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(Integer dataSource) {
        this.dataSource = dataSource;
    }
}
