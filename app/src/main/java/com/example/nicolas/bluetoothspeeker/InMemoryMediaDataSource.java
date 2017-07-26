package com.example.nicolas.bluetoothspeeker;

import android.media.MediaDataSource;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.M)
public class InMemoryMediaDataSource extends MediaDataSource {
    private static final String TAG = "InMemoryMediaDataSource";
    private byte[] data;
    private int pos; //data[0...pos] is valid

    InMemoryMediaDataSource(int size) {
        data = new byte[size];
        pos = 0;

        // BEGIN HACK

        /*
        try {
            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Music");
            File[] songlist = path.listFiles();
            FileInputStream in = new FileInputStream(songlist[0].getAbsolutePath());
            DataInputStream di = new DataInputStream(in);
            data = new byte[in.available()];
            pos = in.available();
            di.readFully(data);

        } catch (IOException e) {
            e.printStackTrace();
        } */
    }

    @Override
    public synchronized int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        Log.d(TAG, "readAt(): " + "position:" + position + " pos:" + pos + " size:" + size);

        if(position == data.length) return -1; // EOF
        if(position + size > data.length) size -= (position + size) - data.length; // truncate at last part

        while(! (position+size < pos)){
            Log.d(TAG, "readAt(): Waiting");
            try {
                //if in here, something's wrong and not working properly...
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.arraycopy(data, (int)position, buffer, offset, size);

        return size;
    }

    @Override
    public synchronized long getSize() throws IOException {
        return -1;
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "Close");
    }

    public synchronized void write(byte[] buffer) {
        System.arraycopy(buffer, 0, data, pos, buffer.length);
        Log.d(TAG, "write(): " + "buffer.length: " + buffer.length + "pos: " + pos);
        pos += buffer.length;
        notifyAll();
    }

}
