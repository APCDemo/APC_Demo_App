/*
     This file is part of the Android app ch.bfh.securevote.
     (C) 2023 Benjamin Fehrensen (and other contributing authors)
     This library is free software; you can redistribute it and/or
     modify it under the terms of the GNU Lesser General Public
     License as published by the Free Software Foundation; either
     version 2.1 of the License, or (at your option) any later version.
     This library is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     Lesser General Public License for more details.
     You should have received a copy of the GNU Lesser General Public
     License along with this library; if not, write to the Free Software
     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package ch.bfh.securevote.utils;

public class Constants {
    //
    public static final String BACKEND_URL="https://apc.ti.bfh.ch";
    public static final String QUESTIONS_URL=BACKEND_URL+"/api/questions";
    public static final String UUID_URL=BACKEND_URL+"/api/uuid";
    public static final String P7M_URL=BACKEND_URL+"/api/p7m";
    public static final String FAILURE_URL=BACKEND_URL+"/api/failure";
    public static final String PROJECT_SITE_URL = "https://www.bfh.ch/en/research/reference-projects/hardware-protected-confirmation/";
    public static final int CONNECTION_TIMEOUT = 400; // im ms

    public static final String KEY_NAME = "BFH_APC_DEMO";
    public static final String defaultKeyAliasName = "APC Demo App";
    public static final String KEY_STORE_TYPE = "AndroidKeyStore";

    // Settings variables
    public static final String settings_key_name = "attestation_key_alias";
    public static final String settings_key_type = "key_type";
    public static final String settings_key_type_default="EC";
    public static final String settings_cert_validity = "cert_validity";
    public static final String settings_cert_validity_default = "3";
    public static final String settings_user_confirmation_required = "user_confirmation_required";
    public static final boolean settings_user_confirmation_required_default=true;
    public static final String settings_unlock_device_required = "unlock_device_required";
    public static final boolean settings_unlock_device_required_default=true;
    public static final String settings_user_authentication_required = "user_authentication_required";
    public static final boolean settings_user_authentication_required_default=false;
    public static final String settings_strong_box_required = "strong_box_required";
    public static final boolean settings_strong_box_required_default=true;
    public static final String settings_ec_curve = "ec_curve";
    public static final String settings_ec_curve_default="secp256r1";
    public static final String settings_rsa_key_length = "rsa_key_length";
    public static final String settings_rsa_key_length_default="2048";


    // Email feedback setting
    public static final String[] emailAddresses = new String[] {"apc.feedback.ti@bfh.ch"};
    public static final String subject = "Feedback to APC Secure Vote";

}
