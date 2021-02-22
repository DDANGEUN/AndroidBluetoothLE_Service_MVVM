package com.lilly.ble

import android.bluetooth.*
import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lilly.ble.util.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


class MyRepository {

    private val TAG = "MyRepository"

    var statusTxt: String = ""
    var txtRead: String = ""
    var isConnected = MutableLiveData<Event<Boolean>>()

    var isStatusChange: Boolean = false
    var isTxtRead: Boolean = false



    var mService: BleGattService? = null
    var mBound: Boolean? = null


    var deviceToConnect: BluetoothDevice? = null


    fun fetchReadText() = flow{
        while(true) {
            if(isTxtRead) {
                emit(txtRead)
                isTxtRead = false
            }
        }
    }.flowOn(Dispatchers.Default)
    fun fetchStatusText() = flow{
        while(true) {
            if(isStatusChange) {
                emit(statusTxt)
                isStatusChange = false
            }
        }
    }.flowOn(Dispatchers.Default)







    /**
     * Handles various events fired by the Service.
     */
    private var mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG,"action ${intent.action}")
            when(intent.action){
                ACTION_GATT_CONNECTED->{
                    isConnected.postValue(Event(true))
                    intent.getStringExtra(MSG_DATA)?.let{
                        statusTxt = it
                        isStatusChange = true
                    }

                }
                ACTION_GATT_DISCONNECTED->{
                    MyApplication.applicationContext().unbindService(mServiceConnection)
                    isConnected.postValue(Event(false))
                    intent.getStringExtra(MSG_DATA)?.let{
                        statusTxt = it
                        isStatusChange = true
                    }
                }
                ACTION_STATUS_MSG->{
                    intent.getStringExtra(MSG_DATA)?.let{
                        statusTxt = it
                        isStatusChange = true
                    }
                }
                ACTION_READ_DATA->{
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))

                    intent.getStringExtra(EXTRA_DATA)?.let{
                        txtRead = it
                        isTxtRead = true
                    }
                }
            }

        }
    }

    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("hereigo", "ServiceConnection: connected to service.")
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as BleGattService.LocalBinder
            mService = binder.service
            mBound = true
            mService?.connectDevice(deviceToConnect)
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            Log.d("hereigo", "ServiceConnection: binding died service.")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("hereigo", "ServiceConnection: disconnected from service.")
            mBound = false
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            Log.d("hereigo", "ServiceConnection: nullbinding")
        }
    }

    fun registerGattReceiver(){
        MyApplication.applicationContext().registerReceiver(mGattUpdateReceiver,
            makeGattUpdateIntentFilter())
    }
    fun unregisterReceiver(){
        MyApplication.applicationContext().unregisterReceiver(mGattUpdateReceiver)
    }
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_READ_DATA)
        intentFilter.addAction(ACTION_STATUS_MSG)
        return intentFilter
    }




    /**
     * Connect to the ble device
     */
    fun connectDevice(device: BluetoothDevice?) {
        deviceToConnect = device
        // Bind to LocalService
        Intent(MyApplication.applicationContext(), BleGattService::class.java).also { intent ->
            MyApplication.applicationContext().bindService(
                intent,
                mServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }



    /**
     * Disconnect Gatt Server
     */
    fun disconnectGattServer() {
        mService?.disconnectGattServer("Disconnected")
        deviceToConnect = null
    }

    fun writeData(cmdByteArray: ByteArray){
        mService?.writeData(cmdByteArray)
    }

}