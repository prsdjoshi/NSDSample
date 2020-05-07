package com.commodity.nsdsample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.adroitandroid.near.connect.NearConnect;
import com.adroitandroid.near.model.Host;
import com.commodity.nsdsample.databinding.ActivityConnectBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

public class ConnectActivity extends AppCompatActivity {

    private ActivityConnectBinding binding;
    public static final String BUNDLE_PARTICIPANT = "bundle_participant";
    private Host mParticipant;
    private NearConnect mNearConnect;
    private static final String STATUS_SEND = "status:send";
    private static final String STATUS_RECEIVED = "status:received";
    private static final String STATUS_EXIT = "status:exit";
    private final int RESULT_LOAD_IMAGE = 1;
    private final int FILE_SELECT_CODE = 2;
    private FilUtils fileutils;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect);
        mParticipant = getIntent().getParcelableExtra(BUNDLE_PARTICIPANT);
        setTitle("Connected with " + mParticipant.getName());
        fileutils= new FilUtils();
        progressDialog = new ProgressDialog(ConnectActivity.this);
        progressDialog.setMessage("Receiving...");
        binding.devicename.setText(mParticipant.getName());
        binding.deviceipaddr.setText(mParticipant.getHostAddress());
        initConnect();
        binding.btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // new genFile();

                new AlertDialog.Builder(ConnectActivity.this)
                        .setMessage("Which content you want to send...Choose file or photo?")
                        .setPositiveButton("Photo", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(i, RESULT_LOAD_IMAGE);

                            }
                        })
                        .setNegativeButton("File", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                try {
//                                    InetAddress inetAddress = InetAddress.getByName(mParticipant.getHostAddress());
//                                    Device device = new Device(
//                                            mParticipant.getName(),
//                                            "",
//                                            inetAddress,
//                                            );
//                                    Intent startTransfer = new Intent(ConnectActivity.this, ExplorerActivity.class);
//                                    startTransfer.putExtra("device_info", device);
//                                    startService(startTransfer);
//                                } catch (UnknownHostException e) {
//                                    e.printStackTrace();
//                                }

                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("*/*");
                                intent.addCategory(Intent.CATEGORY_OPENABLE);

                                try {
                                    startActivityForResult(
                                            Intent.createChooser(intent, "Select a File to Upload"),
                                            FILE_SELECT_CODE);
                                } catch (android.content.ActivityNotFoundException ex) {
                                    // Potentially direct the user to the Market with a Dialog
                                    Toast.makeText(ConnectActivity.this, "Please install a File Manager.",
                                            Toast.LENGTH_SHORT).show();
                                }

                            }
                        }).create().show();

//                byte[] bytes = getBytefromimage();
//                Log.d("File bytes: ", String.valueOf(bytes));
//                mNearConnect.send(bytes,mParticipant);

//                File file = new File("/storage/emulated/0/cupid/recode.log");
//                if(file.exists())
//                {
//                    Toast.makeText(ConnectActivity.this, "File exist", Toast.LENGTH_SHORT).show();
//                    byte[] bytes = getFilebyte();
//                    Log.d("File bytes: ",String.valueOf(bytes));
//                    mNearConnect.send(bytes,mParticipant);
//                }else {
//                    Toast.makeText(ConnectActivity.this, "File not exist", Toast.LENGTH_SHORT).show();
//                }

            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_LOAD_IMAGE:
                if (resultCode == RESULT_OK && null != data) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    cursor.close();
                    File imagefile = new File(picturePath);
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(imagefile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    Bitmap bm = BitmapFactory.decodeStream(fis);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] bytes = baos.toByteArray();
                    Log.d("File bytes: ", String.valueOf(bytes));
                    mNearConnect.send(bytes, mParticipant);
                }
            break;
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d("TAG", "File Uri: " + uri.toString());
                    // Get the path

                    String filepath=fileutils.getPath(getApplicationContext(),uri);
                    Log.d("TAG", "File Path: " + filepath);
                    byte[] bytes = getFilebyte(filepath);
                    Log.d("File bytes: ",String.valueOf(bytes));
                    mNearConnect.send(bytes,mParticipant);

                }
                break;
            default:
                break;
        }
    }


    public byte[] getFilebyte(String filepath)
    {
        File file = new File(filepath);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            try {
                for (int readNum; (readNum = fis.read(buf)) != -1;) {
                    bos.write(buf, 0, readNum); //no doubt here is 0
                    //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                    System.out.println("read " + readNum + " bytes,");
                }
            } catch (IOException ex) {
                // Logger.getLogger(genJpeg.class.getName()).log(Level.SEVERE, null, ex);
            }
            byte[] bytes = bos.toByteArray();
            return bytes;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //System.out.println(file.exists() + "!!");
        //InputStream in = resource.openStream();

        return null;
    }
    public byte[] getBytefromimage()
    {
        String filepath = "/storage/emulated/0/DCIM/favicon.png";
        File imagefile = new File(filepath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(imagefile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100 , baos);
        byte[] b = baos.toByteArray();
        System.out.println("File bytessssss: "+ String.valueOf(b));
        return b;
    }
    private void initConnect() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            ArraySet<Host> peers = new ArraySet<>();
            peers.add(mParticipant);
            mNearConnect = new NearConnect.Builder()
                    .forPeers(peers)
                    .setContext(this)
                    .setListener(getNearConnectListener(), Looper.getMainLooper()).build();
            mNearConnect.startReceiving();
        }
        else {
            HashSet<Host> peers = new HashSet<>();
            peers.add(mParticipant);
            mNearConnect = new NearConnect.Builder()
                    .forPeers(peers)
                    .setContext(this)
                    .setListener(getNearConnectListener(), Looper.getMainLooper()).build();
            mNearConnect.startReceiving();
        }
    }
    private NearConnect.Listener getNearConnectListener(){
        return new NearConnect.Listener() {
            @Override
            public void onReceive(byte[] bytes,Host sender) {
                if (bytes != null) {
                    String data = new String(bytes);
                    switch (data) {
                        case STATUS_SEND:


                            break;
                        case STATUS_RECEIVED:
                            break;
                        case STATUS_EXIT:

                            break;
                        default:


                            new SavePhotoTask().execute(bytes);

//                            File someFile = new File("/storage/emulated/0/recode1.log");
//                            try {
//                                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(someFile));
//                                bos.write(bytes);
//                                bos.flush();
//                                bos.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }

//
//                            FileOutputStream fos = null;
//                            try {
//                                fos = new FileOutputStream(someFile);
//                                fos.write(bytes);
//                                fos.flush();
//                                fos.close();
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }

                            binding.devicecontent.setText(String.valueOf(bytes));
                            break;
                    }
                }
            }

            @Override
            public void onSendComplete(long jobId) {
                binding.devicecontent.setText("Content Sended");
            }

            @Override
            public void onSendFailure(Throwable e, long jobId) {

            }

            @Override
            public void onStartListenFailure(Throwable e) {

            }
        };
    }
    public static void start(Activity activity, Host participant) {
        Intent intent = new Intent(activity, ConnectActivity.class);
        intent.putExtra(BUNDLE_PARTICIPANT, participant);
        activity.startActivityForResult(intent, 1234);
    }
    public class genFile {

        public void main(String[] args) throws FileNotFoundException, IOException {
            File file = new File("/storage/emulated/0/cupid/recode.log");

            FileInputStream fis = new FileInputStream(file);
            //System.out.println(file.exists() + "!!");
            //InputStream in = resource.openStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            try {
                for (int readNum; (readNum = fis.read(buf)) != -1;) {
                    bos.write(buf, 0, readNum); //no doubt here is 0
                    //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                    System.out.println("read " + readNum + " bytes,");
                }
            } catch (IOException ex) {
               // Logger.getLogger(genJpeg.class.getName()).log(Level.SEVERE, null, ex);
            }
            byte[] bytes = bos.toByteArray();
            mNearConnect.send(bytes,mParticipant);
//            //below is the different part
//            File someFile = new File("java2.pdf");
//            FileOutputStream fos = new FileOutputStream(someFile);
//            fos.write(bytes);
//            fos.flush();
//            fos.close();
        }
    }
    class SavePhotoTask extends AsyncTask<byte[], String, byte[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected byte[] doInBackground(byte[]... jpeg) {

            File photo=new File(Environment.getExternalStorageDirectory(), "photo_"+System.currentTimeMillis()+".jpg");

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());
                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            return jpeg[0];
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.deviceimage.setImageBitmap(Bitmap.createScaledBitmap(bmp, binding.deviceimage.getWidth(),
                    binding.deviceimage.getHeight(), false));
            binding.devicecontent.setText("Photo_"+System.currentTimeMillis()+".jpg");
            progressDialog.hide();
            Toast.makeText(ConnectActivity.this, "File downloaded", Toast.LENGTH_SHORT).show();
        }
    }
}
