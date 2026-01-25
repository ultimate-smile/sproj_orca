package com.orca.com.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orca.com.websocket.WebSocketResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedUdpResponseSemanticTest {
    private static final String FIXED_RESPONSE_HEX =
        "c9 62 00 b7 04 00 00 00 02 00 00 00 " +
        "db 14 8f 8b 6a 19 5d 40 59 6a bd df 68 19 5d 40 " +
        "01 00 00 00 9a 99 59 3f 64 00 " +
        "76 4f 1e 16 6a 19 5d 40 1d 38 67 44 69 19 5d 40 " +
        "02 00 00 00 66 66 66 3f c8 00 " +
        "01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 14";

    private static final String EXPECTED_TERRAIN_BASE64 = "AQIDBAUGBwgJCgsMDQ4PEBESExQ=";

    @Test
    void testDecodeFixedResponseAndSemanticOutput() throws Exception {
        byte[] data = hexToBytes(FIXED_RESPONSE_HEX);

        UdpResponse response = UdpResponse.decode(data);
        assertEquals(20250125001L, response.getRequestId());
        assertEquals(2L, response.getCount());
        assertEquals(2, response.getItems().size());

        UdpResponse.ResponseItem item1 = response.getItems().get(0);
        assertEquals(116.397128, item1.getALongitude(), 0.000001);
        assertEquals(116.397026, item1.getBLongitude(), 0.000001);
        assertEquals(1L, item1.getType());
        assertEquals(0.85f, item1.getDensity(), 0.0001f);
        assertEquals(100, item1.getField6());
        assertNull(item1.getTerrainData());

        UdpResponse.ResponseItem item2 = response.getItems().get(1);
        assertEquals(116.397100, item2.getALongitude(), 0.000001);
        assertEquals(116.397050, item2.getBLongitude(), 0.000001);
        assertEquals(2L, item2.getType());
        assertEquals(0.90f, item2.getDensity(), 0.0001f);
        assertEquals(200, item2.getField6());
        assertArrayEquals(expectedTerrain(), item2.getTerrainData());

        WebSocketResponse semantic = WebSocketResponse.fromUdpResponse(response, 1);
        assertEquals(1, semantic.getType());
        assertEquals(20250125001L, semantic.getRequestId());
        assertTrue(semantic.isSuccess());
        assertEquals(2L, semantic.getCount());
        assertEquals(2, semantic.getItems().size());
        assertNull(semantic.getItems().get(0).getTerrainData());
        assertEquals(EXPECTED_TERRAIN_BASE64, semantic.getItems().get(1).getTerrainData());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JsonNode actual = mapper.valueToTree(semantic);
        JsonNode expected = mapper.readTree(
            "{\"type\":1,\"requestId\":20250125001,\"success\":true,\"count\":2," +
                "\"items\":[" +
                "{\"aLongitude\":116.397128,\"bLongitude\":116.397026,\"type\":1,\"density\":0.85,\"field6\":100}," +
                "{\"aLongitude\":116.3971,\"bLongitude\":116.39705,\"type\":2,\"density\":0.9,\"field6\":200," +
                "\"terrainData\":\"" + EXPECTED_TERRAIN_BASE64 + "\"}" +
                "]}"
        );
        assertEquals(expected, actual);
    }

    private static byte[] expectedTerrain() {
        byte[] bytes = new byte[20];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i + 1);
        }
        return bytes;
    }

    private static byte[] hexToBytes(String hex) {
        String normalized = hex.replaceAll("\\s+", "");
        if (normalized.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[normalized.length() / 2];
        for (int i = 0; i < data.length; i++) {
            int hi = Character.digit(normalized.charAt(i * 2), 16);
            int lo = Character.digit(normalized.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex at byte " + i);
            }
            data[i] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}
