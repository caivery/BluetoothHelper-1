package top.wuhaojie.bthelper.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import top.wuhaojie.bthelper.OnBtStateChangeListener;

/**
 * Created by Houxy on 2016/11/3.
 */

public class BtStateReceiver extends BroadcastReceiver{


    public  void OnBtStateOFF(){}
    public  void OnBtStateTurningOFF(){}
    public  void OnBtStateON(){}
    public  void OnBtStateTurningON(){}


    public BtStateReceiver(){
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if( null != intent.getAction()){
            int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            switch (blueState){
                case BluetoothAdapter.STATE_OFF:
                    OnBtStateOFF();
                    break;
                case BluetoothAdapter.STATE_ON:
                    OnBtStateON();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    OnBtStateTurningOFF();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    OnBtStateTurningON();
                    break;
                default:break;
            }
        }

    }
}
