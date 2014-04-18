

public class CheckSystem {
	public static String getOS(){
		return System.getProperty("os.name");
	}
	public static String getJREArch(){
		 return System.getProperty("sun.arch.data.model");
	}
	public static String getOsArch(){
		return System.getProperty("os.arch");
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(getOS());
		System.out.println(getJREArch());
	}
	
}
