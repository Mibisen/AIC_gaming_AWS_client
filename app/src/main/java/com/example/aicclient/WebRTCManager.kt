package com.example.aicclient

import android.content.Context
import org.webrtc.*

class WebRTCManager(
    private val context: Context,
    private val signalingClient: SignalingClient,
    private val eglBaseContext: EglBase.Context
) {
    var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    var dataChannel: DataChannel? = null

    init {
        // Initialize WebRTC
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        // CRITICAL: Hardware decoding, prioritize H264
        val videoDecoderFactory = HardwareVideoDecoderFactory(eglBaseContext)
        val videoEncoderFactory = HardwareVideoEncoderFactory(
            eglBaseContext, true, true
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(observer: PeerConnection.Observer) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        
        // Create Data Channel for Touch Injection
        val dcInit = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("control", dcInit)
    }

    fun handleOffer(sdp: SessionDescription, observer: SdpObserver) {
        peerConnection?.setRemoteDescription(observer, sdp)
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(answerSdp: SessionDescription) {
                peerConnection?.setLocalDescription(observer, answerSdp)
                signalingClient.sendAnswer(answerSdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {}
            override fun onSetFailure(s: String?) {}
        }, MediaConstraints())
    }
    
    fun handleAnswer(sdp: SessionDescription, observer: SdpObserver) {
        peerConnection?.setRemoteDescription(observer, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun dispose() {
        dataChannel?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
    }
}
