import rice.p2p.commonapi.Message;


@SuppressWarnings("serial")
public class HeartBeat implements Message{
	
	enum Type{
		ALIVE,
		DYING
	}
	Type type;
	public HeartBeat(Type type){
		this.type = type;
	}
	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.LOW_PRIORITY;
	} 
}
