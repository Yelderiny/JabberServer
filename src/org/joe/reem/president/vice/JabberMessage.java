package org.joe.reem.president.vice;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class JabberMessage implements Serializable
{

	@Serial
	private static final long serialVersionUID = 1L;
	private final String message;
	private final ArrayList<ArrayList<String>> response;
	
	public JabberMessage(final String message)
	{
		this.message = message;
		response = null;
	}
	
	public JabberMessage(final String message, final ArrayList<ArrayList<String>> data)
	{
		this.message = message;
		response = data;
	}
	
	public ArrayList<ArrayList<String>> getData() { return response; }
	public String getMessage() { return message; }
}
