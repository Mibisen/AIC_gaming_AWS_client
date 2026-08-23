package com.example.aicclient

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.aicclient.theme.AICClientTheme
import org.webrtc.*

class MainActivity : ComponentActivity(), SignalingListener {

    private lateinit var eglBase: EglBase
    private lateinit var signalingClient: SignalingClient
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var touchHandler: TouchHandler

    private var videoTrack: VideoTrack? = null
    private var isConnected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immersive Mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Lock to landscape (for gaming)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Initialize EGL base
        eglBase = EglBase.create()
        
        signalingClient = SignalingClient("ws://192.168.1.100:8000", this)
        webRTCManager = WebRTCManager(this, signalingClient, eglBase.eglBaseContext)
        
        webRTCManager.createPeerConnection(object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    isConnected = true
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate) {
                signalingClient.sendIceCandidate(candidate)
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                if (receiver?.track() is VideoTrack) {
                    videoTrack = receiver.track() as VideoTrack
                }
            }
        })
        
        touchHandler = TouchHandler(webRTCManager.dataChannel)

        // Connect signaling
        signalingClient.connect()

        setContent {
            AICClientTheme {
                val screenWidth = LocalConfiguration.current.screenWidthDp
                val screenHeight = LocalConfiguration.current.screenHeightDp
                
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInteropFilter { event ->
                            // Here we capture native touch events from Compose 
                            // and push them into our TouchHandler to serialize to WebRTC
                            touchHandler.handleTouchEvent(event, screenWidth, screenHeight)
                            true
                        },
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(eglBase.eglBaseContext, null)
                            setEnableHardwareScaler(true)
                            setMirror(false)
                        }
                    },
                    update = { renderer ->
                        videoTrack?.addSink(renderer)
                    }
                )
            }
        }
    }

    override fun onOfferReceived(description: SessionDescription) {
        webRTCManager.handleOffer(description, object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        })
    }

    override fun onAnswerReceived(description: SessionDescription) {
        webRTCManager.handleAnswer(description, object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        })
    }

    override fun onIceCandidateReceived(candidate: IceCandidate) {
        webRTCManager.addIceCandidate(candidate)
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingClient.close()
        webRTCManager.dispose()
        eglBase.release()
    }
}
