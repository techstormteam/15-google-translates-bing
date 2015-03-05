/**
 * @author <a href=mailto:volkodavav@gmail.com>volkodavav</a>
 */
package com.translator.google.core;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

class ProxyAuthenticator extends Authenticator
{
    private String userName, password;

    public ProxyAuthenticator(String userName, String password)
    {
        Assert.containText(userName, "User Name must contain text");
        Assert.containText(password, "User Password must contain text");

        this.userName = userName;
        this.password = password;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(userName, password.toCharArray());
    }
}
