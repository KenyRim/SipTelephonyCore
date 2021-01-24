package com.appdev.siptelephonycore

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.sip.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.text.ParseException

class MainActivity : AppCompatActivity() {

    private lateinit var sipProfile: SipProfile

    private var call: SipAudioCall? = null
    lateinit var callReceiver: IncomingCallReceiver
    val sipManager: SipManager by lazy(LazyThreadSafetyMode.NONE) {
        SipManager.newInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (
            SipManager.isVoipSupported(this) &&
            SipManager.isApiSupported(this)
        ) {
            val permission = PermissionsHelper().checkAndRequestCallPermissions(this)

            if (permission) {

                Log.e("permission","ok!")
                initSip()
                initComp()
            }else{
                Log.e("permission","fail!")
            }

        } else {
            Toast.makeText(this, "Sorry, this device unsupported!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btn_call.setOnClickListener {
            callPhone()
        }

        btn_call_end.setOnClickListener {
            endPhone()
        }
    }

    private fun initSip() {

        try {
            connectSip()
        } catch (e: ParseException) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun initComp() {
        val filter = IntentFilter()
        filter.addAction(CALL_ACTION)
        callReceiver = IncomingCallReceiver()
        this.registerReceiver(callReceiver, filter)
    }

    private fun callPhone() {
        try {
            sipProfile = SipProfile.Builder(SIP_NAME, SIP_DOMAIN).setPassword(SIP_PASSWORD).build()
            val callUsername: String = et_phone.text.toString()
            if (TextUtils.isEmpty(callUsername)) return
            val sipTarget = SipProfile.Builder(callUsername, SIP_DOMAIN).build()
            call = sipManager.makeAudioCall(
                sipProfile.uriString,
                sipTarget.uriString,
                audioCallListener,
                30
            )
        } catch (e: SipException) {
            Log.e("SipException1","${e.message}")
        } catch (e: ParseException) {
            Log.e("SipException2","${e.message}")
        }
    }

    private fun endPhone() {
        try {
            if (call != null) {
                call!!.endCall()
                call!!.close()
            }
            updateStatus(STATE_CONNECTED)
        } catch (e: SipException) {
            e.printStackTrace()
        }
    }

    private val audioCallListener: SipAudioCall.Listener = object : SipAudioCall.Listener() {
        override fun onCallEstablished(call: SipAudioCall) {
            call.startAudio()
            call.setSpeakerMode(true)
            call.toggleMute()
        }

        override fun onCallEnded(call: SipAudioCall) {
            super.onCallEnded(call)
            updateStatus(STATE_CONNECTED)
        }

        override fun onCalling(call: SipAudioCall) {
            if (!call.isMuted) call.toggleMute()
            updateStatus(STATE_CALLING + " " + call.peerProfile.userName)
        }
    }


    private fun connectSip() {
        try {
            val intent = Intent()
            intent.action = CALL_ACTION
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                intent,
                Intent.FILL_IN_DATA
            )
            sipProfile = SipProfile.Builder(SIP_NAME, SIP_DOMAIN).setPassword(SIP_PASSWORD).build()
            sipManager.open(sipProfile, pendingIntent, null)
            sipManager.setRegistrationListener(
                sipProfile.uriString,
                object : SipRegistrationListener {
                    override fun onRegistering(localProfileUri: String) {
                        Log.e("onRegistering",STATE_CONNECTING)
                        updateStatus(STATE_CONNECTING)
                    }

                    override fun onRegistrationDone(localProfileUri: String, expiryTime: Long) {
                        Log.e("onRegistrationDone",STATE_CONNECTED)
                        updateStatus(STATE_CONNECTED)
                    }

                    override fun onRegistrationFailed(
                        localProfileUri: String,
                        errorCode: Int,
                        errorMessage: String
                    ) {
                        Log.e("onRegistering", "$STATE_CONNECTED_FAILURE $errorCode $errorMessage")
                        updateStatus(STATE_CONNECTED_FAILURE)
                    }
                })
        } catch (e: SipException) {
            e.printStackTrace()
            Log.e("connectSip", e.localizedMessage)
        }
    }

    fun updateStatus(string: String) {
        val status = findViewById<TextView>(R.id.tv_status)
        status.text = "Sip status: $string"
    }


    override fun onDestroy() {
        super.onDestroy()
        closeLocalProfile()
        endPhone()
        unregisterReceiver(callReceiver)

    }

    private fun closeLocalProfile() {
        try {
            sipManager.close(sipProfile.uriString)
        } catch (e: SipException) {
            e.printStackTrace()
            Log.e("closeLocalProfile", "Failed to close SipProfile: " + e.message)
        }
    }
}



