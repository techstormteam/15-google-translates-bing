/**
 * @author <a href=mailto:volkodavav@gmail.com>volkodavav</a>
 */
package com.translator.google.core;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class ProxyWrapper
{
	private Proxy proxy;

	public ProxyWrapper(Proxy proxy)
	{
		Assert.notNull(proxy, "Proxy must not be null");

		this.proxy = proxy;
	}

	public ProxyWrapper(Proxy proxy, String userName, String password)
	{
		Assert.notNull(proxy, "Proxy must not be null");
		Assert.containText(userName, "User Name must contain text");
		Assert.containText(password, "User Password must contain text");

		this.proxy = proxy;

		Authenticator.setDefault(new ProxyAuthenticator(userName, password));
	}

	public ProxyWrapper(String host, int port)
	{
		Assert.containText(host, "Host must be set");
		Assert.isTrue(port > 0, "Port number must be > 0");
		Assert.isTrue(port < 65536, "Port number must be < 65536");

		proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
	}

	public ProxyWrapper(String host, int port, String userName, String password)
	{
		Assert.containText(host, "Host must be set");
		Assert.isTrue(port > 0, "Port number must be > 0");
		Assert.isTrue(port < 65536, "Port number must be < 65536");
		Assert.containText(userName, "User Name must contain text");
		Assert.containText(password, "User Password must contain text");

		proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));

		Authenticator.setDefault(new ProxyAuthenticator(userName, password));
	}

	public Proxy getProxy()
	{
		return proxy;
	}
}
