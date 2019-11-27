package Parking;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * ParkingController directs the program based on user input on whether to add
 * parking instances or to pull parking violation report.
 */

class ParkingController {

    /**
     * Logic to call all the other classes to process tickets or add parking
     * instances.
     */

    public void uploadPhotos(Path filePath){
        
        //Start database objects
        Database db = new Database();
        db.createNewDatabase();
        db.createTable("ParkingInstances");

        //handle parking instances
        ParkingInstanceProcessor pip = new ParkingInstanceProcessor();
        ArrayList<ParkingInstance> parkingInstanceArr = new ArrayList<ParkingInstance>();
		parkingInstanceArr = pip.createParkingInstanceArray(filePath);
		pip.addParkingInstanceToDB(db, "ParkingInstances", parkingInstanceArr);

    }

    public void pullViolationReport(LocalDate startDate, LocalDate endDate){
        Database db = new Database();
        db.createNewDatabase();
        db.createTable("Violations");
        
    }

    /**
     * Parking Controller constructor
     */
    public ParkingController() {
    }

    public static void main(String[] args) {
        ParkingController pc = new ParkingController();
        Path filePath = Paths.get("src/test/java/Parking/MultipleImagesFolder/");
        pc.uploadPhotos(filePath);
        
    }


}
