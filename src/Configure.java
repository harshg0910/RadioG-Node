import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configure {
	public static int MAX_LISTENERS = 3;
	public static String AudioFilePath = "";
	public static int MAX_VOLUME = 200;
	public static int MIN_VOLUME = 0;
	public static int INIT_VOLUME = 100;
	
	private static final Properties properties = new Properties();

    static {
        try {
            
            properties.load(new FileInputStream("config.properties"));    
            
            MAX_LISTENERS = Integer.parseInt(getSetting("Slots"));
            if ( MAX_LISTENERS < 4)
            	MAX_LISTENERS = 3;
            AudioFilePath = getSetting("AudioFilePath");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        
    }

    public static String getSetting(String key) {
        return properties.getProperty(key);
    }
}
