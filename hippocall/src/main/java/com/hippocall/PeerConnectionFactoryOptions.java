package com.hippocall;


import org.webrtc.PeerConnectionFactory;

public class PeerConnectionFactoryOptions extends PeerConnectionFactory.Options {

  public PeerConnectionFactoryOptions() {
    this.networkIgnoreMask = 1 << 4;
  }
}
