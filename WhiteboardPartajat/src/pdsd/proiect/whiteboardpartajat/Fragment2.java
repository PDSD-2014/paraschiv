/**
 * Paraschiv Andra
 * Grupa 343C1
 * Whiteboard Partajat
 */

package pdsd.proiect.whiteboardpartajat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.Spinner;

public class Fragment2 extends Fragment {
	
	private volatile boolean finished = false;
	private String message2 = "";
	private Socket whiteboardClient2Socket;
	private final String sdcardRootPath = Environment.getExternalStorageDirectory().getPath();
	private final String imgCode = "a26e4b7ef59c42054img025255fcodeb1c422a4eb4";
	
	@Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle state) {
        return layoutInflater.inflate(R.layout.activity_fragment2, container, false);
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		
		EditText message2Text = (EditText)getActivity().findViewById(R.id.message2);
		
		/** message edit text */
		message2Text.addTextChangedListener(new TextWatcher() {
 
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
 
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
 
			@Override
			public void afterTextChanged(Editable s) {
				OutputStream dataToServer = null;
				
				if (s.toString().equals(message2))
					return;
				message2 = s.toString();
				/** message changed -> send it to server */
				if (message2.endsWith("\n")){
					try {
						dataToServer = whiteboardClient2Socket.getOutputStream();
						PrintStream writer = new PrintStream(dataToServer);
						writer.print(message2);
						dataToServer.flush();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		message2Text.setScroller(new Scroller(getActivity()));
		message2Text.setMaxLines(3);
		message2Text.setVerticalScrollBarEnabled(true);
		message2Text.setMovementMethod(new ScrollingMovementMethod());
		message2Text.setTextSize(13);
		
		/** file path list for uploading images */
		Spinner spinner2 = (Spinner)getActivity().findViewById(R.id.spinner2);
		ArrayList<String> filePathList = new ArrayList<String>();
		/** fill list with all files from sdcard */
		fillFilePathList(filePathList, sdcardRootPath);
		ArrayAdapter<String> filePathAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, filePathList);
		filePathAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(filePathAdapter);
		spinner2.setHorizontalScrollBarEnabled(true);

		/** image upload button */
		Button upload2 = (Button)getActivity().findViewById(R.id.upload2);
		upload2.setOnClickListener(new UploadButtonOnClickListener());
		upload2.setTextSize(13);
		
		/** client thread */
		Thread whiteboardClient2 = new Thread(new WhiteboardClient2());
		whiteboardClient2.start();
	}
	
	@Override
    public void onDestroyView() {
    	super.onDestroy();
    	finished = true;
    }
	
	private void fillFilePathList(ArrayList<String> fileList, String path){
		File dir;
		File[]files;
		
		dir = new File(path);
		files = dir.listFiles();
		for (File f : files){
			if (f.isFile())
				fileList.add(f.getAbsolutePath().substring(sdcardRootPath.length() + 1));
			if (f.isDirectory())
				fillFilePathList(fileList, f.getAbsolutePath());
		}
	}
	
	private class UploadButtonOnClickListener implements View.OnClickListener{
		  @Override
		  public void onClick(View v) {
			Spinner spinner2 = (Spinner)getActivity().findViewById(R.id.spinner2);
			LinearLayout gallery2 = (LinearLayout)getActivity().findViewById(R.id.gallery2);
			ImageButton image = new ImageButton(getActivity());
			OutputStream dataToServer = null;
			File imageFile;
			DataOutputStream data = null;
			InputStream imageFileData;
			long imageFileSize;
			int ret, offset = 0;
			Bitmap imageBitmap;
			String imagePath;
			
			/** get image */
			imagePath = sdcardRootPath + "/" + String.valueOf(spinner2.getSelectedItem());
			imageBitmap = BitmapFactory.decodeFile(imagePath);
			if (imageBitmap != null){
				/** add image to gallery */
				image.setImageBitmap(imageBitmap);
				gallery2.addView(image);
				image.getLayoutParams().height = 75;
				image.getLayoutParams().width = 100;
				image.setScaleType(ScaleType.FIT_XY);
				image.setLayoutParams(image.getLayoutParams());
				
				/** read image and send it to server */
				try {
					dataToServer = whiteboardClient2Socket.getOutputStream();
					PrintStream writer = new PrintStream(dataToServer);
					writer.print(imgCode + "\n");
					dataToServer.flush();
					
					imageFile = new File(imagePath);
					imageFileData = new FileInputStream(imageFile);
					data = new DataOutputStream(dataToServer);
					imageFileSize = imageFile.length();
					data.writeLong(imageFileSize);
					data.flush();
					
					while((ret = imageFileData.read()) != -1){
						data.write(ret);
						data.flush();
						offset++;
						if (offset >= imageFileSize)
							break;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		  }
	}
	
	private class WhiteboardClient2 implements Runnable {
		
		public WhiteboardClient2() {}
		
		private void getMessage(BufferedReader dataFromServer){
			final EditText message2Text = (EditText)getActivity().findViewById(R.id.message2);
			String line = null;
			
			/** get message from server */
			try {
				while(dataFromServer != null && dataFromServer.ready()){
					line = dataFromServer.readLine();
					if (line != null)
						message2 = message2 + line + "\n";
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			/** set edit text */
			getActivity().runOnUiThread(new Runnable() {            
			    @Override
			    public void run() {
			    	if (message2Text != null)
			    		message2Text.setText(message2);
			    }
			});

		}
		
		private void getImage(DataInputStream data){
			final LinearLayout gallery2 = (LinearLayout)getActivity().findViewById(R.id.gallery2);
			long imageFileSize;
			byte dataByte[];
			final Bitmap imageBitmap;
			
			/** get image from server */
			try {
				imageFileSize = data.readLong();
				
				dataByte = new byte[(int)imageFileSize];
				data.readFully(dataByte);
				
				/** add image to gallery */
				imageBitmap = BitmapFactory.decodeByteArray(dataByte, 0, (int)imageFileSize);
				if (imageBitmap != null){
					getActivity().runOnUiThread(new Runnable() {            
					    @Override
					    public void run() {
							ImageButton image = new ImageButton(getActivity());
							image.setImageBitmap(imageBitmap);
							gallery2.addView(image);
							image.getLayoutParams().height = 75;
							image.getLayoutParams().width = 100;
							image.setScaleType(ScaleType.FIT_XY);
							image.setLayoutParams(image.getLayoutParams());
					    }
					});
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			BufferedReader dataFromServer = null;
			String line = null;
			DataInputStream data = null;
			
			/** connect to server */
			try {
				whiteboardClient2Socket = new Socket("127.0.0.1", 9000);
				dataFromServer = new BufferedReader(new InputStreamReader(whiteboardClient2Socket.getInputStream()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			while(!finished) {
				try {
					if (dataFromServer != null)
						line = dataFromServer.readLine();
					if (line != null){
						/** process image */
						if (line.equals(imgCode)){
							data = new DataInputStream(whiteboardClient2Socket.getInputStream());
							getImage(data);
						}
						/** process message */
						else{
							message2 = line + "\n";
							getMessage(dataFromServer);
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			/** close client socket and BufferedReader */
			try {
				whiteboardClient2Socket.close();
				if (dataFromServer != null)
					dataFromServer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
