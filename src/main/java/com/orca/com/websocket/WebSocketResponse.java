package com.orca.com.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orca.com.protocol.UdpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket响应格式
 */
public class WebSocketResponse {
    @JsonProperty("type")
    private int type;
    
    @JsonProperty("requestId")
    private long requestId;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("count")
    private Long count;
    
    @JsonProperty("items")
    private List<ResponseItem> items;
    
    public static class ResponseItem {
        @JsonProperty("aLongitude")
        private double aLongitude;
        
        @JsonProperty("bLongitude")
        private double bLongitude;
        
        @JsonProperty("type")
        private long type;
        
        @JsonProperty("density")
        private float density;
        
        @JsonProperty("field6")
        private int field6;
        
        @JsonProperty("terrainData")
        private String terrainData;  // Base64编码
        
        // Getters and Setters
        public double getALongitude() {
            return aLongitude;
        }
        
        public void setALongitude(double aLongitude) {
            this.aLongitude = aLongitude;
        }
        
        public double getBLongitude() {
            return bLongitude;
        }
        
        public void setBLongitude(double bLongitude) {
            this.bLongitude = bLongitude;
        }
        
        public long getType() {
            return type;
        }
        
        public void setType(long type) {
            this.type = type;
        }
        
        public float getDensity() {
            return density;
        }
        
        public void setDensity(float density) {
            this.density = density;
        }
        
        public int getField6() {
            return field6;
        }
        
        public void setField6(int field6) {
            this.field6 = field6;
        }
        
        public String getTerrainData() {
            return terrainData;
        }
        
        public void setTerrainData(String terrainData) {
            this.terrainData = terrainData;
        }
    }
    
    public WebSocketResponse() {
    }
    
    public static WebSocketResponse fromUdpResponse(UdpResponse udpResponse, int requestType) {
        WebSocketResponse response = new WebSocketResponse();
        response.type = requestType;
        response.requestId = udpResponse.getRequestId();
        response.success = true;
        response.count = udpResponse.getCount();
        response.items = new ArrayList<>();
        
        for (UdpResponse.ResponseItem item : udpResponse.getItems()) {
            ResponseItem wsItem = new ResponseItem();
            wsItem.aLongitude = item.getALongitude();
            wsItem.bLongitude = item.getBLongitude();
            wsItem.type = item.getType();
            wsItem.density = item.getDensity();
            wsItem.field6 = item.getField6();
            if (item.getTerrainData() != null) {
                wsItem.terrainData = java.util.Base64.getEncoder().encodeToString(item.getTerrainData());
            }
            response.items.add(wsItem);
        }
        
        return response;
    }
    
    public static WebSocketResponse error(int requestType, long requestId, String error) {
        WebSocketResponse response = new WebSocketResponse();
        response.type = requestType;
        response.requestId = requestId;
        response.success = false;
        response.error = error;
        return response;
    }
    
    // Getters and Setters
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public Long getCount() {
        return count;
    }
    
    public void setCount(Long count) {
        this.count = count;
    }
    
    public List<ResponseItem> getItems() {
        return items;
    }
    
    public void setItems(List<ResponseItem> items) {
        this.items = items;
    }
}
