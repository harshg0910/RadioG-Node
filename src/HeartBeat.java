import rice.p2p.commonapi.Message;

/**
 * Heartbeat- This class is used to keep alive messages on regural interval and
 * Dying message when a client leaves the network. 
 * 
 * @author Abhi
 * 
 */
@SuppressWarnings("serial")
public class HeartBeat implements Message {
	
	/**
	 * Type of the HeartBeat message
	 * ALIVE - Parent is alive
	 * DYING - Parent is leaving the network
	 * @author Abhi
	 *
	 */
	enum Type {
		ALIVE, DYING
	}
	/**
	 * Type of heartbeat
	 */
	Type type; 
	
	/**
	 * Instantiate HeartBeat
	 * @param type Type of heartbeat
	 */
	public HeartBeat(Type type) {
		this.type = type;
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.LOW_PRIORITY;
	}
}
