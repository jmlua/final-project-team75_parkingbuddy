package Parking;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import net.coobird.thumbnailator.Thumbnails;

/**
 * LicenseOCR calls the OpenALPR API to read license plate from each photo.
 */

class LicenseOCR {

	/**
	 * This method returns a car object by taking in a photo object and extracting the necessary data from the photo. 
	 * We call the OpenALPR API to get the license plate number and state. 
	 * @param photo
	 * @return
	 */
	public Car createCar(Photo photo){
		String photoFilePath = photo.getPhotoFilePath(); 
		String license = "";
		String state = "";

		String[] carData = new String[2];
		carData = generateCarDataWithOpenALPR(photoFilePath);
		license = carData[0];
		state = carData[1];
		Car car = new Car(license, state);
		System.out.println("Car Object: " + car.getLicense() + "," + car.getState() + "\n");

		return car;
	}


	/**
	 * This is a private method that generates the car data by calling the OpenALPR API. 
	 * @param filePath
	 * @return
	 */
	private String[] generateCarDataWithOpenALPR(String filePath) {
		String json_content = "";
		String[] carData = new String[2];

		try {
			String secret_key = "sk_b54c60658f3340d99b2d0531";

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Thumbnails.of(filePath).scale(1).toOutputStream(outputStream);
			byte[] data = outputStream.toByteArray();

			// Encode file bytes to base64
			byte[] encoded = Base64.getEncoder().encode(data);

			// Setup the HTTPS connection to api.openalpr.com
			URL url = new URL("https://api.openalpr.com/v2/recognize_bytes?recognize_vehicle=1&country=us&secret_key=" + secret_key);
			URLConnection con = url.openConnection();
			HttpURLConnection http = (HttpURLConnection)con;
			http.setRequestMethod("POST"); // PUT is another valid option
			http.setFixedLengthStreamingMode(encoded.length);
			http.setDoOutput(true);

			// Send our Base64 content over the stream
			try(OutputStream os = http.getOutputStream()) {
				os.write(encoded);
			}
			int status_code = http.getResponseCode();
			if (status_code == 200) {
				// Read the response
				BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null)
					json_content += inputLine;
				in.close();
			}
			else {
				System.out.println("Got non-200 response: " + status_code);
			}
		}
		catch (MalformedURLException e) {
			System.out.println("Bad URL");
		}
		catch (IOException e) {
			System.out.println("Failed to open connection");
		}

//		System.out.println(json_content);
		File f = new File(filePath);
		String fileName = f.getName();
		if (checkJSONForCar(json_content) == true) {
			System.out.println(fileName + " contains a vehicle.");
			carData = readJSONContent(json_content); 
		} else {
			System.out.println(fileName + " does not contain a vehicle.");
		}

		return carData;
	}

	/**
	 * This is a private helper method that checks if the image contains a vehicle. 
	 * We check this by looking at the JSON file generated by the OpenALPR API call,
	 * and seeing if the confidence of a vehicle present in the image passes a certain threshold. 
	 * @param json_content
	 * @return
	 */
	private boolean checkJSONForCar(String json_content) {
		JSONObject obj = new JSONObject(json_content).optJSONObject("processing_time");
		double vehicle_confidence = obj.optDouble("vehicles");

		if (vehicle_confidence < 10) {
			return false;
		} 
		
		return true;
	}


	/**
	 * This is a helper method that takes in the JSON string file generated by
	 * the OpenALPR API call and parses out the 2 pieces of information we need - 
	 * the car license plate number and the state, returned as a string array.
	 * @param jsonString
	 * @return
	 */
	private String[] readJSONContent(String jsonString) {
		String[] carData = new String[2];
		JSONObject obj = new JSONObject(jsonString);
		// gets values associated with "results" key
		JSONArray result = obj.getJSONArray("results");
		// gets values associated with "plate" key from the first index which has highest confidence
		String licensePlate = result.getJSONObject(0).getString("plate");
		String state = result.getJSONObject(0).getString("region").toUpperCase();
		carData[0] = licensePlate;
		carData[1] = state;
		
		return carData;
	}


	public static void main(String[] args) {
		LicenseOCR test = new LicenseOCR(); 
		JPEGReader r = new JPEGReader();

		Path filePath = Paths.get("src/test/java/Parking/MultipleImagesFolder/");
		ArrayList<Photo> photoArrayList = new ArrayList<Photo>();
		photoArrayList = r.createPhotos(filePath);
		for (Photo photo : photoArrayList) {
			test.createCar(photo);
		}
	}

}
