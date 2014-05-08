Refer : https://github.com/harshg0910/RadioG-Node/

To deploy the application
1. Right click on project Export-> JAVA -> Runnable Jar file
2. For bootstrap, don't disable any field of GUI
3. For surrogate nodes, disable the "BootStrap Node" Radio Button
4. For client nodes, disable following components
	- "Bootstrap Node" radio button
	- "Choose Audio file" button and text field
	- "Stream" button
	- "isSurrogate" radio button
5. Make a text file in the BootstrapURL with only text as "BOOTSTRAP_IP:PORT" ( without quotes)
6. Setup a socket to accept log files. Refer "FileServer.java"

For setting up the show
1. Check system configuration in config.param file. Do necessary changes.
2. Specify bootstrap node address in the file specified in BootstrapURL parameter in the config.param file.
3. Create a bootstrap node with address given in the BootstrapURL's file. 
4. Create surrogate nodes (recommended 3-5) on some trusted systems which will be up for whole show. Provide the stream address as main streaming server's address and click on the button "Stream".
5. Now streaming show has been set upped the user can join the show.

For clients
1. Open the application RadioG.jar
2. Just click on the button "Start".
3. Wait for the system to connect with network and you can enjoy the show after that. 