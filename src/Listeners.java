import java.util.Vector;

import rice.p2p.commonapi.NodeHandle;


public class Listeners {
	private Vector<NodeHandle> listeningClients = new Vector<>();
	public static final int MAX_LISTENER = 3;
	private int noOfListener = 0;
	private static Listeners listeners = null;
	private Object Lock = new Object();
	
	public Listeners(){
		listeners = this;
	}
	public void addClient(NodeHandle client){
		if(!listeningClients.contains(client) && noOfListener < MAX_LISTENER){
			synchronized (Lock) {
				listeningClients.add(client);
				noOfListener++;
			}
			System.out.println("Client "+client+" added");
			System.out.println("Current Clients "+listeningClients+" " + noOfListener);
			Radio.refrestClientList(listeningClients);
		}
		else{
			System.out.println("Max listener limit reached");
		}
	}
	
	public void removeClient(NodeHandle client){
		System.out.print("Removing " + client);
		synchronized (Lock) {
			if(listeningClients.removeElement(client)){
				noOfListener--;
				Radio.refrestClientList(listeningClients);
				System.out.println("Client "+client+" removed");
			}
			else{
				System.out.println("Client "+client+" not removed");
			}
		}
	}
	
	public void update(){
		System.out.println("Updating client list "+listeningClients + " "+noOfListener);
		for(int i = 0; i < noOfListener; i++) {
			/*removing dead clients*/
			NodeHandle client = listeningClients.get(i);
			System.out.println("Checking for "+client);
			if(!RadioApp.getRadioApp().endpoint.isAlive(client)) {
				System.out.println("Client: "+client + " is dead");
				removeClient(client);
			}
		}
	}
	
	public void sendHeartBeat(){
		for(int i = 0; i < noOfListener; i++) {
			HeartBeat heartBeat = new HeartBeat();
			RadioApp.getRadioApp().endpoint.route(null, heartBeat, listeningClients.get(i));
		}
	}
	public int getNoOfListeners(){
		return noOfListener;
	}
	
	public static Listeners getListener(){
		if(listeners == null){
			listeners = new Listeners();
		}
		return listeners;
		
	}
	
	public boolean isClient(NodeHandle handle){
		return listeningClients.contains(handle);
	}
	
	public Vector<NodeHandle> getListeningClients(){
		return listeningClients;
	}
}
