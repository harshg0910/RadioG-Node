import rice.p2p.commonapi.Message;


public class HeartBeat implements Message{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	
	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.LOW_PRIORITY;
	} 
}
