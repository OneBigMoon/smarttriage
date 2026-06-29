package com.wcjk.triage.common.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.github.mjdev.libaums.partition.Partition;
import com.wcjk.triage.common.utils.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.wcjk.triage.common.usb.USBBroadCastReceiver.ACTION_USB_PERMISSION;

/**
 * Created by hyc on 2018/7/28
 */
public class UsbHelper {
    private Log log = Log.getLogger(this.getClass());

    //上下文对象
    private Context context;

    //USB 设备列表

    private UsbMassStorageDevice[] storageDevices;

    //USB 广播

    private USBBroadCastReceiver mUsbReceiver;

    //回调

    private USBBroadCastReceiver.UsbListener usbListener;

    //当前路径

    private UsbFile currentFolder = null;

    //TAG

    private static String TAG = "UsbHelper";


    public UsbHelper(Context context, USBBroadCastReceiver.UsbListener usbListener) {

        this.context = context;

        this.usbListener = usbListener;

        //注册广播

        registerReceiver();

    }

    public void destory() {
        if (mUsbReceiver != null) {
            context.unregisterReceiver(mUsbReceiver);
        }
    }

    /**

     * 注册 USB 监听广播

     */

    private void registerReceiver() {

        mUsbReceiver = new USBBroadCastReceiver();

        mUsbReceiver.setUsbListener(usbListener);

        //监听otg插入 拔出

        IntentFilter usbDeviceStateFilter = new IntentFilter();

        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);

        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        context.registerReceiver(mUsbReceiver, usbDeviceStateFilter);

        //注册监听自定义广播

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        context.registerReceiver(mUsbReceiver, filter);

    }



    /**

     * 读取 USB设备列表

     *

     * @return USB设备列表

     */

    public void getDeviceList() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        log.d("开始去读Otg设备");
        storageDevices = UsbMassStorageDevice.getMassStorageDevices(context);
        PendingIntent mPendingIntent =PendingIntent.getBroadcast(context,0,new Intent(ACTION_USB_PERMISSION),0);
        if (storageDevices.length == 0) {
            log.d("没有检测到U盘s");
            return ;
        }

        log.d("检测到U盘数量:" + storageDevices.length);
        for (UsbMassStorageDevice device : storageDevices){
            if (usbManager.hasPermission(device.getUsbDevice())){
                log.d("检测到有权限，延迟1秒开始读取....");
                try {
                    Thread.sleep(1000 );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                readDevice(device);
            }else {
                log.d("检测到有设备，但是没有权限，申请权限....");
                usbManager.requestPermission(device.getUsbDevice(),mPendingIntent);
            }
        }

    }



    /**

     * 获取device 根目录文件

     *

     * @param device USB 存储设备

     * @return 设备根目录下文件列表

     */

    public void readDevice(UsbMassStorageDevice device) {
        try {
            device.init();
            Partition partition = device.getPartitions().get(0);
            log.i("------------partition---------");
            log.i("VolumnLobel:"+partition.getVolumeLabel());
            log.i("blockSize:"+partition.getBlockSize()+"");
            FileSystem currentFs = partition.getFileSystem();
            log.i("------------FileSystem---------");
            UsbFile root = currentFs.getRootDirectory();
            currentFolder = root;
            log.i("------------currentFolder---------" + currentFolder.getName());
            String deviceName = currentFs.getVolumeLabel();
            log.i("volumnLable:"+deviceName);
            log.i("chunkSize:"+currentFs.getChunkSize());
            log.i("freeSize:"+currentFs.getFreeSpace());
            log.i("OccupiedSpcace:"+currentFs.getOccupiedSpace());
            log.i("capacity"+currentFs.getCapacity());
            log.i("rootFile:"+root.toString());
            log.d("正在读取U盘" + deviceName);
//            readFile(root);
        } catch (Exception e) {
            e.printStackTrace();
            log.d("读取失败:"+e.getMessage());
        }finally {
        }


    }



    /**

     * 读取 USB 内文件夹下文件列表

     *

     * @param usbFolder usb文件夹

     * @return 文件列表

     */

    public ArrayList<UsbFile> getUsbFolderFileList(UsbFile usbFolder) {

        //更换当前目录

        currentFolder = usbFolder;

        ArrayList<UsbFile> usbFiles = new ArrayList<>();

        try {

            Collections.addAll(usbFiles, usbFolder.listFiles());

        } catch (IOException e) {

            e.printStackTrace();

        }

        return usbFiles;

    }


    public boolean saveFileDirToUsb(File targetDir, UsbFile saveFolder, DownloadProgressListener progressListener) {
        boolean result;
        if (!targetDir.isDirectory()) return false;
        try {
            //USB文件是否存在
            boolean isExist = false;
            UsbFile saveFileDir = null;
            for (UsbFile usbFileDir : saveFolder.listFiles()) {
                if (usbFileDir.getName().equals(targetDir.getName())) {
                    isExist = true;
                    saveFileDir = usbFileDir;
                }
            }

            if (isExist) {
                //文件已存在，删除文件
                saveFileDir.delete();
            }
            //创建新目录
            saveFileDir = saveFolder.createDirectory(targetDir.getName());

            List<File> subFileList = new ArrayList<>();
            Collections.addAll(subFileList, targetDir.listFiles());
            if (subFileList != null){
                for (int i = 0;i < subFileList.size();i++){
                    saveSDFileToUsb(subFileList.get(i),saveFileDir,progressListener);
                }
            }
            result = true;

        } catch (final Exception e) {

            e.printStackTrace();

            result = false;

        }

        return result;

    }



    /**

     * 复制文件到 USB

     *

     * @param targetFile       需要复制的文件

     * @param saveFolder       复制的目标文件夹

     * @param progressListener 下载进度回调

     * @return 复制结果

     */

    public boolean saveSDFileToUsb(File targetFile, UsbFile saveFolder, DownloadProgressListener progressListener) {

        boolean result;

        try {

            //USB文件是否存在

            boolean isExist = false;

            UsbFile saveFile = null;

            for (UsbFile usbFile : saveFolder.listFiles()) {

                if (usbFile.getName().equals(targetFile.getName())) {

                    isExist = true;

                    saveFile = usbFile;

                }

            }

            if (isExist) {

                //文件已存在，删除文件

                saveFile.delete();

            }

            //创建新文件

            saveFile = saveFolder.createFile(targetFile.getName());

            //开始写入

            FileInputStream fis = new FileInputStream(targetFile);//读取选择的文件的

            int avi = fis.available();

            UsbFileOutputStream uos = new UsbFileOutputStream(saveFile);

            int bytesRead;

            byte[] buffer = new byte[1024 * 8];

            int writeCount = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {

                uos.write(buffer, 0, bytesRead);

                writeCount += bytesRead;

//                Log.e(TAG, "Progress : " + (writeCount * 100 / avi));

                if (progressListener != null) {

                    //回调下载进度

                    progressListener.downloadProgress(writeCount * 100 / avi);

                }

            }

            uos.flush();

            fis.close();

            uos.close();

            result = true;

        } catch (final Exception e) {

            e.printStackTrace();

            result = false;

        }

        return result;

    }



    /**

     * 复制 USB文件到本地

     *

     * @param targetFile       需要复制的文件

     * @param savePath         复制的目标文件路径

     * @param progressListener 下载进度回调

     * @return 复制结果

     */

    public boolean saveUSbFileToLocal(UsbFile targetFile, String savePath,

                                      DownloadProgressListener progressListener) {

        boolean result;

        try {

            //开始写入

            UsbFileInputStream uis = new UsbFileInputStream(targetFile);//读取选择的文件的

            FileOutputStream fos = new FileOutputStream(savePath);

            //这里uis.available一直为0

//            int avi = uis.available();

            long avi = targetFile.getLength();

            int writeCount = 0;

            int bytesRead;

            byte[] buffer = new byte[1024];

            while ((bytesRead = uis.read(buffer)) != -1) {

                fos.write(buffer, 0, bytesRead);

                writeCount += bytesRead;

//                Log.e(TAG, "Progress : write : " + writeCount + " All : " + avi);

                if (progressListener != null) {

                    //回调下载进度

                    progressListener.downloadProgress((int) (writeCount * 100 / avi));

                }

            }

            fos.flush();

            uis.close();

            fos.close();

            result = true;

        } catch (Exception e) {

            e.printStackTrace();

            result = false;

        }

        return result;

    }



    /**

     * 获取上层目录文件夹

     *

     * @return usbFile : 父目录文件 / null ：无父目录

     */

    public UsbFile getParentFolder() {

        if (currentFolder != null && !currentFolder.isRoot()) {

            return currentFolder.getParent();

        } else {

            return null;

        }

    }





    /**

     * 获取当前 USBFolder

     */

    public UsbFile getCurrentFolder() {

        return currentFolder;

    }



    /**

     * 退出 UsbHelper

     */

    public void finishUsbHelper() {

        context.unregisterReceiver(mUsbReceiver);

    }



    /**

     * 下载进度回调

     */

    public interface DownloadProgressListener {

        void downloadProgress(int progress);

    }

}
