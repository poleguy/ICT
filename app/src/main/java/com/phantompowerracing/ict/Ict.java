package com.phantompowerracing.ict;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by poleguy on 2/3/2017.
 */
public class Ict {


    private List<SpeedCallback> callbacks = new ArrayList<SpeedCallback>();
    private TcpClient mTcpClient;
    private String mIpAddress;
    private int mPort;
    public int corruptReadCount = 0;
    public int totalReadCount = 0;
    public int goodReadCount = 0;
    public double i1 = 0.0;
    public double i2 = 0.0;
    public double iThrottle1 = 0.0;
    public double iThrottle2 = 0.0;
    public double pwm1 = 0.0;
    public double pwm2 = 0.0;
    public double mph1 = 0.0;
    public double mph2 = 0.0;
    public double rpm1 = 0.0;
    public double rpm2 = 0.0;



    public double readRate = 0;
    private long tStart;
    public String logFilename = "sdcard/ict.txt";
    public String rawLogFilename = "sdcard/ict_raw.txt";
    IctLog ictLog = new IctLog(logFilename);
    IctLog ictRawLog = new IctLog(rawLogFilename);

    public void register(SpeedCallback callback) {
        callbacks.add(callback);
    }

    // constructor
    public Ict(String ipAddress, int port) {
        mIpAddress = ipAddress;
        mPort = port;
    }

    //# returns 32 bit int
    Integer parse(String rawStr) {
        return parse(rawStr, null);
    }
    //# returns 32 bit int
    Integer parse(String rawStr, Integer expectedAddress) {
        //#print "parse"
        //#expected format:
        //#addr:
        //00000010 = 0100001e
        String[] list = rawStr.split(" ");
        Integer data;
        // validate address:
        if(expectedAddress != null) {
            Integer address;
            try {
                address = Integer.parseInt(list[1], 16); //#raw 32 bit value
                //#print("raw: " + str(raw))
                //#data = fi(raw, 1, 7, 0)#change to 11
                //#print("data: " + str(data))
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                address = null;
            }
            if((address == null) || (address.intValue() != expectedAddress.intValue())) {
                //corruptReadCount+=1;
                return null;
            }
        }

        //#print(list)
        try {
            data = Integer.parseInt(list[3], 16); //#raw 32 bit value
            //#print("raw: " + str(raw))
            //#data = fi(raw, 1, 7, 0)#change to 11
            //#print("data: " + str(data))
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            //print(e)
            //throw e;
            //ignore!
            data = null;
        }
        return data;
    }

    class RegisterValue {
        long address;
        long data;
        RegisterValue(long a,long d) {
            address = a;
            data = d;
        }
    }
    //# returns 32 bit int for address and data
    RegisterValue parseRead(String rawStr) {
        //#print "parse"
        //#expected format:
        //#addr:
        //00000010 = 0100001e
        Long data = null;
        Long address = null;

        String[] list = rawStr.split(" ");
        //Integer[] ret = null;

        RegisterValue reg = null;
        if(list[0].equals("addr:")) {
            // validate address:
            try {
                address = Long.parseLong(list[1], 16); //#raw 32 bit value
                //#print("raw: " + str(raw))
                //#data = fi(raw, 1, 7, 0)#change to 11
                //#print("data: " + str(data))
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                address = null;
            }

            //#print(list)
            try {
                data = Long.parseLong(list[3], 16); //#raw 32 bit value
                //#print("raw: " + str(raw))
                //#data = fi(raw, 1, 7, 0)#change to 11
                //#print("data: " + str(data))
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                //print(e)
                //throw e;
                //ignore!
                data = null;
            }
            if ((address == null) ||
                    (data == null)) {
                corruptReadCount += 1;
                return null;
            }
            reg = new RegisterValue(address,data);
        }
        return reg;
    }

    //# returns list of 32 bit ints
    List<Integer> parseList(String rawStr, int length) throws NumberFormatException {
        //#print "parse"
        String[] list = rawStr.split("\n");
        //# print(list)
        List<Integer> data = new ArrayList<Integer>();
        //data = [];
        for (int n = 0; n < length; n++) {
            data.add(Integer.parseInt(list[n], 16)); //#raw 32 bit value
            //#print("raw: " + str(raw))
            //#data = fi(raw,1,7,0) # change to 11
            //#print("data: " + str(data))
        }
        return data;
    }

//    def _readline():
//            # print('in readline')
//    data = ""
//            while 1:
//            #karen = client_socket.recv(1)
//            #karen = str(phantomLink.read(1))
//            try:
//    karen = str(phantomLink.read(1).decode('latin-1'))
//    printf(karen)
//    except serial.SerialException as e:
//    print("serial exception")
//    #return data
//            raise
//    except socket.timeout as e:
//    print("can't read, timeout: {}".format(e))
//            #print "error: "+ os.strerror(value)
//            #raise ValueError('timeout')
//    #return data
//            raise
//    except UnicodeEncodeError as e:
//            if 'ordinal not in range' in str(e):
//    karen = ' '
//    print('ordinal not in range... substituting')
//    if karen == '\r':
//            # ignore CR
//    pass
//    if karen == '\n':
//            # handle newline
//    data += karen
//    #printf('\n')
//    # print('readline saw: %s'%data)
//    return data
//    else:
//            # accumulate
//    data += karen
//
    double fi(long v,int s,int i,int f) { //: # value, signed, int bits, fractional bits
        //    # takes a fixed point value and converts it to a double
        int bits = s + i + f;
        double s_adj = 0; // default
        if (s != 0) {
            double s_bit = Math.floor(v / (Math.pow(2, (bits - 1))));
            s_adj = -((Math.pow(2, bits)) * s_bit);
        } else {
            s_adj = 0; // explicit
        }
        double d = (v + s_adj) / (Math.pow(2, f));
        return d;
    }


    long bitSliceGet(long v,int lidx,int ridx) {
        // #print("idx: %d %d"%(lidx,ridx))
        long mask = (1 << (lidx + 1 - ridx)) - 1;
        // # print("mask: %08x"%mask)
        long shifted = v >> ridx;
        long slice = shifted & mask;
        return slice;
    }

//    # returns a bit slice out of a vector from the fifo
//    # 64 bits at a time
//    # from addresses 0x13 and 0x14
//    def parse_fifo(ints):
//            # this returns a dictionary
//            d = {}
//    #print "parse"
//            #print(ints)
//    data=np.zeros((len(ints[::2]),2),dtype=np.uint32)
//    data[:,0] = ints[::2]
//    data[:,1] = ints[1::2]
//
//            # // 0 is 31 downto 0
//            # // 1 is 63 downto 32
//            #print(data)
//    d['pwm'] = fixed.array_fixed(bitsliceget(data[:,0],31,24),0,0,8)*100.0 # percent
//    d['i_target'] = fixed.array_fixed(bitsliceget(data[:,0],23,12),1,11,0) / 2048.0 * 250.0 # amps
//    d['i1'] = fixed.array_fixed(bitsliceget(data[:,0],11,0),1,11,0) / 2048.0 * 250.0 # amps
//    #print(d['i1'])
//    d['diff_mon'] = fixed.array_fixed(bitsliceget(data[:,1],31,21),1,0,9)*100.0 # percent
//    d['hall_2'] = bitsliceget(data[:,1],20,20)
//    d['hall_1'] = bitsliceget(data[:,1],19,19)
//    d['hall_0'] = bitsliceget(data[:,1],18,18)
//    d['int_mon'] = fixed.array_fixed(bitsliceget(data[:,1],17,0),1,0,17)*100.0 # percent
//    return d
//
//    def read_bytes(self, length):
//            # print('in readline')
//    data = []
//    cnt = 0
//            while 1:
//            #karen = client_socket.recv(1)
//            #karen = str(phantomLink.read(1))
//            try:
//    d = phantomLink.read(1)
//    o = ord(d)
//    # print(o)
//    data.append(o)
//    cnt+=1
//            # print(cnt)
//    except serial.SerialException as e:
//    print("serial exception")
//    #return data
//            raise
//    except socket.timeout as e:
//    print("can't read, timeout: {}".format(e))
//            #print "error: "+ os.strerror(value)
//            #raise ValueError('timeout')
//    #return data
//            raise
//    except UnicodeEncodeError as e:
//            if 'ordinal not in range' in str(e):
//    karen = ' '
//    print('oordinal not in range... substituting')
//    if cnt == length:
//            # print('got %d bytes'%length)
//    return data
//    def dump(self,start, stop, length):
//            # http://stackoverflow.com/questions/14678132/python-hexadecimal
//    data = "d %08x %08x %08x\r\n"%(start, stop,length) #
//            # print(data)
//    phantomLink.write(data)
//            # first read echoed command
//    data = _readline()
//    # then read result
//            data = ""
//    for i in range(0,length):
//    data = data + _readline()
//    self.vals = parse_list(data,length)
//    # print(self.vals)
//
//    # check prompt
//    self.expect('>>')
//
//            return self.vals
//
//    def dump_binary(self,start, stop, length):
//            # http://stackoverflow.com/questions/14678132/python-hexadecimal
//    data = "b %08x %08x %08x\r\n"%(start, stop,length) #
//    print(data)
//    phantomLink.write(data)
//            # first read echoed command
//    data = self.read_bytes(28)
//            # read bytes
//    data = self.read_bytes(length*8)
//    ints = []
//            for i in range(0,len(data),4):
//            # ints = struct.unpack("<L",data[i:i+4])[0]
//            ints.append(bytes_to_int(data[i:i+4]))
//
//    self.vals = ints
//    # print(self.vals)
//    # no prompt expecetd after this dump
//
//    return self.vals



    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TcpClient(mIpAddress,mPort, new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            //in the arrayList we add the messages received from server
            //arrayList.add(values[0]);

            Log.d("ICT", "from tcp:" + values[0]);
            handleMessage(values[0]);
            //parseSpeed(values[0]);

            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            //mAdapter.notifyDataSetChanged();
        }
    }

    int pollCarState = 0;
    int currentRegister = 0x80;
    void pollCar() {

        // messages
        String[] messages = {"r 00000080\r",
                "r 00000081\r",
                "r 00000082\r",};

        int[] registers = {0x80,0x81,0x82};

        String message = messages[pollCarState];
        currentRegister = registers[pollCarState];
        pollCarState = (pollCarState+1) % messages.length; // round robin

        //sends the message to the server
        if (mTcpClient != null) {
            if(currentRegister == 0x82) {
                totalReadCount+=1;
            }
            mTcpClient.sendMessage(message);
        }

        //refresh the list
        //mAdapter.notifyDataSetChanged();
    }

    void handleMessage(String s) {
        ictRawLog.append(s); // save everything in a raw log

        for (String line : s.split("\n")) {
            RegisterValue reg = parseRead(line);
            if (reg != null) {
                if (reg.address == 0x80) { // motor 1 register
                    pwm1 = fi(bitSliceGet(reg.data, 31, 24), 0, 8, 0)/255.0 * 100.0; // percent
                    iThrottle1 = fi(bitSliceGet(reg.data, 23, 12), 1, 11, 0) / 2048.0 * 250.0; // amps
                    i1 = fi(bitSliceGet(reg.data, 11, 0), 1, 11, 0) / 2048.0 * 250.0; // amps
                }
                if (reg.address == 0x81) { // motor 2 register
                    pwm2 = fi(bitSliceGet(reg.data, 31, 24), 0, 8, 0)/255.0 * 100.0; // percent
                    iThrottle2 = fi(bitSliceGet(reg.data, 23, 12), 1, 11, 0) / 2048.0 * 250.0; // amps
                    i2 = fi(bitSliceGet(reg.data, 11, 0), 1, 11, 0) / 2048.0 * 250.0; // amps
                }
                if (reg.address == 0x82) { // speed register
                    goodReadCount += 1;
                    // 1/1008 = 1/500*60 sec/min/3.6(pulley ratio)*10*pi/rev/1056 MPH/(in/min)
                    double speed = (double) bitSliceGet(reg.data, 31, 16) / 1008.0;
                    mph1 = speed;
                    rpm1 = fi(bitSliceGet(reg.data, 31, 16), 1, 15, 0) / 500 * 60; // rpm
                    for (SpeedCallback callback : callbacks) {
                        callback.setSpeed(speed);
                    }
                    long tEnd = System.nanoTime();
                    long tRes = tEnd - tStart; // time in nanoseconds
                    readRate = goodReadCount / (tRes * 1e-9);
                }
                // log everything to analyze later
                ictLog.append(String.format(Locale.US, "%s, reg, %08x, %08x, %.1f, %.1f, %.1f", IctLog.timestamp(),
                        reg.address, reg.data, rpm1, pwm1, i1));
            }
        }
    }

    // http://stackoverflow.com/questions/541487/implements-runnable-vs-extends-thread?rq=1
    public class IctRunnable implements Runnable {
        public void run() {
            //todo: add way to stop thread cleanly
            while (mTcpClient != null) {
                // do stuff in a separate thread
                pollCar();
                //uiCallback.sendEmptyMessage(0);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }



    void startPollingCar() {
        new ConnectTask().execute("");
        tStart = System.nanoTime();

        Thread poller = new Thread(new IctRunnable());
        poller.start();
    }

    void disconnect() {
        Log.e("ICT", String.format("disconnecting... "));

        // disconnect
        if(mTcpClient != null) {
            Log.e("ICT", String.format("stopping client... "));
            mTcpClient.stopClient();
            Log.e("ICT", String.format("nulling client... "));
            mTcpClient = null;
        }
        //upload();
    }

    void upload(Context context) {
        Log.d("Ict.upload","about to upload");
        final String[] filenames = {logFilename, rawLogFilename};
        Log.d("Ict.upload",filenames[0]);
        Log.d("Ict.upload",filenames[1]);
        ictLog.upload(context, filenames);
    }

    private Handler uiCallback = new Handler () {
        public void handleUiMessage (Message msg) {
            // do stuff with UI
        }
    };

    void clearLogs() {
        Log.d("Ict.clearLogs","clearing logs");
        ictLog.clear();
        ictRawLog.clear();
    }

}
