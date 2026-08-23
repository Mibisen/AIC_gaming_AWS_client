package com.example.aicclient

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalingListener {
    fun onOfferReceived(description: SessionDescription)
    fun onAnswerReceived(description: SessionDescription)
    fun onIceCandidateReceived(candidate: IceCandidate)
}

class SignalingClient(
    private val url: String,
    private val listener: SignalingListener
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect() {
        Log.d("SignalingClient", "Connecting to signaling server at $url")
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SignalingClient", "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SignalingClient", "Received message: $text")
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "offer" -> {
                            val sdp = SessionDescription(
                                SessionDescription.Type.OFFER,
                                json.getString("sdp")
                            )
                            listener.onOfferReceived(sdp)
                        }
                        "answer" -> {
                            val sdp = SessionDescription(
                                SessionDescription.Type.ANSWER,
                                json.getString("sdp")
                            )
                            listener.onAnswerReceived(sdp)
                        }
                        "candidate" -> {
                            val candidate = IceCandidate(
                                json.getString("sdpMid"),
                                json.getInt("sdpMLineIndex"),
                                json.getString("candidate")
                            )
                            listener.onIceCandidateReceived(candidate)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error parsing message", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SignalingClient", "WebSocket closed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SignalingClient", "WebSocket failure", t)
            }
        })
    }

    fun sendOffer(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
        }
        webSocket?.send(json.toString())
    }

    fun sendAnswer(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp.description)
        }
        webSocket?.send(json.toString())
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val json = JSONObject().apply {
            put("type", "candidate")
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("sdpMid", candidate.sdpMid)
            put("candidate", candidate.sdp)
        }
        webSocket?.send(json.toString())
    }

    fun close() {
        webSocket?.close(1000, "Client closed")
    }
}
