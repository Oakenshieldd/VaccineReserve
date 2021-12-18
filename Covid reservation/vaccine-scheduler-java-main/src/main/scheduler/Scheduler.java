package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;
    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> cancel <appointment_id>");
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    static boolean isValidDate(String input) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            format.parse(input);
            return true;
        }
        catch(ParseException e){
            return false;
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please Try again");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to Patient information to out database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentCaregiver != null) {
            System.out.println("Already logged-in");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        // TODO: Part 2
        // check 1: check if the user have logged in as a patient or a caregiver
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login in first!");
            return;
        }
        // check 2: check if the user enter the right tokens
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        // check: check if the date is valid
        String dateStr = tokens[1];
        if (!isValidDate(dateStr)) {
            System.out.println("Please enter a valid date, the format should be 'yyyy-MM-dd'");
            return;
        }

        Date selectedDate = Date.valueOf(tokens[1]);

        // Setup Connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // SQLs:
        String selectCaregiver = "SELECT Username FROM Availabilities WHERE Time = ?;";
        String selectVaccine = "SELECT * FROM Vaccines WHERE Doses > 0;";
        try {
            // SQL: SELECT available Caregiver
            PreparedStatement statementCaregiver = con.prepareStatement(selectCaregiver);
            statementCaregiver.setDate(1, selectedDate);
            ResultSet caregiverResult = statementCaregiver.executeQuery();
            // check if no caregiver available
            if (caregiverResult == null) {
                System.out.println("No caregiver available at this date, try another one!");
                return;
            }
            // Print out
            System.out.println("Here are available Caregivers:");
            while (caregiverResult.next()) {
                String caregiverName = caregiverResult.getString("Username");
                System.out.println(caregiverName);
            }

            // SQL: SELECT available vaccine
            PreparedStatement statementVaccine = con.prepareStatement(selectVaccine);
            ResultSet vaccineResult = statementVaccine.executeQuery();
            // Print out
            System.out.println("Here are the vaccines we provide:");
            while (vaccineResult.next()) {
                String vaccineName = vaccineResult.getString("Name");
                int doses = vaccineResult.getInt("Doses");
                System.out.println("Vaccine Name:" + vaccineName + "; " + "Dose available:" + doses);
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when user ask Schedule available");
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) throws SQLException {
        // TODO: Part 2
        // reserve <date> <vaccine>
        // check 1: check if the current logged-in user is a patient
        if (currentPatient == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        // Setup a vaccine class
        String vaccineName = tokens[2];
        int doses = 1;
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when get vaccine name");
            e.printStackTrace();
        }
        // check 3: check if given vaccine exists
        if (vaccine == null) {
            System.out.println("We don't provide vaccine:" + vaccineName + "/n" + "Please try another one!");
            return;
        }
        // check: check if the date is valid
        String dateStr = tokens[1];
        if (!isValidDate(dateStr)) {
            System.out.println("Please enter a valid date, the format should be 'yyyy-MM-dd'");
            return;
        }

        Date reserveDate = Date.valueOf(tokens[1]);
        // Use Date + PatientName as appointment_id, since every date + Name is unique
        String appointment_id = tokens[1] + "-" + currentPatient.getUsername();

        // Setup Connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // SQLs:
        String getRandomCaregiver = "SELECT TOP 1 Username FROM Availabilities WHERE Time = ? ORDER BY NEWID();";
        //String getRandomCaregiver = "SELECT Username FROM Availabilities WHERE Time = ?;";
        String getVaccineNum = "SELECT Doses FROM Vaccines WHERE Name = ?;";
        String addAppointment = "INSERT INTO Appointments VALUES (? , ? , ? , ? , ?);";
        String deleteAvailability = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?;";
        try {
            // Caregiver SQL
            PreparedStatement statementSelectCaregiver = con.prepareStatement(getRandomCaregiver);
            statementSelectCaregiver.setDate(1, reserveDate);
            ResultSet caregiverResult = statementSelectCaregiver.executeQuery();
            // Vaccine SQL
            PreparedStatement statementSelectVaccine = con.prepareStatement(getVaccineNum);
            statementSelectVaccine.setString(1, vaccineName);
            ResultSet vaccineResult = statementSelectVaccine.executeQuery();
            while (vaccineResult.next()) {
                int vaccineNum = vaccineResult.getInt("Doses");
                // check 4: check if the selected vaccine is still in stock
                if (vaccineNum == 0) {
                    System.out.println("No available doses for this vaccine, try another one!");
                    return;
                }
            }
            // check 5: check if the date is available
            if (caregiverResult == null) {
                System.out.println("No available caregiver for this date, try another date!");
                return;
            }

            while (caregiverResult.next()) {
                String reservedCaregiverName = caregiverResult.getString("Username");

                // Insert Appointment reserve tuple
                PreparedStatement statementAddAppointment = con.prepareStatement(addAppointment);
                statementAddAppointment.setString(1, appointment_id);
                statementAddAppointment.setString(2, reservedCaregiverName);
                statementAddAppointment.setString(3, currentPatient.getUsername());
                statementAddAppointment.setString(4, vaccineName);
                statementAddAppointment.setDate(5, reserveDate);
                statementAddAppointment.executeUpdate();

                // Delete Availability
                PreparedStatement statementDeleteAvailability = con.prepareStatement(deleteAvailability);
                statementDeleteAvailability.setString(1, reservedCaregiverName);
                statementDeleteAvailability.setDate(2, reserveDate);
                statementDeleteAvailability.executeUpdate();

                // Vaccine -1 dose
                try {
                    vaccine.decreaseAvailableDoses(doses);
                } catch (SQLException e) {
                    System.out.println("Error occurred when decrease doses");
                    e.printStackTrace();
                }

                // Print out message
                System.out.println(" *** Appointment scheduled successfully ***");
                System.out.println("Caregiver name:" + reservedCaregiverName);
                System.out.println("Appointment ID:" + appointment_id);
            }

        } catch (SQLException e) {
            System.out.println("Error occurred when user reserve");
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) throws SQLException {
        // TODO: Extra credit
        // check 1: check if the user has logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: check if the user entered the right tokens
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String appointment_id = tokens[1];

        //Setup Connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        //SQL: Select specific row given the appointment id
        String selectAppointment = "SELECT * FROM Appointments WHERE appointment_id = ?;";
        //SQL: Delete Appointment tuple
        String deleteAppointment = "DELETE FROM Appointments WHERE appointment_id = ?;";
        //SQL: reUpload Availability
        String uploadAvailability = "INSERT INTO Availabilities VALUES (? , ?);";
        try {
            PreparedStatement statementAppointment = con.prepareStatement(selectAppointment);
            statementAppointment.setString(1, appointment_id);
            ResultSet appointmentResult = statementAppointment.executeQuery();
            // check 3: check if the appointment_id is wrong
            if (appointmentResult == null) {
                System.out.println("Appointment_id doesn't exist, please try again!");
                return;
            }
            while (appointmentResult.next()) {
                String caregiverName = appointmentResult.getString("C_name");
                String vaccineName = appointmentResult.getString("V_name");
                String patientName = appointmentResult.getString("P_name");
                Date appointmentDate = appointmentResult.getDate("Time");

                // Add doses for reserved vaccine
                if (currentPatient != null && !Objects.equals(currentPatient.getUsername(), patientName)) {
                    System.out.println("You can only cancel your appointments, please try again.");
                    return;
                }
                try {
                    int doses = 1;
                    Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                    vaccine.increaseAvailableDoses(doses);
                } catch (SQLException e) {
                    System.out.println("Error occurred when add doses");
                    e.printStackTrace();
                }
                // Re-upload Availability for caregiver
                try {
                    PreparedStatement statementUpload = con.prepareStatement(uploadAvailability);
                    statementUpload.setDate(1, appointmentDate);
                    statementUpload.setString(2, caregiverName);
                    statementUpload.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Error occurred when re-upload availability");
                    throw new SQLException();
                }
                // Delete appointments
                try {
                    PreparedStatement statementDelete = con.prepareStatement(deleteAppointment);
                    statementDelete.setString(1, appointment_id);
                    statementDelete.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Error when delete appointment tuple");
                    throw new SQLException();
                }
                System.out.println(" *** Appointment canceled successfully ***");
                System.out.println("You may schedule a different appointment now");
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) throws SQLException {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                throw new SQLException();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        // TODO: Part 2
        // check 1: check if the user has logged in as a patient or caregiver
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in First!");
            return;
        }
        // check 2: check if the user enter the right tokens
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        // Caregiver logged in:
        if (currentCaregiver != null) {
            // Setup Connection
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            // SQLs:
            String selectAppointment = "SELECT appointment_id, V_name, Time, P_name From Appointments WHERE C_name = ?;";
            try {
                PreparedStatement statementCaregiver = con.prepareStatement(selectAppointment);
                statementCaregiver.setString(1, currentCaregiver.getUsername());
                ResultSet caregiverResult = statementCaregiver.executeQuery();
                // Print appointments
                System.out.println("Here are the appointments info with your patients:");
                while (caregiverResult.next()) {
                    String appointment_id = caregiverResult.getString("appointment_id");
                    String vaccineName = caregiverResult.getString("V_name");
                    Date appointmentDate = caregiverResult.getDate("Time");
                    String patientName = caregiverResult.getString("P_name");
                    System.out.println("Appointment ID:" + appointment_id + "; " +
                            "Vaccine Name:" + vaccineName + "; " +
                            "Date:" + appointmentDate + "; " +
                            "Patient Name:" + patientName);
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when Caregiver ask for appointments info");
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
        if (currentPatient != null) {
            // Setup Connection
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            // SQLs:
            String selectAppointment = "SELECT appointment_id, V_name, Time, C_name From Appointments WHERE P_name = ?;";
            try {
                PreparedStatement statementPatient = con.prepareStatement(selectAppointment);
                statementPatient.setString(1, currentPatient.getUsername());
                ResultSet patientResult = statementPatient.executeQuery();
                // Print appointments
                System.out.println("Here are the appointments info with your caregiver:");
                while (patientResult.next()) {
                    String appointment_id = patientResult.getString("appointment_id");
                    String vaccineName = patientResult.getString("V_name");
                    Date appointmentDate = patientResult.getDate("Time");
                    String caregiverName = patientResult.getString("C_name");
                    System.out.println("Appointment ID:" + appointment_id + "; " +
                            "Vaccine Name:" + vaccineName + "; " +
                            "Date:" + appointmentDate + "; " +
                            "Caregiver Name:" + caregiverName);
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when Patient ask for appointments info");
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // check 1: check if the user enter the right tokens
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        // check 1: check if the user has logged in as a patient or caregiver
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in first!");
        } else {
            currentPatient = null;
            currentCaregiver = null;
            System.out.println("Already logged out!");
        }
    }
}
