/**
 * Paraschiv Andra
 * Grupa 343C1
 * Whiteboard Partajat
 */

package pdsd.proiect.whiteboardpartajat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.view.Menu;

public class MainActivity extends Activity {

	private volatile boolean finished = false;
	private String message = "";
	private ArrayList<Socket> whiteboardClients = new ArrayList<Socket>();
	private final String imgCode = "a26e4b7ef59c42054img025255fcodeb1c422a4eb4";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/** Server Thread */
		Thread whiteboardServer = new Thread(new WhiteboardServer());
		whiteboardServer.start();
		/** Client 1 Fragment */
		addFragment1();
		/** Client 2 Fragment */
		addFragment2();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/** Inflate the menu; this adds items to the action bar if it is present. */
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    public void onDestroy() {
    	super.onDestroy();
    	finished = true;
    }
	
	public void addFragment1(){
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		Fragment1 fragment1 = new Fragment1();
		fragmentTransaction.add(R.id.fragment1, fragment1, "fragment1");
		fragmentTransaction.commit();
	}

	public void addFragment2(){
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		Fragment2 fragment2 = new Fragment2();
		fragmentTransaction.add(R.id.fragment2, fragment2, "fragment2");
		fragmentTransaction.commit();
	}
	
	private class WhiteboardServer implements Runnable {
		
		private ServerSocket whiteboardServerSocket;
		
		public WhiteboardServer() {
			try {
				whiteboardServerSocket = new ServerSocket(9000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			int i;
			Socket client = null;
			
			while(!finished) {
				try {
					/** accept client connection */
					client = whiteboardServerSocket.accept();
					whiteboardClients.add(client);
				} catch (Exception e) {
					e.printStackTrace();
				}
				/** processing client data thread */
				new Thread(new ProcessClientData(whiteboardClients.size() - 1)).start();
			}
			/** close each client socket */
			if (!whiteboardClients.isEmpty()){
				for (i = 0; i < whiteboardClients.size(); i++){
					try {
						whiteboardClients.get(i).close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			/** close server socket */
			try {
				whiteboardServerSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ProcessClientData implements Runnable {
		int nrClient;
		
		public ProcessClientData(int nrClient) {
			this.nrClient = nrClient;
		}
		
		private void getMessageAndBroadcast(BufferedReader dataFromClient){
			OutputStream dataToClient = null;
			String line = null;
			int i;
			
			/** get message from client */
			try {
				while(dataFromClient != null && dataFromClient.ready()){
					line = dataFromClient.readLine();
					if (line != null)
						message = message + line + "\n";
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			/** send message to the other clients */
			for (i = 0; i < whiteboardClients.size(); i++)
				if (i != nrClient){
					try {
						dataToClient = whiteboardClients.get(i).getOutputStream();
						PrintStream writer = new PrintStream(dataToClient);
						writer.print(message);
						dataToClient.flush();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
		}
		
		private void getImageAndBroadcast(DataInputStream data){
			OutputStream dataToClient = null;
			DataOutputStream dataOut = null;
			long imageFileSize = 0;
			byte dataByte[];
			int i;
			
			try {
				/** get image from client */
				imageFileSize = data.readLong();
				dataByte = new byte[(int)imageFileSize];
				data.readFully(dataByte);
				/** send image to the other clients */
				for (i = 0; i < whiteboardClients.size(); i++)
					if (i != nrClient){
						dataToClient = whiteboardClients.get(i).getOutputStream();
						PrintStream writer = new PrintStream(dataToClient);
						writer.print(imgCode + "\n");
						dataToClient.flush();
						dataOut = new DataOutputStream(dataToClient);
						dataOut.writeLong(imageFileSize);
						dataOut.flush();
						dataOut.write(dataByte, 0, (int)imageFileSize);
						dataOut.flush();
					}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			BufferedReader dataFromClient = null;
			DataInputStream data = null;
			String line = null;
			
			try {
				dataFromClient = new BufferedReader(new InputStreamReader(whiteboardClients.get(nrClient).getInputStream()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			while(!finished) {
				try {
					if (dataFromClient != null)
						line = dataFromClient.readLine();
					if (line != null){
						/** process image */
						if (line.equals(imgCode)){
							data = new DataInputStream(whiteboardClients.get(nrClient).getInputStream());
							getImageAndBroadcast(data);
						}
						/** process message */
						else{
							message = line + "\n";
							getMessageAndBroadcast(dataFromClient);
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			/** close BufferedReader */
			try {
				if (dataFromClient != null)
					dataFromClient.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
