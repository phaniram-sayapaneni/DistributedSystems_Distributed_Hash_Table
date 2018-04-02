package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SimpleDhtProvider extends ContentProvider {

    static String pred; // = "11108";
    static String succ ="";
    static String portnum =  SimpleDhtProvider.portnum;
    static boolean informnewpred = false;
    static String mynode_hashed;
    static String pred_hashed;
    static String query_portnum;


    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    public static Context context;

    static final int SERVER_PORT = 10000;
    static List<String> active_nodes = new ArrayList<String>();
    static boolean informnewneighbours = false;

    static boolean [] nodes_active = new boolean [5];
    static String [] nodes = new String[5];
    static HashMap<String, Boolean> hm = new HashMap<String, Boolean>();




    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

   // nodes[0] = "11108";
    //nodes[1]

    //static String portnum = portnum;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {



        // TODO Auto-generated method stub
        /// adding delete function
        Context context = getContext();
        Log.d("inside_delete", " deleting this file" + selection);
        File fin = new File(context.getFilesDir(), (selection + ".txt"));
        fin.delete();
        /////////////////////
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        Log.v("insert", values.toString());
        String key = (String) values.get("key");
        String value1 = (String) values.get("value");

        Log.d("input_insert", "key:" + key + "value:" + value1);
        /// for the grading script to run initially


        Log.d("initial_state", "pred:"+ pred + "succ:" + succ + "pred==null:" + (pred==null) + "succ.isempty" + succ.isEmpty())  ;
        if( succ.isEmpty() || pred ==null || succ == null  )
        {
            Log.d("initial_state", "inside the initial state");
            //String value1 = (String) values.get("value");
            String filename = key + ".txt";
            String string = value1 ;
            FileOutputStream outputStream;
            try {
                // outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                //OutputStream fo = new FileOutputStream(filename);
                if (context == null)
                    Log.e("context", "null");
                Log.e("path", context.getFilesDir().getAbsolutePath());
                File file = new File(context.getFilesDir(), filename);
                FileWriter fw =  new FileWriter(file);
                fw.write(string);
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        /// added to forward insert operation if necessary
        try {
            String haskey = genHash(key);
            String succ_2 = Integer.toString( Integer.parseInt(succ)/2);
            String succhash = genHash(succ_2);/// from succ
            String pred_2 = Integer.toString( Integer.parseInt(pred)/2);
            String predhash = genHash(pred_2);
            String portnum_2 = Integer.toString( Integer.parseInt(portnum)/2);
            String myhash = genHash(portnum_2);

            String value = (String) values.get("value");
            String filename = key + ".txt";
            String string = value;



            if(haskey.compareTo(myhash)<=0 && haskey.compareTo(predhash)>0  )
            {
                ///write into filesystem
                //String value = (String) values.get("value");
                //String filename = key + ".txt";
                //String string = value;
                FileOutputStream outputStream;
                try {
                    // outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    //OutputStream fo = new FileOutputStream(filename);
                    if (context == null)
                        Log.e("context", "null");
                    Log.e("path", context.getFilesDir().getAbsolutePath());
                    File file = new File(context.getFilesDir(), filename);
                    FileWriter fw =  new FileWriter(file);
                    fw.write(string);
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return uri;
                ////write ends here

            }

            ////adding else if

            else if(succhash.compareTo(myhash)<0 &&  haskey.compareTo(myhash)>0)
             {
                 String fornewSucc =  "blindinsert" + " " + "key" + " "+ key + " "+ string;
                ////// Log.d("insert_forwarding_blindly", "yo, im sending:" + key + "value:" + value + "to :" + succ);
                 new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fornewSucc, succ);

             }

            // adding now: for the first node to check if the incoming forward packet is for itself:

            else if(predhash.compareTo(myhash)>0 && myhash.compareTo(haskey)>0)
            {
                try {
                    if (context == null)
                        Log.e("context", "null");
                    Log.e("path", context.getFilesDir().getAbsolutePath());
                    File file = new File(context.getFilesDir(), filename);
                    FileWriter fw =  new FileWriter(file);
                    fw.write(string);
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            else {

                String fornewSucc =  "insert" + " " + "key" + " "+ key + " "+ string;
                Log.d("insert_forwarding", "yo, im sending:" + key + "value:" + value + "to :" + succ);
                new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fornewSucc, succ);

            }



        }
        catch ( Exception e)
        {
            Log.d("hashing", "hashing problem");
        }

        return uri;

        //return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        try
        {String delete =  genHash("hi");
            // mypub("genHash" + delete);
            Log.d("content", "hi" + delete);
        }
        catch(Exception e) {
            Log.d("e", "exception with genhash");
            //mypub("exceptin here");
        }

        ///
        nodes[0] ="11108";  nodes[1] ="11124";  nodes[2] ="11120"; nodes[3] ="11112"; nodes[4] ="11116";
        nodes_active[0] = true;  nodes_active[1] = false; nodes_active[2] = false; nodes_active[3] = false; nodes_active[4] = false;

        //////

        context = getContext();
        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        portnum = myPort;
        active_nodes.add(portnum);
        query_portnum = portnum;



        try {
            Log.d("test", "so the problem is with server task1");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.d("test", "so the problem is with server task2");
            new SimpleDhtProvider.ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            //new SimpleDhtProvider.ServerTask1().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            //new SimpleDhtProvider.ServerTask_insert().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            Log.d("test", "so the problem is with server task3");
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");

        }


        ////////adding client task here
        String pingmain = "join" + " " +  portnum;
        if(!portnum.equals("11108"))
        { new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, pingmain, "11108");
        }


       try{Thread.sleep(100); }
       catch (Exception ee) { }
        {
            if (informnewneighbours && !portnum.equals("11108")) {
                String fornewpred = "yourbrandnewsucc"+ " " + portnum;
                new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fornewpred, pred);

                String fornewSucc = "yourbrandnewpred" + " " + portnum;
                new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fornewSucc, succ);
                informnewpred = false;
                informnewneighbours = false;

                Log.d("final", "my final data:" + "succc:" + succ + "pred:" + pred);
                try {
                    mynode_hashed = genHash(Integer.toString( Integer.parseInt(portnum)/2));
                    pred_hashed = genHash(Integer.toString( Integer.parseInt(pred)/2));
                }
                catch(Exception e) { Log.d("hashing", "hashing problem"); }
            }
        }

        return false;
    }

    //public Cursor query(Uri ur)
    public String querystar(String input)
    {
        //String forstarq  = "star" + " "+ "query" + " "+ "askingport" + " "+ portnum + " " +"*";
        String [] msgin1 = input.split(" ");
        if(msgin1.length ==5 && msgin1[0].equals("star"))
         {
             String selections = "";
             try{
                 Context context =  getContext();
                 File fin = new File(context.getFilesDir().getPath());

                 File [] listoffiles = fin.listFiles();
                 for(int i=0; i< listoffiles.length; i++) {
                     //listoffiles[i].getName()
                     String eachfile= listoffiles[i].getName();
                     FileReader fr = new FileReader(fin + "/" + eachfile + ".txt");
                     Log.e("file", "inside file: path: " + fin + "/" + eachfile);
                     BufferedReader br = new BufferedReader(fr);
                     String valuenow = br.readLine();
                     br.close();
                     selections = valuenow + " ";
                 }
                 selections = selections.trim();
                 //String fornewSucc =  "insert" + " " + "key" + " "+ haskey + " "+ string;

             }
             catch (Exception e) {
                 Log.e("readerror", "unable to read the file");
             }

             if(succ.equals(msgin1[2])) return selections;
             else {
                 try {
                     return selections ;
                 }
                 catch(Exception ed) { Log.d("client1", "hi" );}
              }
         }
        return "";
    }




    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        String myhash = "";
        String predhash = "";
        try {
            myhash = genHash(Integer.toString(Integer.parseInt(portnum) / 2));
            predhash = genHash(Integer.toString(Integer.parseInt(pred) / 2));

        } catch (Exception eeec) {
            Log.d("hashproblem", "some is not right with hash");
        }

        //////// redefining query
        if (succ.isEmpty() || pred == null || succ == null) {   /////  non special charactersa
            if (!selection.equals("*") && !selection.equals("@")) {
                Log.d("non_special", " regular query");
                String valuenow = "";
                try {
                    Context context = getContext();
                    File fin = new File(context.getFilesDir().getPath());
                    FileReader fr = new FileReader(fin + "/" + selection + ".txt");
                    Log.e("file", "inside file: path: " + fin + "/" + selection);
                    BufferedReader br = new BufferedReader(fr);
                    valuenow = br.readLine();
                    br.close();
                } catch (Exception e) {
                    Log.e("readerror11", "unable to read the file");
                }
                String[] s = new String[2];
                s[0] = "key";
                s[1] = "value";
                MatrixCursor mc = new MatrixCursor(s);
                String[] rw = new String[2];
                rw[0] = selection;
                Log.d("query_regular11", "so it is querying selesction" + selection);
                rw[1] = valuenow;
                Log.d("query_regular11", "so it is querying value" + valuenow);
                mc.addRow(rw);
                return mc;

            }
            ///////////////
            else {

                String test_s = "";
                String test_v = "";
                Log.d("inside@", "reglar now what");
                String valuenow = "";
                String[] s = new String[2];
                s[0] = "key";
                s[1] = "value";
                MatrixCursor mc = new MatrixCursor(s);
                Log.d("inside@", " regular now what1");
                try {
                    Context context = getContext();
                    Log.d("inside@", " regular now what2");
                    File fin = new File(context.getFilesDir().getPath());
                    Log.d("inside@", " regular now what3");
                    File[] listoffiles = fin.listFiles();
                    Log.d("inside@", " regular now what3");
                    for (int i = 0; i < listoffiles.length; i++) {
                        //listoffiles[i].getName()
                        String eachfile = listoffiles[i].getName();
                        Log.d("inside@", " regular now what4");
                        FileReader fr = new FileReader(fin + "/" + eachfile); /// removed + .txt here
                        Log.e("file", "inside file: path: " + fin + "/" + eachfile);
                        BufferedReader br = new BufferedReader(fr);
                        valuenow = br.readLine();
                        br.close();
                        Log.d("inside@", "now what5");
                        String[] rw = new String[2];
                        eachfile = eachfile.substring(0, eachfile.lastIndexOf('.'));
                        rw[0] = eachfile;
                        test_s = test_s + eachfile + " ";
                        Log.d("@_each", "regular test_s" + test_s);
                        rw[1] = valuenow;
                        test_v = test_v + valuenow + " ";
                        Log.d("@_each", "regular test_v" + test_v);
                        mc.addRow(rw);

                    }
                    Log.d("@", "all selections:" + test_s + "all values:" + test_v);
                    return mc;
                } catch (Exception e) {
                    Log.e("readerror", "unable to read the file");
                }

            }


        }
        ///////////////regular stuff ends

        else {
            if (selection.equals("@")) {

                String test_s = "";
                String test_v = "";
                Log.d("inside@", "reglar now what");
                String valuenow = "";
                String[] s = new String[2];
                s[0] = "key";
                s[1] = "value";
                MatrixCursor mc = new MatrixCursor(s);
                Log.d("inside@", " regular now what1");
                try {
                    Context context = getContext();
                    Log.d("inside@", " regular now what2");
                    File fin = new File(context.getFilesDir().getPath());
                    Log.d("inside@", " regular now what3");
                    File[] listoffiles = fin.listFiles();
                    Log.d("inside@", " regular now what3");
                    for (int i = 0; i < listoffiles.length; i++) {
                        //listoffiles[i].getName()
                        String eachfile = listoffiles[i].getName();
                        Log.d("inside@", " regular now what4");
                        FileReader fr = new FileReader(fin + "/" + eachfile); /// removed + .txt here
                        Log.e("file", "inside file: path: " + fin + "/" + eachfile);
                        BufferedReader br = new BufferedReader(fr);
                        valuenow = br.readLine();
                        br.close();
                        Log.d("inside@", "now what5");
                        String[] rw = new String[2];
                        eachfile = eachfile.substring(0, eachfile.lastIndexOf('.'));
                        rw[0] = eachfile;
                        test_s = test_s + eachfile + " ";
                        Log.d("@_each", "regular test_s" + test_s);
                        rw[1] = valuenow;
                        test_v = test_v + valuenow + " ";
                        Log.d("@_each", "regular test_v" + test_v);
                        mc.addRow(rw);

                    }
                    Log.d("@", "all selections:" + test_s + "all values:" + test_v);
                    return mc;
                } catch (Exception e) {
                    Log.e("readerror", "unable to read the file");
                }

            }

            ////// @ selection ends here

            else if (!selection.equals("*")) {
                String ask_others = "query" + " " + selection;
                String[] s = new String[5];

                String[] sc = new String[2];
                sc[0] = "key";
                sc[1] = "value";
                MatrixCursor mc = new MatrixCursor(sc);

                try {
                    s[0] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11108").get();
                    Log.d("query" ,"info recived from server is: s[0]" + s[0]);
                    if ((!s[0].isEmpty()) && (!s[0].equals("")) && s[0].length() > 2)
                    {
                        Log.d("nonstar query", "selection:" + selection +  "value" +   s[0] );
                        String[] rw = new String[2];
                        rw[0] = selection;
                        rw[1] = s[0];
                        mc.addRow(rw);
                        return mc;
                    }

                } catch (Exception ex1) {
                    Log.d("ex1", "exception");
                }
///////////////////////
              //////////////////////
                try {
                    s[1] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11112").get();
                    Log.d("query" ,"info recived from server is: s[1]" + s[1]);
                    if ((!s[1].isEmpty()) && (!s[1].equals("")) && s[1].length() > 2)
                    {
                        Log.d("nonstar query", "selection:" + selection +  "value" +   s[1] );
                        String[] rw = new String[2];
                        rw[0] = selection;
                        rw[1] = s[1];
                        mc.addRow(rw);
                        return mc;
                    }
                } catch (Exception ex1) {
                    Log.d("ex2", "exception");
                }
                try {
                    s[2] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11116").get();
                    Log.d("query" ,"info recived from server is: s[2]" + s[2]);
                    if ((!s[2].isEmpty()) && (!s[2].equals("")) && s[2].length() > 2)
                    {
                        Log.d("nonstar query", "selection:" + selection +  "value" +   s[2] );
                        String[] rw = new String[2];
                        rw[0] = selection;
                        rw[1] = s[2];
                        mc.addRow(rw);
                        return mc;
                    }
                } catch (Exception ex1) {
                    Log.d("ex3", "exception");
                }
                try {
                    s[3] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11120").get();
                    Log.d("query" ,"info recived from server is: s[3]" + s[3]);
                    if ((!s[3].isEmpty()) && (!s[3].equals("")) && s[3].length() > 2)
                    {
                        Log.d("nonstar query", "selection:" + selection +  "value" +   s[3] );
                        String[] rw = new String[2];
                        rw[0] = selection;
                        rw[1] = s[3];
                        mc.addRow(rw);
                        return mc;
                    }
                } catch (Exception ex1) {
                    Log.d("ex4", "exception");
                }
                try {
                    s[4] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11124").get();
                    Log.d("query" ,"info recived from server is: s[4]" + s[4]);
                    if ((!s[4].isEmpty()) && (!s[4].equals("")) && s[4].length() > 2)
                    {
                        Log.d("nonstar query", "selection:" + selection +  "value" +   s[0] );
                        String[] rw = new String[2];
                        rw[0] = selection;
                        rw[1] = s[4];
                        mc.addRow(rw);
                        return mc;
                    }
                } catch (Exception ex1) {
                    Log.d("ex5", "exception");
                }

                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11112");
                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11116");
                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11120");
                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11124");


               // try
                //{Thread.sleep(20);}
                //catch(Exception ex) {}
                //
                //String[] sc = new String[2];
                //sc[0] = "key";
                //sc[1] = "value";
                //MatrixCursor mc = new MatrixCursor(sc);
                for (int i = 0; i < 5; i++) {
                    if ((!s[i].isEmpty()) && (!s[i].equals("")) && s[i].length() > 2) {
                        Log.d("nonstar query", "selection:" + selection +  "value" +   s[i] );
                        String[] rw = new String[2];
                        rw[0] = selection;
                        rw[1] = s[i];
                        mc.addRow(rw);
                        return mc;
                    }
                    return  mc;
                }
            } else if (selection.equals("*")) {
                String ask_others = "starquery";

                String[] s = new String[5];
                try {
                    s[0] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11108").get();
                    Log.d("starquery", "here is s[0]" +  s[0]);

                } catch (Exception ex1) {
                    Log.d("ex1", "exception");
                }
                try {
                    s[1] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11112").get();
                    Log.d("starquery", "here is s[1]" +  s[1]);
                } catch (Exception ex1) {
                    Log.d("ex2", "exception");
                }
                try {
                    s[2] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11116").get();
                    Log.d("starquery", "here is s[2]" +  s[2]);
                } catch (Exception ex1) {
                    Log.d("ex3", "exception");
                }
                try {
                    s[3] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11120").get();
                    Log.d("starquery", "here is s[3]" +  s[3]);
                } catch (Exception ex1) {
                    Log.d("ex4", "exception");
                }
                try {
                    s[4] = new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11124").get();
                    Log.d("starquery", "here is s[4]" +  s[4]);
                } catch (Exception ex1) {
                    Log.d("ex5", "exception");
                }


                String[] sc = new String[2];
                sc[0] = "key";
                sc[1] = "value";
                MatrixCursor mc = new MatrixCursor(sc);
                for (int i = 0; i < 5; i++) {
                    if ((!s[i].isEmpty()) && (!s[i].equals("")) && s[i].length() > 2) {
                        String[] allmsgs = s[i].split(" ");
                        Log.d("starquery", "here is the msg from s[i]" +  s[i]);
                        for (int j = 0; j < allmsgs.length; j += 2) {


                            String[] rw = new String[2];
                            rw[0] = allmsgs[j];//selctions
                            rw[1] = allmsgs[j + 1];
                            Log.d("starquery", "here is key:" + allmsgs[j] + "here is value:" + allmsgs[j+1]);
                            mc.addRow(rw);
                            //j = j+1;
                        }
                        //return mc;
                    }
                }
                return mc;

                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11108");
                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11112");
                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11116");
                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11120");
                //new SimpleDhtProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ask_others, "11124");


            }


        }

        return null;
    }



    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }


    // my own method
    //@Override


    public class seqcomparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {

           try{
            String s11 = genHash(Integer.toString( Integer.parseInt(s1)/2));
            String s22 = genHash(Integer.toString( Integer.parseInt(s2)/2));
               return  s11.compareTo(s22);
           }
           catch(Exception exc) {Log.d("genhash", "gen hash here too");}
            return  0;
        }
    }

          ///3rd parameter changed from void to string

    private class ServerTask extends AsyncTask<ServerSocket, String, String> {

        @Override
        protected String doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int sequencenum = 0;
            Log.d("test", "Inside the server task");
            Log.d(TAG, "Server testing");
            try {
                //publishProgress("test1");
                while (1 == 1) {
                    Socket s = serverSocket.accept();
                    String msg = "";
                    try{
                        DataInputStream dis = new DataInputStream(s.getInputStream());
                        msg = dis.readUTF();}
                    catch (Exception e) {
                        Log.d("dis",  "input data empty");

                    }

                    Log.d("wtf",  "what the fuck server?1" + "msg" + msg);
                    //if(msg.length()<2 || msg.isEmpty()) return null;
                    ///////adding to know if someone is pingng me
                    String [] msgr = msg.split(" ");
                    Log.d("wtf",  "what the fuck server?2");
                    Log.d("input_server" , "from server input message" + msg);
                    if(msgr.length == 2 && msgr[0].equals("query"))
                    {

                        try{
                            Context context =  getContext();
                            File fin = new File(context.getFilesDir().getPath());
                            FileReader fr = new FileReader(fin + "/" + msgr[1]+ ".txt");
                            Log.e("file", "inside file: path: " + fin + "/" + msgr[1]);
                            BufferedReader br = new BufferedReader(fr);
                            String returnvaluenow1 = br.readLine();
                            br.close();
                            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                            dos.writeUTF(returnvaluenow1);
                            dos.flush();
                            Log.d("input_server" , "message sent in response to query " + msgr[1] + "ans:retuern value" + returnvaluenow1 );
                            //s.close();/// closing socket here
                            //return returnvaluenow1;
                        }
                        catch (Exception e) {
                            Log.e("readerror", "unable to read the file");
                        }
                    }

                    if(msgr.length == 1 && msgr[0].equals("starquery"))
                    {
                        String returnstar = "";

                        try{
                            Context context =  getContext();
                            Log.d("inside@", " regular now what2");
                            File fin = new File(context.getFilesDir().getPath());
                            Log.d("inside@", " regular now what3");
                            File [] listoffiles = fin.listFiles();
                            Log.d("inside@", " regular now what3");
                            for(int i=0; i< listoffiles.length; i++)
                            {
                                //listoffiles[i].getName()
                                String eachfile= listoffiles[i].getName();
                                Log.d("inside@", " regular now what4");
                                FileReader fr = new FileReader(fin + "/" + eachfile); /// removed + .txt here
                                Log.e("file", "inside file: path: " + fin + "/" + eachfile);
                                BufferedReader br = new BufferedReader(fr);
                                String  valuenow = br.readLine();
                                br.close();
                                Log.d("inside@", "now what5");
                                String []rw = new String[2];
                                eachfile = eachfile.substring(0, eachfile.lastIndexOf('.'));
                                returnstar +=  eachfile + " " + valuenow + " ";
                                //rw[0] = eachfile; test_s = test_s + eachfile +" "; Log.d("@_each",  "regular test_s"+  test_s);
                                //rw[1] = valuenow;  test_v = test_v + valuenow +" "; Log.d("@_each",  "regular test_v"+  test_v);
                                //mc.addRow(rw);
                                Log.d("resturnall", "returnallstring is here" + returnstar);

                            }
                            //Log.d("@",  "all selections:" + test_s + "all values:" +  test_v);
                            //return mc;
                            returnstar = returnstar.trim();
                            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                            dos.writeUTF(returnstar);
                            dos.flush();
                            //return returnstar;
                        }
                        catch (Exception e) {
                            Log.e("readerror", "unable to read the file");
                        }
                    }

                    /// query ends here


                    if(msgr.length == 4 && msgr[0].equals("insert"))
                    {
                      /// inseerting task
                        Log.d("inserts_recieved" ,  "message is" + msg);
                        ContentValues cv = new ContentValues();
                        cv.put("key", (msgr[2]));
                        String to_be_stored = msgr[3]; //rcvmsg[0]
                        cv.put("value", to_be_stored);
                        //GroupMessengerProvider  gmprovider  = new GroupMessengerProvider();
                         insert(mUri, cv);
                       ////
                    }

                    /// adding for blind insert

                    else if(msgr.length == 4 && msgr[0].equals("blindinsert"))
                    {
                        /// inseerting task
                        try {
                            // outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                            //OutputStream fo = new FileOutputStream(filename);
                            if (context == null)
                                Log.e("context", "null");
                            Log.e("path", context.getFilesDir().getAbsolutePath());
                            File file = new File(context.getFilesDir(), msgr[2] + ".txt");
                            FileWriter fw =  new FileWriter(file);
                            fw.write(msgr[3]);
                            fw.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d("portnum", "my portnum:" + portnum);
                    if(portnum.equals("11108")) {
                        /////////////////
                        if (msgr.length == 2 && msgr[0].equals("join"))  {
                            //// if only AvD0 is present
                            String send_msg = "";
                            if (succ.isEmpty() || succ.equals("11108"))
                            {
                                Log.d("where","i am inside ip loopp");
                                succ = msgr[1]; pred = msgr[1];
                                send_msg = "your" + " " + "newsuccis" + " " + portnum + " " + "newpredis" + " " + portnum;
                                Log.d("join" , "so node1 joined" + "succ:" + succ + "pred:" + pred);
                               // allnodes.add(msgr[1]);
                                //nodes_active.add(msgr[1]);
                                active_nodes.add(msgr[1]);


                            } else {

                                Log.d("where","i am inside else loopp");
                                String asking_node = msgr[1];
                                try {
                                    //String hashed = genHash(asking_node);
                                    active_nodes.add(asking_node);
                                    Collections.sort(active_nodes, new seqcomparator());
                                    for(int i =0; i<active_nodes.size(); i++) {Log.d("after_sort", "here is sorted list" + active_nodes.get(i));}
                                    int i =0;
                                    while(!active_nodes.get(i).equals(asking_node))
                                     {i++;}
                                    if(i >0 && i< active_nodes.size()-1)
                                    { send_msg = "your" + " " + "newsuccis" + " " + active_nodes.get(i+1) + " " + "newpredis" + " " + active_nodes.get(i-1);
                                         }

                                    else if(i==0)
                                    {send_msg = "your" + " " + "newsuccis" + " " + active_nodes.get(1) + " " + "newpredis" + " " + active_nodes.get(active_nodes.size()-1);}

                                    else if(i==(active_nodes.size()-1))
                                    {send_msg = "your" + " " + "newsuccis" + " " + active_nodes.get(0) + " " + "newpredis" + " " + active_nodes.get(active_nodes.size()-2);}

                                } catch (Exception e) {
                                    Log.d("gen", "problem with hashing");
                                }

                            }
                            //// data sent to new joing node about its

                            DataOutputStream doss = new DataOutputStream(s.getOutputStream());
                            doss.writeUTF(send_msg);
                            doss.flush();

                        }
                        ////////////
                    }
                    ///// for brandnew node  join request
                    if(msgr.length ==2)
                    { if(msgr[0].equals("yourbrandnewsucc"))
                    {succ = msgr[1];

                        Log.d("brandnew", "my brandnew info" + "my portnum:" + portnum + "brandnewsucc:" + succ  );
                    }

                    else if(msgr[0].equals("yourbrandnewpred"))
                    {pred = msgr[1];
                        Log.d("brandnew", "my brandnew info" + "my portnum:" + portnum + "brandnewpred" + pred);
                    }
                    }
                    s.close();
                }


            } catch (IOException e) {

                Log.d("wtf", "server got wrong");
                publishProgress("exception");
                e.printStackTrace();
            }


            return ""; // instead of null;
        }
    }

    //client task starts here
    private class ClientTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... msgs) {
            String msgin = "";
            try {
                //String remotePort = REMOTE_PORT0;
                //if (msgs[1].equals(REMOTE_PORT0))


                ///removed return null
                //if (portnum.equals("11108")) return null;

                Log.d("test", "Inside the client task");

                String remotePort = msgs[1];

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                Log.d("test", "Error at new socket");

                String msgToSend = msgs[0];
                String [] msgrcv ;
                msgrcv = msgToSend.split(" ");

                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                Log.d("MyApp", "I am here:" + msgToSend + " portstr" + remotePort);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(msgToSend);
                dos.flush();
                Log.d("MyApp", "output complete till here");
                Log.d("MyApp", "now i need to get input from other nodes");


                try {
                    //// proposal recieving from all other avds
                    DataInputStream datainputstream = new DataInputStream(socket.getInputStream());
                     msgin = datainputstream.readUTF();
                    String [] rcvmsg_reply = msgin.split(" ");
                    Log.d("client_input", msgin);

                    if(rcvmsg_reply.length == 5)
                    {
                        succ = rcvmsg_reply[2]; pred = rcvmsg_reply[4];
                        informnewneighbours = true;


                        Log.d("someone_joined" , "some node joined" + "succ:" + succ + "pred:" + pred);

                    }

                    else if(msgrcv[0].equals("query") || msgrcv[0].equals("starquery"))
                    {
                        return msgin;
                    }




                }
                catch (Exception e)
                {// something_crashed = true;
                    Log.d("deadnodes", "this node is not present" + remotePort);


                }
                //// / if the server of 11108 sends the info about its new pred and asks it to set succ on avd0
                //DataInputStream datainputstream = new DataInputStream(socket.getInputStream());
                //String msgin = datainputstream.readUTF();
                //Log.d("Ack", "Just Acknowledgement" + msgin);
                socket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return "";
        }
    }
    ///cleant task ends here

    private class ClientTask1 extends AsyncTask<String, Void, String> {
        @Override
        public String doInBackground(String... msgs) {
            try {
                //String remotePort = REMOTE_PORT0;
                //if (msgs[1].equals(REMOTE_PORT0))

                if (portnum.equals("11108")) return null;

                Log.d("test", "Inside the client task");

                String remotePort = msgs[1];

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                Log.d("test", "Error at new socket");

                String msgToSend = msgs[0];
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                Log.d("MyApp", "I am here:" + msgToSend + " portstr" + remotePort);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(msgToSend);
                dos.flush();
                Log.d("MyApp", "output complete till here");
                Log.d("MyApp", "now i need to get input from other nodes");

                //try{
                //Thread.sleep(4000);}
                //catch (Exception e) { Log.d("sleep" , "problem with sleeping");}
                //// / if the server of 11108 sends the info about its new pred and asks it to set succ on avd0
                DataInputStream datainputstream = new DataInputStream(socket.getInputStream());
                String msgin = datainputstream.readUTF();
                Log.d("Ack", "Just Acknowledgement" + msgin);
                socket.close();
                return msgin;

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return "";
        }
    }
//// new server task


    private class ServerTask1 extends AsyncTask<ServerSocket, String, String> {

        @Override
        protected String doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int sequencenum = 0;
            Log.d("test", "Inside the server task");
            Log.d(TAG, "Server testing");
            try {
                //publishProgress("test1");
                while (1 == 1) {
                    Socket s = serverSocket.accept();
                    String msg = "";
                    try{
                        DataInputStream dis = new DataInputStream(s.getInputStream());
                        msg = dis.readUTF();}
                    catch (Exception e) {
                        Log.d("dis",  "input data empty");

                    }

                    //if(msg.length()<2 || msg.isEmpty()) return null;
                    ///////adding to know if someone is pingng me
                    String [] msgr = msg.split(" ");
                    Log.d("input_server1" , "from server input message" + msg);

                    //String forstarq  = "star" + " "+ "query" + " "+ "askingport" + " "+ portnum + " " +"*";

                    /// check for insert command
                    if(msgr.length == 5 && msgr[0].equals("star"))
                    {
                        /// checking for a loop
                        if(succ.equals(msgr[2])) return "";

                        else {

                            String ans =  querystar(msg);
                           // return ans;
                           // now send the data to the client
                            DataOutputStream doss = new DataOutputStream(s.getOutputStream());
                            doss.writeUTF(ans);
                            doss.flush();


                        }


                        ////
                    }
                    s.close();
                }


            } catch (IOException e) {
                publishProgress("exception");
                e.printStackTrace();
            }


            return "";
        }
    }



    private class ServerTask_insert extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int sequencenum = 0;
            Log.d("test", "Inside the server task");
            Log.d("insert_server", "Server testing");
            try {
                //publishProgress("test1");
                while (1 == 1) {
                    Socket s = serverSocket.accept();
                    String msg = "";
                    try {
                        DataInputStream dis = new DataInputStream(s.getInputStream());
                        msg = dis.readUTF();
                    } catch (Exception e) {
                        Log.d("dis", "input data empty");

                    }


                    String[] msgr = msg.split(" ");
                    Log.d("input_server_insert", "from server input message" + msg);


                    if (msgr.length == 4 && msgr[0].equals("insert")) {
                        /// inseerting task
                        ContentValues cv = new ContentValues();
                        cv.put("key", (msgr[2]));
                        String to_be_stored = msgr[3]; //rcvmsg[0]
                        cv.put("value", to_be_stored);
                        //GroupMessengerProvider  gmprovider  = new GroupMessengerProvider();
                        insert(mUri, cv);
                        ////
                    }
                }
            } catch (Exception eee) {
            }
        return null;}
    }



}