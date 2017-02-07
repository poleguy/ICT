package com.phantompowerracing.ict;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
    public double readRate = 0;
    private long tStart;

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
            } catch (NumberFormatException e) {
                address = null;
            } catch (ArrayIndexOutOfBoundsException e) {
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
        } catch (NumberFormatException e) {
            //print(e)
            //throw e;
            //ignore!
            data = null;
        } catch (ArrayIndexOutOfBoundsException e) {
            data = null;
        }
        return data;
    }

    //# returns 32 bit int for address and data
    Integer[] parseRead(String rawStr) {
        //#print "parse"
        //#expected format:
        //#addr:
        //00000010 = 0100001e
        Integer data = null;
        Integer address = null;

        String[] list = rawStr.split(" ");
        Integer[] ret = null;
        if(list[0].equals("addr:")) {
            // validate address:
            try {
                address = Integer.parseInt(list[1], 16); //#raw 32 bit value
                //#print("raw: " + str(raw))
                //#data = fi(raw, 1, 7, 0)#change to 11
                //#print("data: " + str(data))
            } catch (NumberFormatException e) {
                address = null;
            } catch (ArrayIndexOutOfBoundsException e) {
                address = null;
            }

            //#print(list)
            try {
                data = Integer.parseInt(list[3], 16); //#raw 32 bit value
                //#print("raw: " + str(raw))
                //#data = fi(raw, 1, 7, 0)#change to 11
                //#print("data: " + str(data))
            } catch (NumberFormatException e) {
                //print(e)
                //throw e;
                //ignore!
                data = null;
            } catch (ArrayIndexOutOfBoundsException e) {
                data = null;
            }
            if ((address == null) ||
                    (data == null)) {
                corruptReadCount += 1;
                return null;
            }
            ret = new Integer[]{address,data};
        }
        return ret;
    }

    //# returns list of 32 bit ints
    List<Integer> parseList(String rawStr, int length) {
        //#print "parse"
        String[] list = rawStr.split("\n");
        //# print(list)
        List<Integer> data = new ArrayList<Integer>();
        try {
            //data = [];
            for (int n = 0; n < length; n++) {
                data.add(Integer.parseInt(list[n], 16)); //#raw 32 bit value
                //#print("raw: " + str(raw))
                //#data = fi(raw,1,7,0) # change to 11
                //#print("data: " + str(data))
            }
        } catch (NumberFormatException e) {
            throw e;
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
    double fi(int v,int s,int i,int f) { //: # value, signed, int bits, fractional bits
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


    int bitSliceGet(int v,int lidx,int ridx) {
        // #print("idx: %d %d"%(lidx,ridx))
        int mask = (1 << (lidx + 1 - ridx)) - 1;
        // # print("mask: %08x"%mask)
        int shifted = v >> ridx;
        int slice = shifted & mask;
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
            if(currentRegister == 0x81) {
                totalReadCount+=1;
                long tEnd = System.nanoTime();
                long tRes = tEnd - tStart; // time in nanoseconds
                readRate = totalReadCount/(tRes*1e-9);
            }
            mTcpClient.sendMessage(message);
        }

        //refresh the list
        //mAdapter.notifyDataSetChanged();
    }

    void handleMessage(String s) {
        for(SpeedCallback callback : callbacks) {
            //if(currentRegister == 0x81) { // this is speed
                //Integer reg = parse(s,currentRegister);
                Integer[] reg = parseRead(s);
                if (reg != null) {
                    if(reg[0] == 0x81) { // speed register
                        goodReadCount+=1;
                        int speed = bitSliceGet(reg[1], 15, 0);
                        callback.setSpeed(speed);
                    }
                }
            //}
        }
    }

    Thread timer = new Thread() {
        public void run () {
            for (;;) {
                // do stuff in a separate thread
                pollCar();
                uiCallback.sendEmptyMessage(0);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    void startPollingCar() {
        new ConnectTask().execute("");
        tStart = System.nanoTime();
        timer.start();
    }

    void disconnect() {
        // disconnect
        mTcpClient.stopClient();
        mTcpClient = null;
    }

    private Handler uiCallback = new Handler () {
        public void handleUiMessage (Message msg) {
            // do stuff with UI
        }
    };


}
