package com.appdev.siptelephonycore;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.sip.SipAudioCall
import android.net.sip.SipException
import android.net.sip.SipProfile
import android.util.Log
import androidx.appcompat.app.AlertDialog

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mainActivity = context as MainActivity
        lateinit var incomingCall: SipAudioCall
        val listener: SipAudioCall.Listener = object : SipAudioCall.Listener() {
            override fun onRinging(call: SipAudioCall, caller: SipProfile) {
                try {
                    Log.e("IncomingCallReceiver","onRinging");
                    call.answerCall(30)
                } catch (e: SipException) {
                    e.printStackTrace()
                }
            }

            override fun onCalling(call: SipAudioCall) {
                super.onCalling(call)
                mainActivity.updateStatus(STATE_CALLING + " " + call.peerProfile.userName)
            }

            override fun onCallEnded(call: SipAudioCall) {
                super.onCallEnded(call)
                mainActivity.updateStatus(STATE_CONNECTED)
            }
        }

        incomingCall = try {
            mainActivity.sipManager.takeAudioCall(intent, listener)
        } catch (e: SipException) {
            e.printStackTrace()
            return
        }

        val call = incomingCall
        val builder = AlertDialog.Builder(context)
        Log.e("IncomingCallReceiver","onRinging");
        builder.setTitle("New Call")
            .setMessage("From " + incomingCall.peerProfile.userName)
            .setPositiveButton("Accept") { dialog, which ->
                try {
                    call.answerCall(30)
                    call.startAudio()
                    call.setSpeakerMode(true)
                    if (call.isMuted) call.toggleMute()
                } catch (e: SipException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Deny") { dialog, which ->
                try {
                    call.endCall()
                    call.close()
                } catch (e: SipException) {
                    e.printStackTrace()
                }
            }
        builder.create().show()
    }
}