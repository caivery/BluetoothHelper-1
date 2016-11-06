package top.wuhaojie.bthelper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import top.wuhaojie.bthelper.bean.MessageItem;
import top.wuhaojie.bthelper.i.IConnectionListener;
import top.wuhaojie.bthelper.i.OnAcceptListener;
import top.wuhaojie.bthelper.i.OnReceiveMessageListener;
import top.wuhaojie.bthelper.i.OnSearchDeviceListener;
import top.wuhaojie.bthelper.i.OnSendMessageListener;
import top.wuhaojie.bthelper.receiver.BroadcastType;
import top.wuhaojie.bthelper.receiver.BtReceiver;
import top.wuhaojie.bthelper.runn.AcceptRunnable;
import top.wuhaojie.bthelper.runn.ConnectDeviceRunnable;
import top.wuhaojie.bthelper.runn.ReadRunnable;
import top.wuhaojie.bthelper.runn.WriteRunnable;

/**
 * Bluetooth Helper as a Client.
 * Created by wuhaojie on 2016/9/7 18:57.
 */
public class BtHelperClient {

    private static final String DEVICE_HAS_NOT_BLUETOOTH_MODULE = "device has not bluetooth module!";

    private Context mContext;
    private ConnectDeviceRunnable mConnectDeviceRunnable;
    private AcceptRunnable mAcceptRunnable;
    private BluetoothAdapter mBluetoothAdapter;
//    private volatile BtReceiver mReceiver;
//    private static volatile BtHelperClient sBtHelperClient;
    private BtReceiver mReceiver;
    private static BtHelperClient sBtHelperClient;
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();
    private static int type = Constants.CONNECT_TYPE_SERVER;

    /**
     * Obtains the BtHelperClient from the given context.
     *
     * @param context context
     */
    public static void init(Context context) {
        if (sBtHelperClient == null) {
            synchronized (BtHelperClient.class) {
                if (sBtHelperClient == null)
                    sBtHelperClient = new BtHelperClient(context);
            }
        }
    }

    public static BtHelperClient getDefault(){
        if( null == sBtHelperClient){
            throw new NullPointerException("you must be init BtHelperClient before you use it");
        }

        return sBtHelperClient;
    }

    /**
     * private constructor for singleton
     *
     * @param context context
     */
    private BtHelperClient(Context context) {
        mContext = context.getApplicationContext();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        requestEnableBt();
        initAcceptRunn();
    }

    private void initAcceptRunn() {

        OnAcceptListener onAcceptListener = new OnAcceptListener() {

            @Override
            public void OnAcceptSuccess(BluetoothDevice device) {
                Intent intent = new Intent(BroadcastType.BROADCAST_TYPE_ACCEPT_CONNECTION);
                intent.putExtra(Constants.REMOTE_DEVICE, device);
                mContext.sendBroadcast(intent);
                receiveMessage();
            }

            @Override
            public void OnAcceptFailed(Exception e) {

            }
        };

        mAcceptRunnable = new AcceptRunnable(onAcceptListener);
        mExecutorService.submit(mAcceptRunnable);
    }


    /**
     * Request for enable the device's bluetooth asynchronously.
     * Throw a NullPointerException if the device doesn't have a bluetooth module.
     */
    private void requestEnableBt() {
        if (mBluetoothAdapter == null) {
            throw new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE);
        }
        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();
    }


    /**
     * discovery the devices.
     *
     * @param listener listener for the process
     */
    public void searchDevices(final OnSearchDeviceListener listener) {

        checkNotNull(listener);

        if (mBluetoothAdapter == null) {
            listener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
            return;
        }



        if (mReceiver == null){
            mReceiver = new BtReceiver(listener);
        }

        // ACTION_FOUND
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, filter);

        // ACTION_DISCOVERY_FINISHED
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mReceiver, filter);

        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();

        mBluetoothAdapter.startDiscovery();
        listener.onStartDiscovery();
    }

    public void searchDevices(){

    }


    /**
     * Send a message to a remote device.
     * If the local device did't connected to the remote devices, it will call connectDevice(), then send the message.
     * If you want to get a response from the remote device, call another overload method, this method default will not obtain a response.
     *
     * @param item     the message need to send
     * @param listener lister for the sending process
     */
    public void sendMessage(MessageItem item, OnSendMessageListener listener) {
//        WriteRunnable writeRunnable = new WriteRunnable(item,listener,mConnectDeviceRunnable.getOutputStream());
//        mExecutorService.submit(writeRunnable);

        WriteRunnable writeRunnable;
        if( type == Constants.CONNECT_TYPE_CLIENT ){
            writeRunnable = new WriteRunnable(item,listener,mConnectDeviceRunnable.getOutputStream());
            mExecutorService.submit(writeRunnable);
        }else if( type == Constants.CONNECT_TYPE_SERVER){
            writeRunnable = new WriteRunnable(item,listener,mAcceptRunnable.getOutputStream());
            mExecutorService.submit(writeRunnable);
        }
    }

    private void receiveMessage() {

        OnReceiveMessageListener onReceiveMessageListener = new OnReceiveMessageListener() {
            @Override
            public void onNewLine(String s) {
                Intent intent = new Intent(BroadcastType.BROADCAST_TYPE_RECEIVED_MESSAGE);
                intent.putExtra("message", s);
                mContext.sendBroadcast(intent);
            }

            @Override
            public void onConnectionLost() {
                Intent intent = new Intent(BroadcastType.BROADCAST_TYPE_CONNECTION_LOST);
                mContext.sendBroadcast(intent);
            }

            @Override
            public void onError(Exception e) {

            }
        };

//        ReadRunnable readRunnable = new ReadRunnable(onReceiveMessageListener, mConnectDeviceRunnable.getInputStream());
//        mExecutorService.submit(readRunnable);

        ReadRunnable readRunnable;
        if( type == Constants.CONNECT_TYPE_CLIENT){
            readRunnable = new ReadRunnable(onReceiveMessageListener, mConnectDeviceRunnable.getInputStream());
            mExecutorService.submit(readRunnable);
        }else if( type == Constants.CONNECT_TYPE_SERVER){
            readRunnable = new ReadRunnable(onReceiveMessageListener, mAcceptRunnable.getInputStream());
            mExecutorService.submit(readRunnable);
        }

    }

    /**
     * 如果你想和其他蓝牙设备进行通信，你必须先调用此方法
     * @param mac device address
     * @param listener connection listener
     */
    public void connectDevice(String mac, final IConnectionListener listener) {

        //主动连接 代表本机是主机
        type = Constants.CONNECT_TYPE_CLIENT;

        if( mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        if (mac == null || TextUtils.isEmpty(mac))
            throw new IllegalArgumentException("mac address is null or empty!");
        if (!BluetoothAdapter.checkBluetoothAddress(mac))
            throw new IllegalArgumentException("mac address is not correct! make sure it's upper case!");

        IConnectionListener iConnectionListener = new IConnectionListener() {
            @Override
            public void OnConnectionStart() {
                listener.OnConnectionStart();
            }

            @Override
            public void OnConnectionSuccess() {
                listener.OnConnectionSuccess();
                //一旦连接成功就开始监听是否有数据发过来
                receiveMessage();
            }

            @Override
            public void OnConnectionFailed(Exception e) {
                listener.OnConnectionFailed(e);
            }
        };

        mConnectDeviceRunnable = new ConnectDeviceRunnable(mac, iConnectionListener);
        checkNotNull(mExecutorService);

        mExecutorService.submit(mConnectDeviceRunnable);

    }


    private void checkNotNull(Object o) {
        if (o == null)
            throw new NullPointerException();
    }

    public  ArrayList<BluetoothDevice> getBondedDevices(){
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> bondedDevices = new ArrayList<>();
        for(BluetoothDevice device:devices){
            bondedDevices.add(device);
            Log.d("TAG", "NAME :" + device.getAddress());
        }
        return bondedDevices;
    }

    /**
     * Closes the connection and releases any system resources associated
     * with the stream.
     */
    public void close() {
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();

        if( null != mReceiver){
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        //如果是服务端 ，就要重开一个线程，再次监听连接请求
        if( type == Constants.CONNECT_TYPE_SERVER){
            initAcceptRunn();
        }
    }

}
