/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package help;

import java.util.regex.Pattern;

/**
 *
 * @author esma
 */
public class RegixMethods {
    
     public static boolean isValidEmail(String mail) {

         String emailRegex = "^([A-z]|[0-9])([A-z]|[0-9]|(\\.)){5,30}@[a-z]{2,9}(\\.)[a-z]{2,3}$";
         
        return mail.matches(emailRegex);
    }

    public static boolean isValidPassword(String pass) {
        String passwordRegex
                = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,50}$";      
        return pass.matches(passwordRegex);
    }
}
