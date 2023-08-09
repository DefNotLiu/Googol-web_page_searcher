package com.example.servingwebcontent.beans;

import java.security.SecureRandom;
import java.util.Base64;

public class SessionToken {
    String value;

    public SessionToken(){
        SecureRandom secureRandom = new SecureRandom(); //threadsafe
        Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe

        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        value = base64Encoder.encodeToString(randomBytes);
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }


}
