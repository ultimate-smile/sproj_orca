package com.orca.com.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orca.com.protocol.UdpResponse;
import com.orca.com.protocol.TerrainResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket响应格式
 */
public class WebSocketResponse<T> {
    @JsonProperty("type")
    private int type;
    
    @JsonProperty("requestId")
    private long requestId;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("error")
    private String error;

    @JsonProperty("data")
    private T data;
    
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

    public static class TerrainData {
        @JsonProperty("count")
        private Long count;
        
        @JsonProperty("items")
        private List<ResponseItem> items;

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

    public static class EvaluationConfigData {
        @JsonProperty("testBackground")
        private String testBackground;

        @JsonProperty("evaluationPurpose")
        private String evaluationPurpose;

        @JsonProperty("evalTaskId")
        private Long evalTaskId;

        @JsonProperty("testPlatforms")
        private List<Integer> testPlatforms;

        @JsonProperty("sonarTestLocation")
        private List<Integer> sonarTestLocation;

        @JsonProperty("sonarTestTasks")
        private List<Integer> sonarTestTasks;

        @JsonProperty("testMethod")
        private Integer testMethod;

        public String getTestBackground() {
            return testBackground;
        }

        public void setTestBackground(String testBackground) {
            this.testBackground = testBackground;
        }

        public String getEvaluationPurpose() {
            return evaluationPurpose;
        }

        public void setEvaluationPurpose(String evaluationPurpose) {
            this.evaluationPurpose = evaluationPurpose;
        }

        public Long getEvalTaskId() {
            return evalTaskId;
        }

        public void setEvalTaskId(Long evalTaskId) {
            this.evalTaskId = evalTaskId;
        }

        public List<Integer> getTestPlatforms() {
            return testPlatforms;
        }

        public void setTestPlatforms(List<Integer> testPlatforms) {
            this.testPlatforms = testPlatforms;
        }

        public List<Integer> getSonarTestLocation() {
            return sonarTestLocation;
        }

        public void setSonarTestLocation(List<Integer> sonarTestLocation) {
            this.sonarTestLocation = sonarTestLocation;
        }

        public List<Integer> getSonarTestTasks() {
            return sonarTestTasks;
        }

        public void setSonarTestTasks(List<Integer> sonarTestTasks) {
            this.sonarTestTasks = sonarTestTasks;
        }

        public Integer getTestMethod() {
            return testMethod;
        }

        public void setTestMethod(Integer testMethod) {
            this.testMethod = testMethod;
        }
    }
    
    public WebSocketResponse() {
    }
    
    public static WebSocketResponse<TerrainData> fromUdpResponse(TerrainResponse udpResponse, int requestType) {
        WebSocketResponse<TerrainData> response = new WebSocketResponse<>();
        response.type = requestType;
        response.requestId = udpResponse.getRequestId();
        response.success = true;

        TerrainData terrainData = new TerrainData();
        terrainData.count = udpResponse.getCount();
        terrainData.items = new ArrayList<>();
        
        for (TerrainResponse.ResponseItem item : udpResponse.getItems()) {
            ResponseItem wsItem = new ResponseItem();
            wsItem.aLongitude = item.getALongitude();
            wsItem.bLongitude = item.getBLongitude();
            wsItem.type = item.getType();
            wsItem.density = item.getDensity();
            wsItem.field6 = item.getField6();
            if (item.getTerrainData() != null) {
                wsItem.terrainData = java.util.Base64.getEncoder().encodeToString(item.getTerrainData());
            }
            terrainData.items.add(wsItem);
        }
        
        response.setData(terrainData);
        return response;
    }

    public static WebSocketResponse<EvaluationConfigData> fromEvaluationConfigResponse(com.orca.com.protocol.EvaluationConfigResponse udpResponse, int requestType) {
        WebSocketResponse<EvaluationConfigData> response = new WebSocketResponse<>();
        response.type = requestType;
        response.requestId = udpResponse.getRequestId();
        response.success = true;
        
        EvaluationConfigData configData = new EvaluationConfigData();
        configData.testBackground = udpResponse.getTestBackground();
        configData.evaluationPurpose = udpResponse.getEvaluationPurpose();
        configData.evalTaskId = udpResponse.getEvalTaskId();
        configData.testPlatforms = udpResponse.getTestPlatforms();
        configData.sonarTestLocation = udpResponse.getSonarTestLocation();
        configData.sonarTestTasks = udpResponse.getSonarTestTasks();
        configData.testMethod = udpResponse.getTestMethod();
        
        response.setData(configData);
        return response;
    }
    
    public static WebSocketResponse<Void> error(int requestType, long requestId, String error) {
        WebSocketResponse<Void> response = new WebSocketResponse<>();
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
