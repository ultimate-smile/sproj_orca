package com.orca.com.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orca.udp")
public class UdpProperties {
  private String listenHost = "0.0.0.0";
  private int listenPort = 19210;

  private String sendHost = "0.0.0.0";
  private int sendPort = 19211;

  private int maxDatagramSize = 1400;
  private long reassemblyTimeoutMs = 3000;

  public String getListenHost() {
    return listenHost;
  }

  public void setListenHost(String listenHost) {
    this.listenHost = listenHost;
  }

  public int getListenPort() {
    return listenPort;
  }

  public void setListenPort(int listenPort) {
    this.listenPort = listenPort;
  }

  public String getSendHost() {
    return sendHost;
  }

  public void setSendHost(String sendHost) {
    this.sendHost = sendHost;
  }

  public int getSendPort() {
    return sendPort;
  }

  public void setSendPort(int sendPort) {
    this.sendPort = sendPort;
  }

  public int getMaxDatagramSize() {
    return maxDatagramSize;
  }

  public void setMaxDatagramSize(int maxDatagramSize) {
    this.maxDatagramSize = maxDatagramSize;
  }

  public long getReassemblyTimeoutMs() {
    return reassemblyTimeoutMs;
  }

  public void setReassemblyTimeoutMs(long reassemblyTimeoutMs) {
    this.reassemblyTimeoutMs = reassemblyTimeoutMs;
  }
}

