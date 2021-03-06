package service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import bean.Angle;
import bean.ObservationTO;
import exceptions.TCCException;
import util.TCCConstants;
import util.Utilities;

public class TCCService implements TCCConstants{

	static TCCService tccService = null;
	public static TCCService createTccInstance(){
		if(tccService == null) tccService = new TCCService();
		return tccService;
	}

	private TCCService(){
	}

	public String talkToAnansi(String xmlMessage) throws TCCException{
		try{
			
			String xmlResponse;
			//System.out.println("TO SERVER: " + xmlMessage + " "+ tccControllerIP + " " + tccControllerPort);
			Socket client = new Socket(tccControllerIP, tccControllerPort);
			client.setSoTimeout(20000);
			DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
			outToServer.write(xmlMessage.getBytes());
			xmlResponse = inFromServer.readLine();
			//System.out.println("FROM SERVER: " + xmlResponse);

			client.close();
		return xmlResponse;
	}
	catch (Exception e) {
		throw new TCCException("TCCservice failed: Cause:"+ e.getMessage(), ExceptionUtils.getStackTrace(e));
	}
}

public String sendCommand(String command) throws TCCException{
	String response = "";
	Map<String, String> messageMap = new HashMap<String, String>();
	messageMap.put("command", command);
	StrSubstitutor messageSubstitutor  = new StrSubstitutor(messageMap);
	String message = "";
	switch (command) {

	case track:
		StrSubstitutor paramSubstitutor  = new StrSubstitutor(defaultParams);
		String params = paramSubstitutor.replace(pointParameters);
		messageMap.put("parameters", params);
		break;
	case stop:
		messageMap.put("parameters", "");
		break;
	}
	message = messageSubstitutor.replace(messageWrapper);
	try {
		response = talkToAnansi(message);
	} catch (TCCException e) {
		e.printStackTrace();
		throw new TCCException("TCCservice failed: Cause:"+ e.getMessage(), ExceptionUtils.getStackTrace(e));
	}
	return response;
}

public void pointAndTrackSource(ObservationTO observationTO) throws TCCException{
	
	defaultParams.put("xcoord", observationTO.getAngleRA().toHHMMSS());
	defaultParams.put("ycoord", observationTO.getAngleDEC().toDDMMSS());
	
	defaultParams.put("ns_east_offset", observationTO.getDegNSOffset().toString());
	defaultParams.put("ns_west_offset", observationTO.getDegNSOffset().toString());

	
	if(observationTO.getMdTransit()){
		defaultParams.put("md_east_state", "disabled");
		defaultParams.put("md_west_state", "disabled");

	} else{
		defaultParams.put("md_east_state", "auto");
		defaultParams.put("md_west_state", "auto");
	}
	
	sendCommand(track);
}

@Deprecated
public void pointAndTrackSource(String RA_hhmmss, String DEC_ddmmss, boolean mdTransit) throws TCCException{

	defaultParams.put("xcoord", RA_hhmmss);
	defaultParams.put("ycoord", DEC_ddmmss);
	
	if(mdTransit){
		defaultParams.put("md_east_state", "disabled");
		defaultParams.put("md_west_state", "disabled");

	} else{
		defaultParams.put("md_east_state", "auto");
		defaultParams.put("md_west_state", "auto");
	}
	
	sendCommand(track);


}


/**
 * This is really a hack as "default parameters" are auto read by many parts of the code. Convert to NS/MD, send command, revert back to earlier.
 * @param NS
 * @throws TCCException 
 */
public void pointNS(Angle ns,String arm) throws TCCException{
	
	defaultParams.put("units", "degrees");
	defaultParams.put("epoch", "2000");
	defaultParams.put("system", "nsew");
	defaultParams.put("tracking", "off");
	defaultParams.put("ns_east_state", 	(arm.equals(BOTH_ARMS) || arm.equals(EAST))? "auto": "disabled");
	
	defaultParams.put("ns_west_state", 	(arm.equals(BOTH_ARMS) || arm.equals(WEST))? "auto": "disabled");

	defaultParams.put("md_east_state", "disabled");
	defaultParams.put("md_west_state", "disabled");

	defaultParams.put("ns_east_offset", "0.0");
	defaultParams.put("md_east_offset", "0.0");
	defaultParams.put("ns_west_offset", "0.0");
	defaultParams.put("md_west_offset", "0.0");

	defaultParams.put("ns_east_offset_units", "degrees");
	defaultParams.put("md_east_offset_units", "degrees");
	defaultParams.put("ns_west_offset_units", "degrees");
	defaultParams.put("md_west_offset_units", "degrees");


	defaultParams.put("xcoord", ns.getDegreeValue().toString());
	defaultParams.put("ycoord", "0.0");
	
	sendCommand(track);
	
	loadDefaultParams();

}



public void stopTelescope() throws TCCException{
	sendCommand(stop);

}

public static void main(String[] args) throws InterruptedException {
	TCCService t = new TCCService();
	try {
		t.pointAndTrackSource("22:41:00", "-52:36:00", false);
		Thread.sleep(20000);
		t.stopTelescope();
	} catch (TCCException e) {
		// TODO Auto-generated catch block
		e.printStackTrace(); 
	}
}




static{
	loadDefaultParams();
}

public static void loadDefaultParams(){
	defaultParams.put("units", "hhmmss");
	defaultParams.put("epoch", "2000");
	defaultParams.put("system", "equatorial");
	defaultParams.put("tracking", "on");

	defaultParams.put("ns_east_state", "auto");
	defaultParams.put("md_east_state", "auto");
	defaultParams.put("ns_west_state", "auto");
	defaultParams.put("md_west_state", "auto");

	defaultParams.put("ns_east_offset", "0.0");
	defaultParams.put("md_east_offset", "0.0");
	defaultParams.put("ns_west_offset", "0.0");
	defaultParams.put("md_west_offset", "0.0");

	defaultParams.put("ns_east_offset_units", "degrees");
	defaultParams.put("md_east_offset_units", "degrees");
	defaultParams.put("ns_west_offset_units", "degrees");
	defaultParams.put("md_west_offset_units", "degrees");


	defaultParams.put("xcoord", "00:00:00.0");
	defaultParams.put("ycoord", "00:00:00.0");
}

}
