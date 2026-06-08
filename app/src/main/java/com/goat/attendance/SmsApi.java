package com.goat.attendance;

import android.telephony.SmsManager;

public class SmsApi {

    public static void sendAbsentSms(String phoneNumber, String studentName) {
        try {
            String message = "THE GOAT INDIA SKATING CLUB \n Hi, Your Student " + studentName + " was absent today!";

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void sendPresentSms(String phoneNumber, String studentName) {
        try {
            String message = "THE GOAT INDIA SKATING CLUB \n Hi, Your Student " + studentName + " was present today!";

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}