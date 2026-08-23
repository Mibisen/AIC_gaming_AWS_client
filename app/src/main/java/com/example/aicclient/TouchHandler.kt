package com.example.aicclient

import android.view.MotionEvent
import org.webrtc.DataChannel
import java.nio.ByteBuffer

class TouchHandler(private val dataChannel: DataChannel?) {

    fun handleTouchEvent(event: MotionEvent, screenWidth: Int, screenHeight: Int): Boolean {
        if (dataChannel == null || dataChannel.state() != DataChannel.State.OPEN) {
            return false
        }

        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        // Normalize X,Y
        val x = (event.getX(pointerIndex) / screenWidth.toFloat()).coerceIn(0f, 1f)
        val y = (event.getY(pointerIndex) / screenHeight.toFloat()).coerceIn(0f, 1f)

        // Serialize into Scrcpy Control Message format (Basic Structure)
        // Scrcpy Inject Touch Event:
        // type (1 byte) = 2 (SC_CONTROL_MSG_TYPE_INJECT_TOUCH_EVENT)
        // action (1 byte)
        // pointerId (8 bytes)
        // position x (4 bytes)
        // position y (4 bytes)
        // video width (2 bytes)
        // video height (2 bytes)
        // pressure (2 bytes)
        // buttons (4 bytes)
        
        val buffer = ByteBuffer.allocate(28)
        buffer.put(2.toByte()) // SC_CONTROL_MSG_TYPE_INJECT_TOUCH_EVENT
        
        val scrcpyAction = when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 0 // AMOTION_EVENT_ACTION_DOWN
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> 1 // AMOTION_EVENT_ACTION_UP
            MotionEvent.ACTION_MOVE -> 2 // AMOTION_EVENT_ACTION_MOVE
            else -> return false
        }
        
        buffer.put(scrcpyAction.toByte())
        buffer.putLong(pointerId.toLong())
        
        // Use normalized coordinates mapped to a theoretical 10000x10000 plane 
        // or just absolute pixel coordinates based on remote screen size.
        // For MVP, sending raw normalized * screen size
        buffer.putInt((x * screenWidth).toInt())
        buffer.putInt((y * screenHeight).toInt())
        
        buffer.putShort(screenWidth.toShort())
        buffer.putShort(screenHeight.toShort())
        
        // Pressure
        val pressure = (event.getPressure(pointerIndex) * 65535f).toInt().toShort()
        buffer.putShort(pressure)
        
        // Buttons
        buffer.putInt(0) // Default no buttons for touch
        
        buffer.flip()
        
        val dataBuffer = DataChannel.Buffer(buffer, true)
        dataChannel.send(dataBuffer)
        
        return true
    }
}
